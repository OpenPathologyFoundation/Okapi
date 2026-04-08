/**
 * Viewer Bridge — Orchestrator Side
 *
 * Manages the communication channel between the Starling orchestrator
 * and the digital-viewer window via postMessage.
 */

import { ModuleBridge } from './bridges/module-bridge';
import type { ModuleBridgeEventMap } from './types/module-bridge';
import type {
	BridgeMessage,
	InitPayload,
	OrchestratorMessage,
	ViewerAuditEvent,
	ViewerLaunchConfig,
	ViewerMessage,
} from './types/viewer-bridge';

export type ViewerBridgeState = 'idle' | 'launching' | 'connected' | 'closed';

export interface ViewerBridgeEvents extends ModuleBridgeEventMap<ViewerAuditEvent> {
	caseLoaded: (data: { caseId: string; slideCount: number }) => void;
	viewerError: (data: { code: string; message: string }) => void;
}

export class ViewerBridge extends ModuleBridge<
	ViewerLaunchConfig,
	ViewerMessage,
	ViewerAuditEvent,
	ViewerBridgeEvents
> {
	constructor(options?: { mintToken?: () => Promise<string | null> }) {
		super({
			mintToken: options?.mintToken,
			auditEndpoint: '/api/viewer-events',
		});
	}

	/** Send a case change to an already-open viewer */
	sendCaseChange(caseId: string, accession: string): void {
		if (this.state !== 'connected') return;
		this.sendToModule({
			type: 'orchestrator:case-change',
			payload: { caseId, accession },
		});
	}

	protected getModuleUrl(config: ViewerLaunchConfig): string {
		return config.viewerUrl;
	}

	protected getModuleOrigin(config: ViewerLaunchConfig): string {
		return config.viewerOrigin;
	}

	protected getWindowName(): string {
		return 'okapi-viewer';
	}

	protected isModuleMessage(message: unknown): message is ViewerMessage {
		return !!message && typeof message === 'object' && 'type' in message && typeof (message as BridgeMessage).type === 'string' && (message as BridgeMessage).type.startsWith('viewer:');
	}

	protected isReadyMessage(message: ViewerMessage): boolean {
		return message.type === 'viewer:ready';
	}

	protected buildInitMessage(config: ViewerLaunchConfig): OrchestratorMessage {
		const initPayload: InitPayload = {
			token: config.token,
			caseId: config.caseId,
			accession: config.accession,
			tileServerUrl: config.tileServerUrl,
			sessionServiceUrl: config.sessionServiceUrl,
			userId: config.userId,
			orchestratorOrigin: window.location.origin,
			mode: config.mode,
		};

		return { type: 'orchestrator:init', payload: initPayload };
	}

	protected handleLaunchFailure(): void {
		console.error('[ViewerBridge] Failed to open viewer window — popup blocked?');
	}

	protected handleModuleMessage(message: ViewerMessage): void {
		switch (message.type) {
			case 'viewer:heartbeat-ack':
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
			case 'viewer:ready':
				break;
		}
	}

	protected override sendToModule(message: OrchestratorMessage): void {
		super.sendToModule(message);
	}
}
