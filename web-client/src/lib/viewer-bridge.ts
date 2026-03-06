/**
 * Viewer Bridge — Orchestrator Side
 *
 * Manages the communication channel between the Okapi orchestrator
 * and the digital-viewer window via postMessage.
 *
 * Responsibilities:
 * - Open/close the viewer window
 * - Send typed messages with origin validation
 * - Heartbeat monitoring (detect viewer closed)
 * - Token pre-expiry refresh
 * - Proxy viewer audit events to backend
 */

import { csrfHeaders } from './csrf';
import type {
	OrchestratorMessage,
	ViewerMessage,
	ViewerLaunchConfig,
	ViewerAuditEvent,
	InitPayload,
} from './types/viewer-bridge';

export type ViewerBridgeState = 'idle' | 'launching' | 'connected' | 'closed';

export interface ViewerBridgeEvents {
	stateChange: (state: ViewerBridgeState) => void;
	caseLoaded: (data: { caseId: string; slideCount: number }) => void;
	viewerError: (data: { code: string; message: string }) => void;
	auditEvent: (event: ViewerAuditEvent) => void;
}

export class ViewerBridge {
	private viewerWindow: Window | null = null;
	private viewerOrigin: string;
	private config: ViewerLaunchConfig | null = null;
	private state: ViewerBridgeState = 'idle';

	// Timers
	private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
	private windowCheckInterval: ReturnType<typeof setInterval> | null = null;
	private tokenRefreshTimeout: ReturnType<typeof setTimeout> | null = null;

	// Event listeners
	private messageHandler: ((event: MessageEvent) => void) | null = null;
	private eventListeners: Map<string, Set<Function>> = new Map();

	// Audit event batching
	private auditEventBatch: ViewerAuditEvent[] = [];
	private auditFlushTimeout: ReturnType<typeof setTimeout> | null = null;

	// Token refresh callback (caller provides this to mint new tokens)
	private mintToken: (() => Promise<string | null>) | null = null;

	constructor(options?: { mintToken?: () => Promise<string | null> }) {
		this.viewerOrigin = '';
		this.mintToken = options?.mintToken ?? null;
	}

	/** Launch the viewer window */
	launch(config: ViewerLaunchConfig): void {
		if (!config.viewerOrigin) {
			throw new Error('[ViewerBridge] viewerOrigin is required — cannot launch without origin validation');
		}

		// Clean up any previous session before opening a new window
		this.cleanup();
		if (this.viewerWindow && !this.viewerWindow.closed) {
			this.viewerWindow.close();
		}

		this.config = config;
		this.viewerOrigin = config.viewerOrigin;
		this.setState('launching');

		// Open viewer window with positioning for multi-monitor
		const features = [
			'width=1400',
			'height=900',
			'menubar=no',
			'toolbar=no',
			'location=no',
			'status=no',
		].join(',');

		this.viewerWindow = window.open(config.viewerUrl, 'okapi-viewer', features);
		console.log('[ViewerBridge] window.open result:', this.viewerWindow ? 'opened (closed=' + this.viewerWindow.closed + ')' : 'NULL (blocked)');

		if (!this.viewerWindow) {
			console.error('[ViewerBridge] Failed to open viewer window — popup blocked?');
			this.setState('idle');
			return;
		}

		// Start listening for messages
		this.messageHandler = (event: MessageEvent) => this.handleMessage(event);
		window.addEventListener('message', this.messageHandler);

		// Start checking if window is closed
		this.windowCheckInterval = setInterval(() => {
			if (this.viewerWindow?.closed) {
				this.handleViewerClosed();
			}
		}, 1000);
	}

	/** Send focus state to the viewer (active = interactive, blurred = dimmed overlay) */
	sendFocusState(state: 'active' | 'blurred'): void {
		if (this.state !== 'connected') return;
		this.sendToViewer({
			type: 'orchestrator:focus',
			payload: { state },
		});
	}

	/** Send a case change to an already-open viewer */
	sendCaseChange(caseId: string, accession: string): void {
		if (this.state !== 'connected') return;
		this.sendToViewer({
			type: 'orchestrator:case-change',
			payload: { caseId, accession },
		});
	}

	/** Close the viewer window */
	close(): void {
		console.warn('[ViewerBridge] close() called — state=' + this.state + ', windowAlive=' + (!!this.viewerWindow && !this.viewerWindow?.closed));
		this.sendToViewer({ type: 'orchestrator:logout', payload: {} });
		this.cleanup();
		if (this.viewerWindow && !this.viewerWindow.closed) {
			this.viewerWindow.close();
		}
		this.viewerWindow = null;
		this.setState('closed');
	}

	/** Check if the viewer window is alive */
	isAlive(): boolean {
		return this.state === 'connected' && !!this.viewerWindow && !this.viewerWindow.closed;
	}

	/** Get current state */
	getState(): ViewerBridgeState {
		return this.state;
	}

	/** Subscribe to events */
	on<K extends keyof ViewerBridgeEvents>(event: K, handler: ViewerBridgeEvents[K]): () => void {
		if (!this.eventListeners.has(event)) {
			this.eventListeners.set(event, new Set());
		}
		this.eventListeners.get(event)!.add(handler);
		return () => this.eventListeners.get(event)?.delete(handler);
	}

