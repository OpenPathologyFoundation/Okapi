import { browser } from '$app/environment';
import type {
	WorklistItem,
	WorklistFilters,
	WorklistSortState,
	WorklistPageResponse,
	WorklistCounts,
	PathologyService,
	CaseStatus,
	CasePriority,
	WorklistSortField,
	SortDirection
} from '$lib/types/worklist';
import { ACTIVE_STATUSES } from '$lib/types/worklist';

// ============================================
// MOCK PATHOLOGISTS (matches seed data)
// In production, this would come from auth context
// ============================================
export const MOCK_PATHOLOGISTS = [
	{ id: 1, username: 'path.rumtugger', display: 'Dr. Rum Tug Tugger' },
	{ id: 2, username: 'path.skimbleshanks', display: 'Dr. Skimbleshanks' },
	{ id: 3, username: 'path.deuteronomy', display: 'Dr. Old Deuteronomy' },
	{ id: 4, username: 'path.mungojerrie', display: 'Dr. Mungojerrie' }
] as const;

// Default logged-in user for development (Dr. Rum Tug Tugger)
const DEFAULT_CURRENT_USER = MOCK_PATHOLOGISTS[0];

class WorklistStore {
	// Current user context (in production, from auth)
	currentUser = $state<{ id: number; username: string; display: string }>(DEFAULT_CURRENT_USER);

	// Data state
	items = $state<WorklistItem[]>([]);
	counts = $state<WorklistCounts | null>(null);

	// Pagination
	page = $state(0);
	pageSize = $state(50);
	totalItems = $state(0);
	totalPages = $state(0);

	// Filters
	filters = $state<WorklistFilters>({
		services: [],
		statuses: [],
		priorities: [],
		myCasesOnly: false,
		search: '',
		fromDate: null,
		toDate: null
	});

	// Sort
	sort = $state<WorklistSortState>({
		field: 'caseDate',
		direction: 'desc'
	});

	// UI state
	isLoading = $state(false);
	error = $state<string | null>(null);
	lastRefreshTime = $state<Date | null>(null);

	constructor() {
		if (browser) {
			this.loadFiltersFromStorage();
		}
	}

	private loadFiltersFromStorage() {
		const stored = localStorage.getItem('worklist_filters');
		if (stored) {
			try {
				const parsed = JSON.parse(stored);
				// Don't restore myCasesOnly - it's route-dependent
				this.filters = { ...this.filters, ...parsed, myCasesOnly: false };
			} catch {
				// Ignore invalid stored data
			}
		}
	}

	private saveFiltersToStorage() {
		if (browser) {
			// Don't persist myCasesOnly - it's route-dependent
			const { myCasesOnly, ...persistableFilters } = this.filters;
			localStorage.setItem('worklist_filters', JSON.stringify(persistableFilters));
		}
	}

	// Derived state: filtered items (client-side filtering for mock mode)
	// Using $derived.by for reactive computed values in Svelte 5
	filteredItems = $derived.by(() => {
		let result = [...this.items];

		// My Cases filter - filter by current user's assigned cases
		if (this.filters.myCasesOnly) {
			result = result.filter((item) => item.assignedToId === this.currentUser.id);
		}

		// Service filter
		if (this.filters.services.length > 0) {
			result = result.filter((item) => this.filters.services.includes(item.service));
		}

		// Status filter
		if (this.filters.statuses.length > 0) {
			result = result.filter((item) => this.filters.statuses.includes(item.status));
		}

		// Priority filter
		if (this.filters.priorities.length > 0) {
			result = result.filter((item) => this.filters.priorities.includes(item.priority));
		}

		// Search filter
		if (this.filters.search) {
			const search = this.filters.search.toLowerCase();
			result = result.filter(
				(item) =>
					item.accessionNumber.toLowerCase().includes(search) ||
					item.patientDisplay?.toLowerCase().includes(search) ||
					item.patientMrn?.toLowerCase().includes(search)
			);
		}

		return result;
	});

