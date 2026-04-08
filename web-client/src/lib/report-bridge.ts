import { ModuleBridge } from './bridges/module-bridge';
import type { ModuleBridgeEventMap } from './types/module-bridge';
import type { ReportInitPayload, ReportLaunchConfig, ReportMessage } from './types/report-bridge';

export interface ReportBridgeEvents extends ModuleBridgeEventMap {
	stateUpdate: (data: { key: string; value: unknown }) => void;
	reportError: (data: { code: string; message: string }) => void;
	reportFinalized: (data: { accession: string }) => void;
}

export class ReportBridge extends ModuleBridge<
	ReportLaunchConfig,
	ReportMessage,
	unknown,
	ReportBridgeEvents
> {
	protected getModuleUrl(config: ReportLaunchConfig): string {
		return config.reportUrl;
	}

	protected getModuleOrigin(config: ReportLaunchConfig): string {
		return config.reportOrigin;
	}

	protected getWindowName(): string {
		return 'starling-report';
	}

	protected isModuleMessage(message: unknown): message is ReportMessage {
		return !!message && typeof message === 'object' && 'type' in message && typeof (message as { type: unknown }).type === 'string' && (((message as { type: string }).type).startsWith('module:') || (message as { type: string }).type.startsWith('report:'));
	}

	protected isReadyMessage(message: ReportMessage): boolean {
		return message.type === 'module:ready';
	}

	protected buildInitMessage(config: ReportLaunchConfig) {
		const payload: ReportInitPayload = {
			token: config.token,
			userId: config.userId,
			orchestratorOrigin: window.location.origin,
			caseId: config.caseId,
			accession: config.accession,
			role: config.role,
		};

		return { type: 'orchestrator:init', payload };
	}

	protected handleModuleMessage(message: ReportMessage): void {
		switch (message.type) {
			case 'module:heartbeat-ack':
				break;
			case 'module:state-update':
				this.emit('stateUpdate', message.payload);
				break;
			case 'module:error':
				this.emit('reportError', message.payload);
				break;
			case 'module:audit-event':
				this.handleAuditEvent(message.payload);
				break;
			case 'report:finalized':
				this.emit('reportFinalized', message.payload);
				break;
			case 'module:ready':
				break;
		}
	}
}
