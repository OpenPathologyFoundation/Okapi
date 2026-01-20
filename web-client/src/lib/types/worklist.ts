// ============================================
// Pathology Worklist TypeScript Types
// Trace: qms/dhf/04-SDS/05-Worklist-Architecture.md
// ============================================

// ============================================
// ENUMS AND TYPES
// ============================================

/** Pathology service lines */
export type PathologyService = 'SURGICAL' | 'CYTOLOGY' | 'HEMATOLOGY' | 'AUTOPSY';

/** Case workflow status */
export type CaseStatus =
	| 'ACCESSIONED'
	| 'GROSSING'
	| 'PROCESSING'
	| 'SLIDES_CUT'
	| 'PENDING_SIGNOUT'
	| 'UNDER_REVIEW'
	| 'SIGNED_OUT'
	| 'AMENDED';

/** Case priority */
export type CasePriority = 'STAT' | 'URGENT' | 'ROUTINE';

// ============================================
// WORKLIST ITEM
// ============================================

/** Worklist item response from API */
export interface WorklistItem {
	id: number;
	accessionNumber: string;
	patientMrn: string | null;
	patientDisplay: string | null;
	service: PathologyService;
	specimenType: string | null;
	specimenSite: string | null;
	status: CaseStatus;
	lisStatus: string | null;
	wsiStatus: string | null;
	authoringStatus: string | null;
	priority: CasePriority;
	assignedToId: number | null;
	assignedToDisplay: string | null;
	slideCount: number;
	slidePending: number;
	slideScanned: number;
	caseDate: string;
	receivedAt: string;
	collectedAt: string | null;
	annotations: Array<Record<string, unknown>>;
	alerts: Array<Record<string, unknown>>;
	metadata: Record<string, unknown>;
}

// ============================================
// API RESPONSE TYPES
// ============================================

/** Worklist counts by category */
export interface WorklistCounts {
	total: number;
	myCases: number;
	byService: Record<PathologyService, number>;
	byStatus: Record<CaseStatus, number>;
	byPriority: Record<CasePriority, number>;
}

/** Paginated worklist response */
export interface WorklistPageResponse {
	items: WorklistItem[];
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
	counts: WorklistCounts;
}

// ============================================
// FILTER AND SORT STATE
// ============================================

/** Worklist filter state */
export interface WorklistFilters {
	services: PathologyService[];
	statuses: CaseStatus[];
	priorities: CasePriority[];
	myCasesOnly: boolean;
	search: string;
	fromDate: string | null;
	toDate: string | null;
}

/** Sort field options */
export type WorklistSortField =
	| 'caseDate'
	| 'accessionNumber'
	| 'status'
	| 'priority'
	| 'service'
	| 'patientDisplay';

/** Sort direction */
export type SortDirection = 'asc' | 'desc';

/** Sort state */
export interface WorklistSortState {
	field: WorklistSortField;
	direction: SortDirection;
}

// ============================================
// DISPLAY LABELS
// ============================================

export const SERVICE_LABELS: Record<PathologyService, string> = {
	SURGICAL: 'Surgical',
	CYTOLOGY: 'Cytology',
	HEMATOLOGY: 'Hematology',
	AUTOPSY: 'Autopsy'
};

export const STATUS_LABELS: Record<CaseStatus, string> = {
	ACCESSIONED: 'Accessioned',
	GROSSING: 'Grossing',
	PROCESSING: 'Processing',
	SLIDES_CUT: 'Slides Cut',
	PENDING_SIGNOUT: 'Pending Sign-out',
	UNDER_REVIEW: 'Under Review',
	SIGNED_OUT: 'Signed Out',
	AMENDED: 'Amended'
};

export const PRIORITY_LABELS: Record<CasePriority, string> = {
	STAT: 'STAT',
	URGENT: 'Urgent',
	ROUTINE: 'Routine'
};

// ============================================
// DISPLAY COLORS (CSS variable-based for theme support)
// These use CSS custom properties defined in layout.css
// that automatically adapt to light/dark mode
// ============================================

