import { browser } from '$app/environment';
import { authStore } from '$lib/stores/auth.svelte';
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

const AUTO_REFRESH_INTERVAL = 3 * 60 * 1000; // 3 minutes
const TICK_INTERVAL = 10 * 1000; // 10 seconds for timestamp display updates

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
	lastRefreshTime = $state<Date | null>(null);
	private _tick = $state(0); // bumped periodically to force timestamp re-render
	private autoRefreshTimer: ReturnType<typeof setInterval> | null = null;
	private tickTimer: ReturnType<typeof setInterval> | null = null;

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

	// Derived state: filtered items (client-side filtering)
	filteredItems = $derived.by(() => {
		let result = [...this.items];

		// My Cases filter — match by logged-in user's identity ID
		if (this.filters.myCasesOnly && authStore.user?.identityId) {
			const myId = authStore.user.identityId;
			result = result.filter((item) => item.assignedToIdentityId === myId);
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

	// Get formatted last refresh time (reads _tick so Svelte re-evaluates)
	getLastRefreshDisplay(): string {
		void this._tick; // reactive dependency
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

	// Start auto-refresh (3 min) and timestamp tick (10s)
	startAutoRefresh() {
		this.stopAutoRefresh();
		this.tickTimer = setInterval(() => {
			this._tick++;
		}, TICK_INTERVAL);
		this.autoRefreshTimer = setInterval(() => {
			this.loadWorklist();
		}, AUTO_REFRESH_INTERVAL);
	}

	// Stop timers
	stopAutoRefresh() {
		if (this.autoRefreshTimer) {
			clearInterval(this.autoRefreshTimer);
			this.autoRefreshTimer = null;
		}
		if (this.tickTimer) {
			clearInterval(this.tickTimer);
			this.tickTimer = null;
		}
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

export const worklistStore = new WorklistStore();