	// Derived state: sorted items
	// Using $derived.by for reactive computed values in Svelte 5
	sortedItems = $derived.by(() => {
		const items = [...this.filteredItems];
		const { field, direction } = this.sort;
		const multiplier = direction === 'asc' ? 1 : -1;

		return items.sort((a, b) => {
			let aVal: string | number | null;
			let bVal: string | number | null;

			switch (field) {
				case 'caseDate':
					aVal = a.caseDate;
					bVal = b.caseDate;
					break;
				case 'accessionNumber':
					aVal = a.accessionNumber;
					bVal = b.accessionNumber;
					break;
				case 'status':
					aVal = a.status;
					bVal = b.status;
					break;
				case 'priority':
					// Custom priority order: STAT > URGENT > ROUTINE
					const priorityOrder = { STAT: 0, URGENT: 1, ROUTINE: 2 };
					aVal = priorityOrder[a.priority];
					bVal = priorityOrder[b.priority];
					break;
				case 'service':
					aVal = a.service;
					bVal = b.service;
					break;
				case 'patientDisplay':
					aVal = a.patientDisplay;
					bVal = b.patientDisplay;
					break;
				default:
					return 0;
			}

			if (aVal === null && bVal === null) return 0;
			if (aVal === null) return 1;
			if (bVal === null) return -1;

			if (typeof aVal === 'string' && typeof bVal === 'string') {
				return aVal.localeCompare(bVal) * multiplier;
			}

			return ((aVal as number) - (bVal as number)) * multiplier;
		});
	});

	// Filter actions
	toggleService(service: PathologyService) {
		const idx = this.filters.services.indexOf(service);
		if (idx >= 0) {
			this.filters.services = this.filters.services.filter((s) => s !== service);
		} else {
			this.filters.services = [...this.filters.services, service];
		}
		this.saveFiltersToStorage();
	}

	toggleStatus(status: CaseStatus) {
		const idx = this.filters.statuses.indexOf(status);
		if (idx >= 0) {
			this.filters.statuses = this.filters.statuses.filter((s) => s !== status);
		} else {
			this.filters.statuses = [...this.filters.statuses, status];
		}
		this.saveFiltersToStorage();
	}

	togglePriority(priority: CasePriority) {
		const idx = this.filters.priorities.indexOf(priority);
		if (idx >= 0) {
			this.filters.priorities = this.filters.priorities.filter((p) => p !== priority);
		} else {
			this.filters.priorities = [...this.filters.priorities, priority];
		}
		this.saveFiltersToStorage();
	}

	setMyCasesOnly(value: boolean) {
		this.filters.myCasesOnly = value;
		// Don't persist this - it's route-dependent
	}

	setSearch(value: string) {
		this.filters.search = value;
	}

	setSort(field: WorklistSortField, direction?: SortDirection) {
		if (this.sort.field === field && !direction) {
			// Toggle direction
			this.sort.direction = this.sort.direction === 'asc' ? 'desc' : 'asc';
		} else {
			this.sort.field = field;
			this.sort.direction = direction ?? 'desc';
		}
	}

	clearFilters() {
		this.filters = {
			services: [],
			statuses: [],
			priorities: [],
			myCasesOnly: false,
			search: '',
			fromDate: null,
			toDate: null
		};
		this.saveFiltersToStorage();
	}

	// Show only active (non-completed) cases
	showActiveCasesOnly() {
		this.filters.statuses = [...ACTIVE_STATUSES];
		this.saveFiltersToStorage();
	}

	// Set current user (for development/testing)
	setCurrentUser(userId: number) {
		const user = MOCK_PATHOLOGISTS.find((p) => p.id === userId);
		if (user) {
			this.currentUser = user;
			// Recalculate counts
			if (this.items.length > 0) {
				this.updateCounts();
			}
		}
	}

	private updateCounts() {
		this.counts = {
			total: this.items.length,
			myCases: this.items.filter((i) => i.assignedToId === this.currentUser.id).length,
			byService: {
				SURGICAL: this.items.filter((i) => i.service === 'SURGICAL').length,
				CYTOLOGY: this.items.filter((i) => i.service === 'CYTOLOGY').length,
				HEMATOLOGY: this.items.filter((i) => i.service === 'HEMATOLOGY').length,
				AUTOPSY: this.items.filter((i) => i.service === 'AUTOPSY').length
			},
			byStatus: {
				ACCESSIONED: this.items.filter((i) => i.status === 'ACCESSIONED').length,
				GROSSING: this.items.filter((i) => i.status === 'GROSSING').length,
				PROCESSING: this.items.filter((i) => i.status === 'PROCESSING').length,
				SLIDES_CUT: this.items.filter((i) => i.status === 'SLIDES_CUT').length,
				PENDING_SIGNOUT: this.items.filter((i) => i.status === 'PENDING_SIGNOUT').length,
				UNDER_REVIEW: this.items.filter((i) => i.status === 'UNDER_REVIEW').length,
				SIGNED_OUT: this.items.filter((i) => i.status === 'SIGNED_OUT').length,
				AMENDED: this.items.filter((i) => i.status === 'AMENDED').length
			},
			byPriority: {
				STAT: this.items.filter((i) => i.priority === 'STAT').length,
				URGENT: this.items.filter((i) => i.priority === 'URGENT').length,
				ROUTINE: this.items.filter((i) => i.priority === 'ROUTINE').length
			}
		};
	}