	/** Destroy the bridge and all resources */
	destroy(): void {
		this.close();
		this.eventListeners.clear();
	}

	// ============ Private Methods ============

	private setState(state: ViewerBridgeState): void {
		this.state = state;
		this.emit('stateChange', state);
	}

	private emit<K extends keyof ViewerBridgeEvents>(event: K, ...args: Parameters<ViewerBridgeEvents[K]>): void {
		const listeners = this.eventListeners.get(event);
		if (listeners) {
			for (const listener of listeners) {
				(listener as Function)(...args);
			}
		}
	}

	private sendToViewer(message: OrchestratorMessage): void {
		if (!this.viewerWindow || this.viewerWindow.closed) return;
		if (!this.viewerOrigin) return; // Never send without origin validation
		try {
			this.viewerWindow.postMessage(message, this.viewerOrigin);
		} catch (error) {
			console.warn('[ViewerBridge] Failed to send message:', error);
		}
	}

	private handleMessage(event: MessageEvent): void {
		// Origin validation — only accept messages from the expected viewer origin
		if (this.viewerOrigin && event.origin !== this.viewerOrigin) {
			return;
		}

		// Ensure message is from our viewer window
		if (event.source !== this.viewerWindow) {
			return;
		}

		const message = event.data as ViewerMessage;
		if (!message || typeof message.type !== 'string' || !message.type.startsWith('viewer:')) {
			return;
		}

		switch (message.type) {
			case 'viewer:ready':
				this.handleViewerReady();
				break;
			case 'viewer:heartbeat-ack':
				// Heartbeat acknowledged — viewer is alive
				break;
			case 'viewer:case-loaded':
				this.emit('caseLoaded', message.payload);
				break;
			case 'viewer:error':
				this.emit('viewerError', message.payload);
				break;
			case 'viewer:audit-event':
				this.handleAuditEvent(message.payload);
				break;
		}
	}

	private handleViewerReady(): void {
		if (!this.config) return;

		this.setState('connected');

		// Send initialization payload
		const initPayload: InitPayload = {
			token: this.config.token,
			caseId: this.config.caseId,
			accession: this.config.accession,
			tileServerUrl: this.config.tileServerUrl,
			sessionServiceUrl: this.config.sessionServiceUrl,
			userId: this.config.userId,
			orchestratorOrigin: window.location.origin,
			mode: this.config.mode,
		};

		this.sendToViewer({ type: 'orchestrator:init', payload: initPayload });

		// Start heartbeat
		this.startHeartbeat();

		// Schedule token refresh
		this.scheduleTokenRefresh(this.config.token);
	}

	private startHeartbeat(): void {
		this.stopHeartbeat();
		this.heartbeatInterval = setInterval(() => {
			this.sendToViewer({
				type: 'orchestrator:heartbeat',
				payload: { timestamp: Date.now() },
			});
		}, 5000); // Every 5 seconds
	}

	private stopHeartbeat(): void {
		if (this.heartbeatInterval) {
			clearInterval(this.heartbeatInterval);
			this.heartbeatInterval = null;
		}
	}

	/** Schedule a token refresh 60 seconds before expiry */
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
			const refreshAt = expiresAt - 60000; // 60 seconds before expiry
			const delay = refreshAt - Date.now();

			if (delay <= 0) {
				// Token is already about to expire — refresh now
				this.refreshToken();
				return;
			}

			this.tokenRefreshTimeout = setTimeout(() => this.refreshToken(), delay);
		} catch {
			// Can't parse token — skip refresh scheduling
		}
	}

	private async refreshToken(): Promise<void> {
		if (!this.mintToken) return;

		try {
			const newToken = await this.mintToken();
			if (newToken && this.state === 'connected') {
				this.sendToViewer({
					type: 'orchestrator:token-refresh',
					payload: { token: newToken },
				});
				// Update config for future reference
				if (this.config) {
					this.config.token = newToken;
				}
				// Schedule next refresh
				this.scheduleTokenRefresh(newToken);
			}
		} catch (error) {
			console.error('[ViewerBridge] Token refresh failed:', error);
		}
	}

	private handleAuditEvent(event: ViewerAuditEvent): void {
		this.auditEventBatch.push(event);
		this.emit('auditEvent', event);

		// Flush batch after 5 seconds of collecting
		if (!this.auditFlushTimeout) {
			this.auditFlushTimeout = setTimeout(() => this.flushAuditEvents(), 5000);
		}
	}

	private async flushAuditEvents(): Promise<void> {
		this.auditFlushTimeout = null;
		if (this.auditEventBatch.length === 0) return;

		const events = [...this.auditEventBatch];
		this.auditEventBatch = [];

		try {
			await fetch('/api/viewer-events', {
				method: 'POST',
				credentials: 'include',
				headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
				body: JSON.stringify({ events }),
			});
		} catch (error) {
			console.warn('[ViewerBridge] Failed to flush audit events:', error);
			// Re-queue failed events
			this.auditEventBatch.unshift(...events);
		}
	}

	private handleViewerClosed(): void {
		console.log('[ViewerBridge] handleViewerClosed() — viewer window detected as closed');
		this.cleanup();
		this.viewerWindow = null;
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
			this.flushAuditEvents(); // Final flush
		}
		if (this.messageHandler) {
			window.removeEventListener('message', this.messageHandler);
			this.messageHandler = null;
		}
	}
}
