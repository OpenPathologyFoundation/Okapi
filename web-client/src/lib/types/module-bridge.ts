import type { ModuleMessage } from '@starling/module-protocol';

export type BridgeMessage<TPayload = unknown> = ModuleMessage<TPayload>;

export type ModuleBridgeState = 'idle' | 'launching' | 'connected' | 'closed';

export interface ModuleLaunchConfig {
	/** JWT access token used by the external module */
	token: string;
}

export interface ModuleBridgeOptions {
	mintToken?: () => Promise<string | null>;
	auditEndpoint?: string;
}

export interface ModuleBridgeEventMap<TAuditEvent = unknown> {
	stateChange: (state: ModuleBridgeState) => void;
	auditEvent: (event: TAuditEvent) => void;
}
