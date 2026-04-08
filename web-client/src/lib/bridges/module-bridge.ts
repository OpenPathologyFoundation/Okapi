import { csrfHeaders } from '../csrf';
import type {
	BridgeMessage,
	ModuleBridgeEventMap,
	ModuleBridgeOptions,
	ModuleBridgeState,
	ModuleLaunchConfig,
} from '../types/module-bridge';

type EventHandler = (...args: any[]) => void;

export abstract class ModuleBridge<
	TConfig extends ModuleLaunchConfig,
	TIncoming extends BridgeMessage,
	TAuditEvent = unknown,
	TEvents extends ModuleBridgeEventMap<TAuditEvent> = ModuleBridgeEventMap<TAuditEvent>,
> {
	protected moduleWindow: Window | null = null;
	protected moduleOrigin = '';
	protected config: TConfig | null = null;
	protected state: ModuleBridgeState = 'idle';

	private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
	private windowCheckInterval: ReturnType<typeof setInterval> | null = null;
	private tokenRefreshTimeout: ReturnType<typeof setTimeout> | null = null;
	private auditFlushTimeout: ReturnType<typeof setTimeout> | null = null;
	private messageHandler: ((event: MessageEvent) => void) | null = null;
	private eventListeners: Map<string, Set<EventHandler>> = new Map();
	private auditEventBatch: TAuditEvent[] = [];
	private mintToken: (() => Promise<string | null>) | null = null;
	private ownsModuleWindow = false;

	constructor(options?: ModuleBridgeOptions) {
		this.mintToken = options?.mintToken ?? null;
		this.auditEndpoint = options?.auditEndpoint ?? null;
	}

	private auditEndpoint: string | null;

	launch(config: TConfig): void {
		const moduleOrigin = this.getModuleOrigin(config);
		if (!moduleOrigin) {
			throw new Error(`[${this.constructor.name}] module origin is required — cannot launch without origin validation`);
		}

		const targetWindow = window.open(
			this.getModuleUrl(config),
			this.getWindowName(config),
			this.getWindowFeatures(config),
		);

		if (!targetWindow) {
			this.setState('idle');
			this.handleLaunchFailure();
			return;
		}

		this.attachToTarget(config, targetWindow, { ownsTargetWindow: true, trackWindowClosed: true });
	}

	attachToTarget(
		config: TConfig,
		targetWindow: Window,
		options?: { ownsTargetWindow?: boolean; trackWindowClosed?: boolean },
	): void {
		const moduleOrigin = this.getModuleOrigin(config);
		if (!moduleOrigin) {
			throw new Error(`[${this.constructor.name}] module origin is required — cannot attach without origin validation`);
		}

		this.cleanup();
		if (this.moduleWindow && this.ownsModuleWindow && !this.moduleWindow.closed) {
			this.moduleWindow.close();
		}

		this.config = config;
		this.moduleOrigin = moduleOrigin;
		this.moduleWindow = targetWindow;
		this.ownsModuleWindow = options?.ownsTargetWindow ?? false;
		this.setState('launching');

		this.messageHandler = (event: MessageEvent) => this.handleMessageEvent(event);
		window.addEventListener('message', this.messageHandler);

		if (options?.trackWindowClosed ?? false) {
			this.windowCheckInterval = setInterval(() => {
				if (this.moduleWindow?.closed) {
					this.handleModuleClosed();
				}
			}, 1000);
		}
	}

	close(): void {
		this.sendToModule(this.buildLogoutMessage());
		this.cleanup();
		if (this.moduleWindow && this.ownsModuleWindow && !this.moduleWindow.closed) {
			this.moduleWindow.close();
		}
		this.moduleWindow = null;
		this.ownsModuleWindow = false;
		this.setState('closed');
	}

	isAlive(): boolean {
		return this.state === 'connected' && !!this.moduleWindow && !this.moduleWindow.closed;
	}

	getState(): ModuleBridgeState {
		return this.state;
	}

	sendFocusState(state: 'active' | 'blurred'): void {
		if (this.state !== 'connected') return;
		this.sendToModule(this.buildFocusMessage(state));
	}

	sendContextUpdate(key: string, value: unknown): void {
		if (this.state !== 'connected') return;
		this.sendToModule(this.buildContextUpdateMessage(key, value));
	}

	on<K extends keyof TEvents>(event: K, handler: TEvents[K] extends EventHandler ? TEvents[K] : never): () => void {
		const eventName = String(event);
		if (!this.eventListeners.has(eventName)) {
			this.eventListeners.set(eventName, new Set());
		}
		const callback = handler as EventHandler;
		this.eventListeners.get(eventName)!.add(callback);
		return () => this.eventListeners.get(eventName)?.delete(callback);
	}

	destroy(): void {
		this.close();
		this.eventListeners.clear();
	}

	protected abstract getModuleUrl(config: TConfig): string;
	protected abstract getModuleOrigin(config: TConfig): string;
	protected abstract isModuleMessage(message: unknown): message is TIncoming;
	protected abstract isReadyMessage(message: TIncoming): boolean;
	protected abstract buildInitMessage(config: TConfig): BridgeMessage;

	protected getWindowName(_config: TConfig): string {
		return 'starling-module';
	}

	protected getWindowFeatures(_config: TConfig): string {
		return 'width=1400,height=900,menubar=no,toolbar=no,location=no,status=no';
	}

	protected buildFocusMessage(state: 'active' | 'blurred'): BridgeMessage<{ state: 'active' | 'blurred' }> {
		return { type: 'orchestrator:focus', payload: { state } };
	}

	protected buildContextUpdateMessage(key: string, value: unknown): BridgeMessage<{ key: string; value: unknown }> {
		return { type: 'orchestrator:context-update', payload: { key, value } };
	}

	protected buildLogoutMessage(): BridgeMessage<Record<string, never>> {
		return { type: 'orchestrator:logout', payload: {} };
	}

	protected buildHeartbeatMessage(timestamp: number): BridgeMessage<{ timestamp: number }> {
		return { type: 'orchestrator:heartbeat', payload: { timestamp } };
	}

	protected buildTokenRefreshMessage(token: string): BridgeMessage<{ token: string }> {
		return { type: 'orchestrator:token-refresh', payload: { token } };
	}

	protected handleModuleMessage(_message: TIncoming): void {}

	protected handleLaunchFailure(): void {}

	protected handleModuleReady(): void {
		if (!this.config) return;

		this.setState('connected');
		this.sendToModule(this.buildInitMessage(this.config));
		this.startHeartbeat();
		this.scheduleTokenRefresh(this.config.token);
	}

	protected handleAuditEvent(event: TAuditEvent): void {
		this.auditEventBatch.push(event);
		this.emit('auditEvent', event);

		if (!this.auditEndpoint) return;
		if (!this.auditFlushTimeout) {
			this.auditFlushTimeout = setTimeout(() => void this.flushAuditEvents(), 5000);
		}
	}

	protected emit(event: keyof TEvents, ...args: any[]): void {
		const listeners = this.eventListeners.get(String(event));
		if (!listeners) return;
		for (const listener of listeners) {
			listener(...args);
		}
	}

	protected sendToModule(message: BridgeMessage): void {
		if (!this.moduleWindow || this.moduleWindow.closed) return;
		if (!this.moduleOrigin) return;
		try {
			this.moduleWindow.postMessage(message, this.moduleOrigin);
		} catch (error) {
			console.warn(`[${this.constructor.name}] Failed to send message:`, error);
		}
	}

	protected getModuleWindow(): Window | null {
		return this.moduleWindow;
	}

	private handleMessageEvent(event: MessageEvent): void {
		if (this.moduleOrigin && event.origin !== this.moduleOrigin) {
			return;
		}
		if (event.source !== this.moduleWindow) {
			return;
		}
		if (!this.isModuleMessage(event.data)) {
			return;
		}

		if (this.isReadyMessage(event.data)) {
			this.handleModuleReady();
			return;
		}

		this.handleModuleMessage(event.data);
	}

	private startHeartbeat(): void {
		this.stopHeartbeat();
		this.heartbeatInterval = setInterval(() => {
			this.sendToModule(this.buildHeartbeatMessage(Date.now()));
		}, 5000);
	}

	private stopHeartbeat(): void {
		if (this.heartbeatInterval) {
			clearInterval(this.heartbeatInterval);
			this.heartbeatInterval = null;
		}
	}

	private scheduleTokenRefresh(token: string): void {
		if (this.tokenRefreshTimeout) {
			clearTimeout(this.tokenRefreshTimeout);
		}

		try {
			const parts = token.split('.');
			if (parts.length !== 3) return;
			const payload = JSON.parse(atob(parts[1]));
			if (!payload.exp) return;

			const expiresAt = payload.exp * 1000;
			const refreshAt = expiresAt - 60000;
			const delay = refreshAt - Date.now();

			if (delay <= 0) {
				void this.refreshToken();
				return;
			}

			this.tokenRefreshTimeout = setTimeout(() => void this.refreshToken(), delay);
		} catch {
			// Ignore malformed tokens; refresh falls back to manual relaunch.
		}
	}

	private async refreshToken(): Promise<void> {
		if (!this.mintToken) return;

		try {
			const newToken = await this.mintToken();
			if (newToken && this.state === 'connected') {
				this.sendToModule(this.buildTokenRefreshMessage(newToken));
				if (this.config) {
					this.config.token = newToken;
				}
				this.scheduleTokenRefresh(newToken);
			}
		} catch (error) {
			console.error(`[${this.constructor.name}] Token refresh failed:`, error);
		}
	}

	private async flushAuditEvents(): Promise<void> {
		this.auditFlushTimeout = null;
		if (!this.auditEndpoint || this.auditEventBatch.length === 0) return;

		const events = [...this.auditEventBatch];
		this.auditEventBatch = [];

		try {
			await fetch(this.auditEndpoint, {
				method: 'POST',
				credentials: 'include',
				headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
				body: JSON.stringify(this.getAuditRequestBody(events)),
			});
		} catch (error) {
			console.warn(`[${this.constructor.name}] Failed to flush audit events:`, error);
			this.auditEventBatch.unshift(...events);
		}
	}

	protected getAuditRequestBody(events: TAuditEvent[]): unknown {
		return { events };
	}

	private handleModuleClosed(): void {
		this.cleanup();
		this.moduleWindow = null;
		this.ownsModuleWindow = false;
		this.setState('closed');
	}

	private cleanup(): void {
		this.stopHeartbeat();

		if (this.windowCheckInterval) {
			clearInterval(this.windowCheckInterval);
			this.windowCheckInterval = null;
		}
		if (this.tokenRefreshTimeout) {
			clearTimeout(this.tokenRefreshTimeout);
			this.tokenRefreshTimeout = null;
		}
		if (this.auditFlushTimeout) {
			clearTimeout(this.auditFlushTimeout);
			void this.flushAuditEvents();
		}
		if (this.messageHandler) {
			window.removeEventListener('message', this.messageHandler);
			this.messageHandler = null;
		}
	}

	private setState(state: ModuleBridgeState): void {
		this.state = state;
		this.emit('stateChange', state);
	}
}
