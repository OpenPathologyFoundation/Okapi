import type { BaseInitPayload, ModuleAuditEvent, ModuleBaseMessage } from '@starling/module-protocol';
import type { ModuleLaunchConfig } from './module-bridge';

export interface ReportInitPayload extends BaseInitPayload {
	caseId: string;
	accession: string;
	role?: string;
}

export interface ReportAuditEvent extends ModuleAuditEvent {
	eventType: 'REPORT_OPENED' | 'REPORT_SAVED' | 'REPORT_FINALIZED';
	caseId: string;
	accession: string;
}

export type ReportMessage =
	| ModuleBaseMessage
	| { type: 'report:finalized'; payload: { accession: string } };

export interface ReportLaunchConfig extends ModuleLaunchConfig {
	caseId: string;
	accession: string;
	userId: string;
	reportUrl: string;
	reportOrigin: string;
	role?: string;
}