export const SERVICE_COLORS: Record<PathologyService, string> = {
	SURGICAL: 'bg-worklist-service-surgical/15 text-worklist-service-surgical border-worklist-service-surgical/30',
	CYTOLOGY: 'bg-worklist-service-cytology/15 text-worklist-service-cytology border-worklist-service-cytology/30',
	HEMATOLOGY: 'bg-worklist-service-hematology/15 text-worklist-service-hematology border-worklist-service-hematology/30',
	AUTOPSY: 'bg-worklist-service-autopsy/15 text-worklist-service-autopsy border-worklist-service-autopsy/30'
};

export const STATUS_COLORS: Record<CaseStatus, string> = {
	ACCESSIONED: 'bg-worklist-status-accessioned/15 text-worklist-status-accessioned border-worklist-status-accessioned/30',
	GROSSING: 'bg-worklist-status-grossing/15 text-worklist-status-grossing border-worklist-status-grossing/30',
	PROCESSING: 'bg-worklist-status-processing/15 text-worklist-status-processing border-worklist-status-processing/30',
	SLIDES_CUT: 'bg-worklist-status-slides-cut/15 text-worklist-status-slides-cut border-worklist-status-slides-cut/30',
	PENDING_SIGNOUT: 'bg-worklist-status-pending-signout/15 text-worklist-status-pending-signout border-worklist-status-pending-signout/30',
	UNDER_REVIEW: 'bg-worklist-status-under-review/15 text-worklist-status-under-review border-worklist-status-under-review/30',
	SIGNED_OUT: 'bg-worklist-status-signed-out/15 text-worklist-status-signed-out border-worklist-status-signed-out/30',
	AMENDED: 'bg-worklist-status-amended/15 text-worklist-status-amended border-worklist-status-amended/30'
};

export const PRIORITY_COLORS: Record<CasePriority, string> = {
	STAT: 'bg-worklist-priority-stat/15 text-worklist-priority-stat border-worklist-priority-stat/30',
	URGENT: 'bg-worklist-priority-urgent/15 text-worklist-priority-urgent border-worklist-priority-urgent/30',
	ROUTINE: 'bg-worklist-priority-routine/15 text-worklist-priority-routine border-worklist-priority-routine/30'
};

// ============================================
// HELPER FUNCTIONS
// ============================================

/** Get all services for filter options */
export const ALL_SERVICES: PathologyService[] = ['SURGICAL', 'CYTOLOGY', 'HEMATOLOGY', 'AUTOPSY'];

/** Get all statuses for filter options */
export const ALL_STATUSES: CaseStatus[] = [
	'ACCESSIONED',
	'GROSSING',
	'PROCESSING',
	'SLIDES_CUT',
	'PENDING_SIGNOUT',
	'UNDER_REVIEW',
	'SIGNED_OUT',
	'AMENDED'
];

/** Get active (non-completed) statuses */
export const ACTIVE_STATUSES: CaseStatus[] = [
	'ACCESSIONED',
	'GROSSING',
	'PROCESSING',
	'SLIDES_CUT',
	'PENDING_SIGNOUT',
	'UNDER_REVIEW'
];

/** Get all priorities for filter options */
export const ALL_PRIORITIES: CasePriority[] = ['STAT', 'URGENT', 'ROUTINE'];

/** Check if status is a completed status */
export function isCompletedStatus(status: CaseStatus): boolean {
	return status === 'SIGNED_OUT' || status === 'AMENDED';
}

/** Format slide count display */
export function formatSlideCount(scanned: number, total: number): string {
	if (total === 0) return '—';
	return `${scanned}/${total}`;
}

/** Calculate case age in days from case date */
export function getCaseAgeDays(caseDate: string): number {
	const date = new Date(caseDate);
	const now = new Date();
	const diffTime = now.getTime() - date.getTime();
	return Math.floor(diffTime / (1000 * 60 * 60 * 24));
}

/** Get human-readable case age */
export function formatCaseAge(caseDate: string): string {
	const days = getCaseAgeDays(caseDate);
	if (days === 0) return 'Today';
	if (days === 1) return 'Yesterday';
	if (days < 7) return `${days} days ago`;
	if (days < 30) return `${Math.floor(days / 7)} weeks ago`;
	return `${Math.floor(days / 30)} months ago`;
}
