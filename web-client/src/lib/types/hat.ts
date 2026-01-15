// ============================================
// HAT (Histology Asset Tracking) TypeScript Types
// Trace: qms/dhf/04-SDS/04-HAT-Architecture.md
// ============================================

// ============================================
// ASSET FACTS (State)
// ============================================

/** Identifier types for crosswalk support */
export type IdentifierType =
	| 'BARCODE_RAW'
	| 'BARCODE_NORMALIZED'
	| 'LIS_ACCESSION'
	| 'LIS_PART'
	| 'EXTERNAL_CONSULT';

/** Asset identifier with provenance */
export interface AssetIdentifier {
	type: IdentifierType;
	value: string;
	source: string;
	createdAt: string;
}

/** Asset status enum */
export type AssetStatus =
	| 'AVAILABLE'
	| 'CHECKED_OUT'
	| 'IN_TRANSIT'
	| 'IN_QA'
	| 'DISTRIBUTED'
	| 'RETURNED'
	| 'DESTROYED'
	| 'MISSING';

/** Asset location */
export interface AssetLocation {
	facility: string;
	building?: string;
	room?: string;
	storage?: string;
	assertedAt: string;
	assertedBy: string;
}

/** Custody record */
export interface AssetCustody {
	holder: string;
	holderDisplay: string;
	checkedOutAt: string;
	dueBackAt?: string;
	purpose?: string;
}

/** Provenance for state assertions */
export interface Provenance {
	source: 'OKAPI' | 'COPATH' | 'BEAKER' | 'MANUAL' | 'EXTERNAL';
	assertedBy: string;
	assertedAt: string;
	comment?: string;
}

/** Current asset state (projection) - SYS-HAT-004 */
export interface AssetCurrentState {
	id: string;
	identifiers: AssetIdentifier[];
	status: AssetStatus;
	location: AssetLocation;
	custody?: AssetCustody;
	provenance: Provenance;
	lifecycleFlags: {
		isPlaceholder: boolean;
		hasConflict: boolean;
		isHighRisk: boolean;
	};
	updatedAt: string;
}

/** Event types for append-only history */
export type AssetEventType =
	| 'CREATED'
	| 'STATUS_CHANGED'
	| 'LOCATION_CHANGED'
	| 'CUSTODY_CHANGED'
	| 'IDENTIFIER_ADDED'
	| 'CORRECTION'
	| 'RECONCILED'
	| 'REQUEST_LINKED'
	| 'MILESTONE_REACHED';

/** Asset event (append-only history) - SYS-HAT-005 */
export interface AssetEvent {
	id: string;
	assetId: string;
	type: AssetEventType;
	timestamp: string;
	actor: {
		id: string;
		display: string;
		role?: string;
	};
	payload: Record<string, unknown>;
	linkedRequestId?: string;
	comment?: string;
	supersedes?: string;
}

// ============================================
// LOOKUP OUTCOMES - SYS-HAT-002
// ============================================

export type LookupOutcome = 'MATCHED' | 'NOT_FOUND' | 'AMBIGUOUS';

export interface LookupResult {
	outcome: LookupOutcome;
	query: {
		raw: string;
		normalized: string;
	};
	match?: AssetCurrentState;
	candidates?: AssetCurrentState[];
	message?: string;
}

// ============================================
// INTENT/WORK (Requests + Execution)
// ============================================

/** Request action types */
export type RequestAction =
	| 'PULL'
	| 'STAIN'
	| 'RECUT'
	| 'QA'
	| 'SCAN'
	| 'DISTRIBUTE'
	| 'RETURN'
	| 'DESTROY';

/** Request priority */
export type RequestPriority = 'ROUTINE' | 'URGENT' | 'STAT';

/** Request status - SYS-HAT-008 */
export type RequestStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

/** Asset execution status within a request */
export type AssetExecutionStatus =
	| 'PENDING'
	| 'IN_PROGRESS'
	| 'COMPLETED'
	| 'SKIPPED'
	| 'MISSING'
	| 'SUBSTITUTED';

/** Per-asset execution record */
export interface RequestAssetExecution {
	assetId: string;
	asset?: AssetCurrentState;
	status: AssetExecutionStatus;
	scanConfirmedAt?: string;
	completedAt?: string;
	evidence?: Record<string, string>;
	notes?: string;
}

/** Work request - SYS-HAT-007 */
export interface WorkRequest {
	id: string;
	actions: RequestAction[];
	priority: RequestPriority;
	status: RequestStatus;
	assetSelectors?: string[];
	assets: RequestAssetExecution[];
	requester: {
		id: string;
		display: string;
		isExternal?: boolean;
	};
	assignee?: {
		id?: string;
		team?: string;
		display: string;
	};
	dueDate?: string;
	createdAt: string;
	updatedAt: string;
	completionEvidence?: {
		trackingNumber?: string;
		destination?: string;
		receiptConfirmed?: boolean;
		returnExpected?: string;
	};
}

// ============================================
// RBAC / PERMISSIONS - SYS-HAT-013
// ============================================

export type HatPermission =
	| 'hat:asset:view'
	| 'hat:asset:edit'
	| 'hat:request:view'
	| 'hat:request:create'
	| 'hat:request:execute'
	| 'hat:distribute:external'
	| 'hat:distribute:research'
	| 'hat:admin';

export interface HatUser {
	id: string;
	display: string;
	role: string;
	permissions: HatPermission[];
}

// ============================================
// UI STATE
// ============================================

export interface HatSearchFilters {
	status?: AssetStatus[];
	location?: string;
	dateRange?: {
		from: string;
		to: string;
	};
	accession?: string;
	stain?: string;
}

export type HatViewMode = 'scan' | 'search' | 'list';

// ============================================
// DISPLAY HELPERS
// ============================================

export const STATUS_LABELS: Record<AssetStatus, string> = {
	AVAILABLE: 'Available',
	CHECKED_OUT: 'Checked Out',
	IN_TRANSIT: 'In Transit',
	IN_QA: 'In QA',
	DISTRIBUTED: 'Distributed',
	RETURNED: 'Returned',
	DESTROYED: 'Destroyed',
	MISSING: 'Missing'
};

export const PRIORITY_LABELS: Record<RequestPriority, string> = {
	STAT: 'STAT',
	URGENT: 'Urgent',
	ROUTINE: 'Routine'
};

export const ACTION_LABELS: Record<RequestAction, string> = {
	PULL: 'Pull',
	STAIN: 'Stain',
	RECUT: 'Recut',
	QA: 'QA Review',
	SCAN: 'Scan',
	DISTRIBUTE: 'Distribute',
	RETURN: 'Return',
	DESTROY: 'Destroy'
};

export const REQUEST_STATUS_LABELS: Record<RequestStatus, string> = {
	OPEN: 'Open',
	IN_PROGRESS: 'In Progress',
	DONE: 'Done',
	CANCELLED: 'Cancelled'
};