	// Data loading
	async loadWorklist(): Promise<void> {
		this.isLoading = true;
		this.error = null;

		try {
			const params = new URLSearchParams();

			if (this.filters.services.length > 0) {
				this.filters.services.forEach((s) => params.append('services', s));
			}
			if (this.filters.statuses.length > 0) {
				this.filters.statuses.forEach((s) => params.append('statuses', s));
			}
			if (this.filters.priorities.length > 0) {
				this.filters.priorities.forEach((p) => params.append('priorities', p));
			}
			if (this.filters.myCasesOnly) {
				params.set('myCasesOnly', 'true');
			}
			if (this.filters.search) {
				params.set('search', this.filters.search);
			}
			if (this.filters.fromDate) {
				params.set('fromDate', this.filters.fromDate);
			}
			if (this.filters.toDate) {
				params.set('toDate', this.filters.toDate);
			}

			params.set('sortBy', this.sort.field);
			params.set('sortDir', this.sort.direction);
			params.set('page', String(this.page));
			params.set('pageSize', String(this.pageSize));

			const response = await fetch(`/api/worklist?${params.toString()}`);
			if (!response.ok) {
				throw new Error(`Failed to load worklist: ${response.statusText}`);
			}

			const data: WorklistPageResponse = await response.json();
			this.items = data.items;
			this.page = data.page;
			this.totalItems = data.totalItems;
			this.totalPages = data.totalPages;
			this.counts = data.counts;
			this.lastRefreshTime = new Date();
		} catch (e) {
			const message = e instanceof Error ? e.message : 'Unknown error';
			this.error = message;
			throw e;
		} finally {
			this.isLoading = false;
		}
	}

	// Mock data loading for development
	loadMockData() {
		this.isLoading = true;
		this.items = generateMockWorklist();
		this.totalItems = this.items.length;
		this.totalPages = 1;
		this.lastRefreshTime = new Date();
		this.updateCounts();
		this.isLoading = false;
	}

	// Get formatted last refresh time
	getLastRefreshDisplay(): string {
		if (!this.lastRefreshTime) return 'Never';
		const now = new Date();
		const diff = now.getTime() - this.lastRefreshTime.getTime();
		const seconds = Math.floor(diff / 1000);
		const minutes = Math.floor(seconds / 60);

		if (seconds < 10) return 'Just now';
		if (seconds < 60) return `${seconds}s ago`;
		if (minutes < 60) return `${minutes}m ago`;

		return this.lastRefreshTime.toLocaleTimeString('en-US', {
			hour: 'numeric',
			minute: '2-digit'
		});
	}

	// Clear state
	clear() {
		this.items = [];
		this.counts = null;
		this.error = null;
	}

	// Initialize store
	init() {
		if (browser) {
			this.loadFiltersFromStorage();
		}
	}
}

