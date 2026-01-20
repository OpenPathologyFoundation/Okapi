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
import { ACTIVE_STATUSES, isCompletedStatus } from '$lib/types/worklist';

class WorklistStore {
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
				this.filters = { ...this.filters, ...parsed };
			} catch {
				// Ignore invalid stored data
			}
		}
	}

	private saveFiltersToStorage() {
		if (browser) {
			localStorage.setItem('worklist_filters', JSON.stringify(this.filters));
		}
	}

	// Derived state: filtered items (client-side filtering for mock mode)
	get filteredItems(): WorklistItem[] {
		let result = [...this.items];

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
	}

	// Derived state: sorted items
	get sortedItems(): WorklistItem[] {
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
	}

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
		this.saveFiltersToStorage();
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
		this.items = generateMockWorklist();
		this.totalItems = this.items.length;
		this.totalPages = 1;
		this.counts = {
			total: this.items.length,
			myCases: this.items.filter((i) => i.assignedToId === 1).length,
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

// Mock data generator
function generateMockWorklist(): WorklistItem[] {
	const services: PathologyService[] = ['SURGICAL', 'CYTOLOGY', 'HEMATOLOGY', 'AUTOPSY'];
	const statuses: CaseStatus[] = [
		'ACCESSIONED',
		'GROSSING',
		'PROCESSING',
		'SLIDES_CUT',
		'PENDING_SIGNOUT',
		'UNDER_REVIEW',
		'SIGNED_OUT'
	];
	const priorities: CasePriority[] = ['STAT', 'URGENT', 'ROUTINE'];
	const pathologists = [
		{ id: 1, display: 'Dr. Rum Tug Tugger' },
		{ id: 2, display: 'Dr. Skimbleshanks' },
		{ id: 3, display: 'Dr. Old Deuteronomy' },
		{ id: 4, display: 'Dr. Mungojerrie' }
	];
	const specimens = [
		{ type: 'Biopsy', site: 'Colon' },
		{ type: 'Excision', site: 'Breast' },
		{ type: 'Resection', site: 'Lung' },
		{ type: 'FNA', site: 'Thyroid' },
		{ type: 'Biopsy', site: 'Prostate' },
		{ type: 'Pap Smear', site: 'Cervix' },
		{ type: 'Peripheral Blood', site: 'Blood' },
		{ type: 'Bone Marrow Biopsy', site: 'Iliac Crest' }
	];
	const patients = [
		'Smith, John A',
		'Johnson, Mary B',
		'Williams, Robert C',
		'Brown, Elizabeth D',
		'Davis, Michael E',
		'Miller, Sarah F',
		'Wilson, James G',
		'Moore, Patricia H',
		'Taylor, David I',
		'Anderson, Linda J'
	];

	const items: WorklistItem[] = [];

	for (let i = 0; i < 35; i++) {
		const service = services[Math.floor(Math.random() * services.length)];
		const prefix = service === 'SURGICAL' ? 'S' : service === 'CYTOLOGY' ? 'C' : service === 'HEMATOLOGY' ? 'H' : 'A';
		const status = statuses[Math.floor(Math.random() * statuses.length)];
		const priority = priorities[Math.floor(Math.random() * priorities.length)];
		const pathologist = pathologists[Math.floor(Math.random() * pathologists.length)];
		const specimen = specimens[Math.floor(Math.random() * specimens.length)];
		const patient = patients[Math.floor(Math.random() * patients.length)];

		const slideTotal = Math.floor(Math.random() * 15) + 1;
		const slideScanned =
			status === 'SIGNED_OUT' || status === 'PENDING_SIGNOUT' || status === 'UNDER_REVIEW'
				? slideTotal
				: Math.floor(Math.random() * slideTotal);
		const slidePending = slideTotal - slideScanned;

		const caseDate = new Date();
		caseDate.setDate(caseDate.getDate() - Math.floor(Math.random() * 7));

		items.push({
			id: i + 1,
			accessionNumber: `${prefix}24-${String(i + 1).padStart(5, '0')}`,
			patientMrn: `MRN-${100000 + i}`,
			patientDisplay: patient,
			service,
			specimenType: specimen.type,
			specimenSite: specimen.site,
			status,
			lisStatus: status === 'SIGNED_OUT' ? 'FINAL' : 'IN_PROGRESS',
			wsiStatus: slideScanned === slideTotal ? 'SCANNED' : 'PENDING',
			authoringStatus: status === 'PENDING_SIGNOUT' ? 'DRAFT' : null,
			priority,
			assignedToId: pathologist.id,
			assignedToDisplay: pathologist.display,
			slideCount: slideTotal,
			slidePending,
			slideScanned,
			caseDate: caseDate.toISOString().split('T')[0],
			receivedAt: caseDate.toISOString(),
			collectedAt: new Date(caseDate.getTime() - 24 * 60 * 60 * 1000).toISOString(),
			annotations: [],
			alerts: priority === 'STAT' ? [{ type: 'STAT', message: 'Urgent case' }] : [],
			metadata: {}
		});
	}

	return items;
}

export const worklistStore = new WorklistStore();
