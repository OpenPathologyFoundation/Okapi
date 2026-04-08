import type { ModuleBridgeState } from '$lib/types/module-bridge';
import type { ModuleBridge } from '$lib/bridges/module-bridge';
import type { ViewerMode } from '$lib/types/viewer-bridge';

export type ActivityMountStrategy = 'internal' | 'window' | 'iframe';
export type ActivityType = 'internal' | 'external';
export type ActivityBridgeKind = 'viewer' | 'report';

export interface ActivityMountDefinition {
	strategy: ActivityMountStrategy;
	windowFeatures?: string;
	protocolVersion?: '1.0';
	slot?: string;
}

export interface ActivityDefinition {
	id: string;
	label: string;
	type: ActivityType;
	path: string;
	caseScoped: boolean;
	mount?: ActivityMountDefinition;
	bridge?: ActivityBridgeKind;
	requiredPermissions?: string[];
}

export interface ViewerActivityContext {
	caseId: string;
	accession: string;
	tileServerUrl: string;
	sessionServiceUrl: string;
	userId: string;
	mode?: ViewerMode;
}

export interface ReportActivityContext {
	caseId: string;
	accession: string;
	userId: string;
	role?: string;
}

export type ActivityContext = ViewerActivityContext | ReportActivityContext;

export interface ActivityModuleState {
	slideCount?: number;
	mode?: ViewerMode;
	[key: string]: unknown;
}

export interface ActivityError {
	code: string;
	message: string;
}

export interface ActivityInstance {
	activityId: string;
	status: ModuleBridgeState;
	caseId: string | null;
	accession: string | null;
	bridge: ModuleBridge<any, any, any, any> | null;
	moduleState: ActivityModuleState;
	lastError: ActivityError | null;
	visible: boolean;
	launchConfig?: unknown;
}