// ============================================
// DETERMINISTIC MOCK DATA GENERATOR
// Creates realistic test scenarios
// ============================================
function generateMockWorklist(): WorklistItem[] {
	const items: WorklistItem[] = [];

	// Helper to create dates relative to today
	const daysAgo = (days: number): string => {
		const date = new Date();
		date.setDate(date.getDate() - days);
		return date.toISOString().split('T')[0];
	};

	const dateTimeAgo = (days: number, hours: number = 0): string => {
		const date = new Date();
		date.setDate(date.getDate() - days);
		date.setHours(date.getHours() - hours);
		return date.toISOString();
	};

	// ============================================
	// DR. RUM TUG TUGGER'S CASES (id: 1) - 10 cases
	// This is the default logged-in user
	// ============================================

	// STAT case - urgent colon biopsy, fully scanned
	items.push({
		id: 1,
		accessionNumber: 'S24-00001',
		patientMrn: 'MRN-100001',
		patientDisplay: 'Smith, John A',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Colon - Ascending',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'DRAFT',
		priority: 'STAT',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 4,
		slidePending: 0,
		slideScanned: 4,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 2),
		collectedAt: dateTimeAgo(0, 6),
		annotations: [],
		alerts: [{ type: 'STAT', message: 'Intraoperative consultation - surgeon waiting' }],
		metadata: {}
	});

	// Urgent breast excision - under review by resident
	items.push({
		id: 2,
		accessionNumber: 'S24-00002',
		patientMrn: 'MRN-100002',
		patientDisplay: 'Johnson, Mary B',
		service: 'SURGICAL',
		specimenType: 'Excision',
		specimenSite: 'Breast - Left',
		status: 'UNDER_REVIEW',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'PENDING_REVIEW',
		priority: 'URGENT',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 12,
		slidePending: 0,
		slideScanned: 12,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 4),
		collectedAt: dateTimeAgo(1, 8),
		annotations: [{ type: 'NOTE', message: 'Margins close - recommend re-excision' }],
		alerts: [],
		metadata: { draftBy: 'Dr. Skimbleshanks', draftDate: daysAgo(0) }
	});

	// Routine prostate biopsy - slides still being cut
	items.push({
		id: 3,
		accessionNumber: 'S24-00003',
		patientMrn: 'MRN-100003',
		patientDisplay: 'Williams, Robert C',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Prostate - 12 cores',
		status: 'SLIDES_CUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 12,
		slidePending: 8,
		slideScanned: 4,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 3),
		collectedAt: dateTimeAgo(2, 5),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// Old case - 5 days, needs attention
	items.push({
		id: 4,
		accessionNumber: 'S24-00004',
		patientMrn: 'MRN-100004',
		patientDisplay: 'Brown, Elizabeth D',
		service: 'SURGICAL',
		specimenType: 'Resection',
		specimenSite: 'Lung - Right Upper Lobe',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'DRAFT',
		priority: 'ROUTINE',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 18,
		slidePending: 0,
		slideScanned: 18,
		caseDate: daysAgo(5),
		receivedAt: dateTimeAgo(5, 2),
		collectedAt: dateTimeAgo(5, 6),
		annotations: [],
		alerts: [{ type: 'AGE', message: 'Case exceeds 4-day TAT threshold' }],
		metadata: {}
	});

	// Cytology case
	items.push({
		id: 5,
		accessionNumber: 'C24-00001',
		patientMrn: 'MRN-100005',
		patientDisplay: 'Davis, Michael E',
		service: 'CYTOLOGY',
		specimenType: 'FNA',
		specimenSite: 'Thyroid - Left Lobe',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 6,
		slidePending: 0,
		slideScanned: 6,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 5),
		collectedAt: dateTimeAgo(1, 6),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// Being grossed
	items.push({
		id: 6,
		accessionNumber: 'S24-00005',
		patientMrn: 'MRN-100006',
		patientDisplay: 'Miller, Sarah F',
		service: 'SURGICAL',
		specimenType: 'Excision',
		specimenSite: 'Skin - Back',
		status: 'GROSSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 1),
		collectedAt: dateTimeAgo(0, 3),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// Just accessioned
	items.push({
		id: 7,
		accessionNumber: 'S24-00006',
		patientMrn: 'MRN-100007',
		patientDisplay: 'Wilson, James G',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Stomach - Antrum',
		status: 'ACCESSIONED',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 0),
		collectedAt: dateTimeAgo(0, 2),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// Processing
	items.push({
		id: 8,
		accessionNumber: 'S24-00007',
		patientMrn: 'MRN-100008',
		patientDisplay: 'Moore, Patricia H',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Liver',
		status: 'PROCESSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'URGENT',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 2),
		collectedAt: dateTimeAgo(1, 4),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// Hematology case
	items.push({
		id: 9,
		accessionNumber: 'H24-00001',
		patientMrn: 'MRN-100009',
		patientDisplay: 'Taylor, David I',
		service: 'HEMATOLOGY',
		specimenType: 'Bone Marrow Biopsy',
		specimenSite: 'Iliac Crest',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'URGENT',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 8,
		slidePending: 0,
		slideScanned: 8,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 4),
		collectedAt: dateTimeAgo(2, 5),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// Signed out case (should not show in default view)
	items.push({
		id: 10,
		accessionNumber: 'S24-00008',
		patientMrn: 'MRN-100010',
		patientDisplay: 'Anderson, Linda J',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Colon - Sigmoid',
		status: 'SIGNED_OUT',
		lisStatus: 'FINAL',
		wsiStatus: 'SCANNED',
		authoringStatus: 'SIGNED',
		priority: 'ROUTINE',
		assignedToId: 1,
		assignedToDisplay: 'Dr. Rum Tug Tugger',
		slideCount: 3,
		slidePending: 0,
		slideScanned: 3,
		caseDate: daysAgo(7),
		receivedAt: dateTimeAgo(7, 3),
		collectedAt: dateTimeAgo(7, 5),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// ============================================
	// DR. SKIMBLESHANKS' CASES (id: 2) - 8 cases
	// ============================================

	items.push({
		id: 11,
		accessionNumber: 'S24-00009',
		patientMrn: 'MRN-100011',
		patientDisplay: 'Thompson, Richard K',
		service: 'SURGICAL',
		specimenType: 'Excision',
		specimenSite: 'Skin - Face',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'DRAFT',
		priority: 'STAT',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 5,
		slidePending: 0,
		slideScanned: 5,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 3),
		collectedAt: dateTimeAgo(0, 5),
		annotations: [],
		alerts: [{ type: 'STAT', message: 'Mohs - patient in procedure room' }],
		metadata: {}
	});

	items.push({
		id: 12,
		accessionNumber: 'S24-00010',
		patientMrn: 'MRN-100012',
		patientDisplay: 'Garcia, Maria L',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Cervix',
		status: 'SLIDES_CUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 4,
		slidePending: 2,
		slideScanned: 2,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 6),
		collectedAt: dateTimeAgo(1, 8),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 13,
		accessionNumber: 'C24-00002',
		patientMrn: 'MRN-100013',
		patientDisplay: 'Martinez, Carlos R',
		service: 'CYTOLOGY',
		specimenType: 'Pap Smear',
		specimenSite: 'Cervix',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 2,
		slidePending: 0,
		slideScanned: 2,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 4),
		collectedAt: dateTimeAgo(2, 6),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 14,
		accessionNumber: 'S24-00011',
		patientMrn: 'MRN-100014',
		patientDisplay: 'Robinson, Angela M',
		service: 'SURGICAL',
		specimenType: 'Resection',
		specimenSite: 'Colon - Sigmoid',
		status: 'UNDER_REVIEW',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'PENDING_REVIEW',
		priority: 'ROUTINE',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 24,
		slidePending: 0,
		slideScanned: 24,
		caseDate: daysAgo(3),
		receivedAt: dateTimeAgo(3, 2),
		collectedAt: dateTimeAgo(3, 4),
		annotations: [{ type: 'NOTE', message: '15 lymph nodes identified' }],
		alerts: [],
		metadata: { draftBy: 'Dr. Mungojerrie', draftDate: daysAgo(1) }
	});

	items.push({
		id: 15,
		accessionNumber: 'S24-00012',
		patientMrn: 'MRN-100015',
		patientDisplay: 'Clark, Steven W',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Kidney',
		status: 'GROSSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'URGENT',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 4),
		collectedAt: dateTimeAgo(0, 6),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 16,
		accessionNumber: 'H24-00002',
		patientMrn: 'MRN-100016',
		patientDisplay: 'Lewis, Jennifer A',
		service: 'HEMATOLOGY',
		specimenType: 'Peripheral Blood',
		specimenSite: 'Blood',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'STAT',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 2,
		slidePending: 0,
		slideScanned: 2,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 1),
		collectedAt: dateTimeAgo(0, 2),
		annotations: [],
		alerts: [{ type: 'STAT', message: 'Suspected leukemia - oncologist waiting' }],
		metadata: {}
	});

	items.push({
		id: 17,
		accessionNumber: 'S24-00013',
		patientMrn: 'MRN-100017',
		patientDisplay: 'Walker, Barbara J',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Bladder',
		status: 'PROCESSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 3),
		collectedAt: dateTimeAgo(1, 5),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 18,
		accessionNumber: 'S24-00014',
		patientMrn: 'MRN-100018',
		patientDisplay: 'Hall, Thomas E',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Esophagus',
		status: 'ACCESSIONED',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 2,
		assignedToDisplay: 'Dr. Skimbleshanks',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 0),
		collectedAt: dateTimeAgo(0, 1),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// ============================================
	// DR. OLD DEUTERONOMY'S CASES (id: 3) - 9 cases
	// ============================================

	items.push({
		id: 19,
		accessionNumber: 'S24-00015',
		patientMrn: 'MRN-100019',
		patientDisplay: 'Allen, Dorothy P',
		service: 'SURGICAL',
		specimenType: 'Excision',
		specimenSite: 'Breast - Right',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'DRAFT',
		priority: 'URGENT',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 15,
		slidePending: 0,
		slideScanned: 15,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 5),
		collectedAt: dateTimeAgo(1, 7),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 20,
		accessionNumber: 'S24-00016',
		patientMrn: 'MRN-100020',
		patientDisplay: 'Young, Kevin R',
		service: 'SURGICAL',
		specimenType: 'Resection',
		specimenSite: 'Pancreas',
		status: 'SLIDES_CUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 30,
		slidePending: 20,
		slideScanned: 10,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 3),
		collectedAt: dateTimeAgo(2, 5),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 21,
		accessionNumber: 'A24-00001',
		patientMrn: 'MRN-100021',
		patientDisplay: 'Hernandez, Jose M',
		service: 'AUTOPSY',
		specimenType: 'Autopsy',
		specimenSite: 'Complete',
		status: 'GROSSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 2),
		collectedAt: dateTimeAgo(1, 3),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 22,
		accessionNumber: 'S24-00017',
		patientMrn: 'MRN-100022',
		patientDisplay: 'King, Nancy L',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Small Bowel',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 6,
		slidePending: 0,
		slideScanned: 6,
		caseDate: daysAgo(3),
		receivedAt: dateTimeAgo(3, 4),
		collectedAt: dateTimeAgo(3, 6),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 23,
		accessionNumber: 'C24-00003',
		patientMrn: 'MRN-100023',
		patientDisplay: 'Wright, Sandra K',
		service: 'CYTOLOGY',
		specimenType: 'FNA',
		specimenSite: 'Lymph Node - Cervical',
		status: 'UNDER_REVIEW',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'PENDING_REVIEW',
		priority: 'URGENT',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 4,
		slidePending: 0,
		slideScanned: 4,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 3),
		collectedAt: dateTimeAgo(1, 4),
		annotations: [],
		alerts: [],
		metadata: { draftBy: 'Dr. Rum Tug Tugger', draftDate: daysAgo(0) }
	});

	items.push({
		id: 24,
		accessionNumber: 'S24-00018',
		patientMrn: 'MRN-100024',
		patientDisplay: 'Lopez, Daniel F',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Rectum',
		status: 'PROCESSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 5),
		collectedAt: dateTimeAgo(1, 7),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 25,
		accessionNumber: 'H24-00003',
		patientMrn: 'MRN-100025',
		patientDisplay: 'Hill, Rebecca S',
		service: 'HEMATOLOGY',
		specimenType: 'Bone Marrow Biopsy',
		specimenSite: 'Iliac Crest',
		status: 'SLIDES_CUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'URGENT',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 10,
		slidePending: 6,
		slideScanned: 4,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 2),
		collectedAt: dateTimeAgo(2, 3),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 26,
		accessionNumber: 'S24-00019',
		patientMrn: 'MRN-100026',
		patientDisplay: 'Scott, Michelle T',
		service: 'SURGICAL',
		specimenType: 'Excision',
		specimenSite: 'Thyroid - Total',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'DRAFT',
		priority: 'ROUTINE',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 8,
		slidePending: 0,
		slideScanned: 8,
		caseDate: daysAgo(4),
		receivedAt: dateTimeAgo(4, 3),
		collectedAt: dateTimeAgo(4, 5),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 27,
		accessionNumber: 'A24-00002',
		patientMrn: 'MRN-100027',
		patientDisplay: 'Green, Paul W',
		service: 'AUTOPSY',
		specimenType: 'Autopsy',
		specimenSite: 'Limited - Brain',
		status: 'ACCESSIONED',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 3,
		assignedToDisplay: 'Dr. Old Deuteronomy',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 2),
		collectedAt: dateTimeAgo(0, 3),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	// ============================================
	// DR. MUNGOJERRIE'S CASES (id: 4) - 8 cases
	// ============================================

	items.push({
		id: 28,
		accessionNumber: 'S24-00020',
		patientMrn: 'MRN-100028',
		patientDisplay: 'Adams, Christine E',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Endometrium',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 4,
		slidePending: 0,
		slideScanned: 4,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 4),
		collectedAt: dateTimeAgo(2, 6),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 29,
		accessionNumber: 'S24-00021',
		patientMrn: 'MRN-100029',
		patientDisplay: 'Baker, William H',
		service: 'SURGICAL',
		specimenType: 'Resection',
		specimenSite: 'Stomach',
		status: 'SLIDES_CUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'URGENT',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 20,
		slidePending: 12,
		slideScanned: 8,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 2),
		collectedAt: dateTimeAgo(1, 4),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 30,
		accessionNumber: 'C24-00004',
		patientMrn: 'MRN-100030',
		patientDisplay: 'Nelson, Laura M',
		service: 'CYTOLOGY',
		specimenType: 'Pap Smear',
		specimenSite: 'Cervix',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 2,
		slidePending: 0,
		slideScanned: 2,
		caseDate: daysAgo(3),
		receivedAt: dateTimeAgo(3, 5),
		collectedAt: dateTimeAgo(3, 7),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 31,
		accessionNumber: 'S24-00022',
		patientMrn: 'MRN-100031',
		patientDisplay: 'Carter, Raymond G',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Lung - Transbronchial',
		status: 'GROSSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'STAT',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(0),
		receivedAt: dateTimeAgo(0, 2),
		collectedAt: dateTimeAgo(0, 3),
		annotations: [],
		alerts: [{ type: 'STAT', message: 'Suspected malignancy - oncology consult pending' }],
		metadata: {}
	});

	items.push({
		id: 32,
		accessionNumber: 'H24-00004',
		patientMrn: 'MRN-100032',
		patientDisplay: 'Mitchell, Susan D',
		service: 'HEMATOLOGY',
		specimenType: 'Peripheral Blood',
		specimenSite: 'Blood',
		status: 'PENDING_SIGNOUT',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 2,
		slidePending: 0,
		slideScanned: 2,
		caseDate: daysAgo(1),
		receivedAt: dateTimeAgo(1, 3),
		collectedAt: dateTimeAgo(1, 4),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 33,
		accessionNumber: 'S24-00023',
		patientMrn: 'MRN-100033',
		patientDisplay: 'Perez, Anthony J',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Bone',
		status: 'PROCESSING',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'PENDING',
		authoringStatus: null,
		priority: 'ROUTINE',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 0,
		slidePending: 0,
		slideScanned: 0,
		caseDate: daysAgo(2),
		receivedAt: dateTimeAgo(2, 4),
		collectedAt: dateTimeAgo(2, 6),
		annotations: [],
		alerts: [],
		metadata: {}
	});

	items.push({
		id: 34,
		accessionNumber: 'S24-00024',
		patientMrn: 'MRN-100034',
		patientDisplay: 'Roberts, Emily K',
		service: 'SURGICAL',
		specimenType: 'Excision',
		specimenSite: 'Soft Tissue - Thigh',
		status: 'UNDER_REVIEW',
		lisStatus: 'IN_PROGRESS',
		wsiStatus: 'SCANNED',
		authoringStatus: 'PENDING_REVIEW',
		priority: 'ROUTINE',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 10,
		slidePending: 0,
		slideScanned: 10,
		caseDate: daysAgo(4),
		receivedAt: dateTimeAgo(4, 2),
		collectedAt: dateTimeAgo(4, 4),
		annotations: [],
		alerts: [],
		metadata: { draftBy: 'Dr. Old Deuteronomy', draftDate: daysAgo(2) }
	});

	items.push({
		id: 35,
		accessionNumber: 'S24-00025',
		patientMrn: 'MRN-100035',
		patientDisplay: 'Turner, Brian L',
		service: 'SURGICAL',
		specimenType: 'Biopsy',
		specimenSite: 'Appendix',
		status: 'AMENDED',
		lisStatus: 'AMENDED',
		wsiStatus: 'SCANNED',
		authoringStatus: 'AMENDMENT_PENDING',
		priority: 'ROUTINE',
		assignedToId: 4,
		assignedToDisplay: 'Dr. Mungojerrie',
		slideCount: 3,
		slidePending: 0,
		slideScanned: 3,
		caseDate: daysAgo(10),
		receivedAt: dateTimeAgo(10, 3),
		collectedAt: dateTimeAgo(10, 5),
		annotations: [],
		alerts: [{ type: 'AMENDMENT', message: 'Addendum requested by clinician' }],
		metadata: { amendedBy: 'Dr. Mungojerrie' }
	});

	return items;
}

export const worklistStore = new WorklistStore();
