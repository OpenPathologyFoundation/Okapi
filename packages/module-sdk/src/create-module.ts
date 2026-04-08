import type {
	BaseInitPayload,
	ModuleAuditEvent,
	ModuleBaseMessage,
	OrchestratorBaseMessage,
} from '@starling/module-protocol';

export interface CreateStarlingModuleOptions<TInitPayload extends BaseInitPayload = BaseInitPayload> {
	expectedOrigin: string;
	onInit?: (payload: TInitPayload) => void;
	onTokenRefresh?: (payload: { token: string }) => void;
	onFocus?: (payload: { state: 'active' | 'blurred' }) => void;
	onContextUpdate?: (payload: { key: string; value: unknown }) => void;
	onHeartbeat?: (payload: { timestamp: number }) => void;
	onLogout?: () => void;
}

export interface StarlingModuleClient {
	ready: () => void;
	error: (code: string, message: string) => void;
	updateState: (key: string, value: unknown) => void;
	auditEvent: (event: ModuleAuditEvent) => void;
	emit: (type: string, payload: unknown) => void;
	handleMessage: (event: MessageEvent<OrchestratorBaseMessage>) => void;
}

export function createStarlingModule<TInitPayload extends BaseInitPayload = BaseInitPayload>(
	options: CreateStarlingModuleOptions<TInitPayload>,
): StarlingModuleClient {
	function getHostWindow(): Window | null {
		if (window.opener && !window.opener.closed) return window.opener;
		if (window.parent && window.parent !== window) return window.parent;
		return null;
	}

	function postMessage(message: ModuleBaseMessage): void {
		getHostWindow()?.postMessage(message, options.expectedOrigin);
	}

	function handleMessage(event: MessageEvent<OrchestratorBaseMessage>): void {
		if (event.origin !== options.expectedOrigin) return;

		switch (event.data.type) {
			case 'orchestrator:init':
				options.onInit?.(event.data.payload as TInitPayload);
				break;
			case 'orchestrator:token-refresh':
				options.onTokenRefresh?.(event.data.payload);
				break;
			case 'orchestrator:focus':
				options.onFocus?.(event.data.payload);
				break;
			case 'orchestrator:context-update':
				options.onContextUpdate?.(event.data.payload);
				break;
			case 'orchestrator:heartbeat':
				options.onHeartbeat?.(event.data.payload);
				postMessage({
					type: 'module:heartbeat-ack',
					payload: { timestamp: event.data.payload.timestamp },
				});
				break;
			case 'orchestrator:logout':
				options.onLogout?.();
				break;
		}
	}

	return {
		ready() {
			postMessage({ type: 'module:ready', payload: {} });
		},
		error(code: string, message: string) {
			postMessage({ type: 'module:error', payload: { code, message } });
		},
		updateState(key: string, value: unknown) {
			postMessage({ type: 'module:state-update', payload: { key, value } });
		},
		auditEvent(event: ModuleAuditEvent) {
			postMessage({ type: 'module:audit-event', payload: event });
		},
		emit(type: string, payload: unknown) {
			getHostWindow()?.postMessage({ type, payload }, options.expectedOrigin);
		},
		handleMessage,
	};
}
