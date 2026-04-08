export type ProtocolVersion = '1.0';

export interface ModuleMessage<TPayload = unknown> {
	type: string;
	payload: TPayload;
}

export interface BaseInitPayload {
	token: string;
	userId: string;
	orchestratorOrigin: string;
}

export type OrchestratorBaseMessage =
	| { type: 'orchestrator:init'; payload: BaseInitPayload }
	| { type: 'orchestrator:token-refresh'; payload: { token: string } }
	| { type: 'orchestrator:focus'; payload: { state: 'active' | 'blurred' } }
	| { type: 'orchestrator:context-update'; payload: { key: string; value: unknown } }
	| { type: 'orchestrator:heartbeat'; payload: { timestamp: number } }
	| { type: 'orchestrator:logout'; payload: Record<string, never> };

export interface ModuleAuditEvent {
	eventType: string;
	occurredAt: string;
	metadata?: Record<string, unknown>;
}

export type ModuleBaseMessage =
	| { type: 'module:ready'; payload: Record<string, never> }
	| { type: 'module:heartbeat-ack'; payload: { timestamp: number } }
	| { type: 'module:error'; payload: { code: string; message: string } }
	| { type: 'module:state-update'; payload: { key: string; value: unknown } }
	| { type: 'module:audit-event'; payload: ModuleAuditEvent };
