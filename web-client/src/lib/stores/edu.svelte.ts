import { browser } from '$app/environment';
import type {
	EduCaseListItem,
	EduCaseDetail,
	EduNamedCollection,
	EduNamedCollectionDetail,
	EduSearchFilters,
	EduFacets,
	EduPageResponse
} from '$lib/types/edu';

class EduStore {
	// Data state
	cases = $state<EduCaseListItem[]>([]);
	collections = $state<EduNamedCollection[]>([]);
	selectedCase = $state<EduCaseDetail | null>(null);
	selectedCollection = $state<EduNamedCollectionDetail | null>(null);
	facets = $state<EduFacets | null>(null);

	// Pagination
	page = $state(0);
	pageSize = $state(50);
	totalItems = $state(0);
	totalPages = $state(0);

	// Filters
	filters = $state<EduSearchFilters>({
		query: '',
		anatomicSite: '',
		specimenType: '',
		difficulty: '',
		stain: '',
		curatorId: '',
		collectionId: ''
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
		const stored = localStorage.getItem('edu_filters');
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
			localStorage.setItem('edu_filters', JSON.stringify(this.filters));
		}
	}

	// Data loading
	async loadCases(): Promise<void> {
		this.isLoading = true;
		this.error = null;

		try {
			const params = new URLSearchParams();

			if (this.filters.query) params.set('query', this.filters.query);
			if (this.filters.anatomicSite) params.set('anatomicSite', this.filters.anatomicSite);
			if (this.filters.specimenType) params.set('specimenType', this.filters.specimenType);
			if (this.filters.difficulty) params.set('difficulty', this.filters.difficulty);
			if (this.filters.stain) params.set('stain', this.filters.stain);
			if (this.filters.curatorId) params.set('curatorId', this.filters.curatorId);
			if (this.filters.collectionId) params.set('collectionId', this.filters.collectionId);

			params.set('page', String(this.page));
			params.set('pageSize', String(this.pageSize));

			const response = await fetch(`/api/edu/cases?${params.toString()}`);
			if (!response.ok) {
				throw new Error(`Failed to load educational cases: ${response.statusText}`);
			}

			const data: EduPageResponse = await response.json();
			this.cases = data.items;
			this.page = data.page;
			this.totalItems = data.totalItems;
			this.totalPages = data.totalPages;
			this.facets = data.facets;
		} catch (e) {
			const message = e instanceof Error ? e.message : 'Unknown error';
			this.error = message;
		} finally {
			this.isLoading = false;
		}
	}

	async loadCaseDetail(accession: string): Promise<void> {
		this.isLoading = true;
		this.error = null;

		try {
			const response = await fetch(`/api/edu/cases/${encodeURIComponent(accession)}`);
			if (!response.ok) {
				throw new Error(`Failed to load case detail: ${response.statusText}`);
			}

			this.selectedCase = await response.json();
		} catch (e) {
			const message = e instanceof Error ? e.message : 'Unknown error';
			this.error = message;
		} finally {
			this.isLoading = false;
		}
	}

	async loadCollections(): Promise<void> {
		this.isLoading = true;
		this.error = null;

		try {
			const response = await fetch('/api/edu/collections');
			if (!response.ok) {
				throw new Error(`Failed to load collections: ${response.statusText}`);
			}

			this.collections = await response.json();
		} catch (e) {
			const message = e instanceof Error ? e.message : 'Unknown error';
			this.error = message;
		} finally {
			this.isLoading = false;
		}
	}

	async loadCollectionDetail(id: string): Promise<void> {
		this.isLoading = true;
		this.error = null;

		try {
			const response = await fetch(`/api/edu/collections/${encodeURIComponent(id)}`);
			if (!response.ok) {
				throw new Error(`Failed to load collection detail: ${response.statusText}`);
			}

			this.selectedCollection = await response.json();
		} catch (e) {
			const message = e instanceof Error ? e.message : 'Unknown error';
			this.error = message;
		} finally {
			this.isLoading = false;
		}
	}

	async loadFacets(): Promise<void> {
		try {
			const response = await fetch('/api/edu/facets');
			if (response.ok) {
				this.facets = await response.json();
			}
		} catch {
			// Non-critical, silently fail
		}
	}

	// Filter mutations
	setFilter(key: keyof EduSearchFilters, value: string) {
		this.filters = { ...this.filters, [key]: value };
		this.page = 0;
		this.saveFiltersToStorage();
	}

	clearFilters() {
		this.filters = {
			query: '',
			anatomicSite: '',
			specimenType: '',
			difficulty: '',
			stain: '',
			curatorId: '',
			collectionId: ''
		};
		this.page = 0;
		this.saveFiltersToStorage();
	}

	get hasActiveFilters(): boolean {
		return Object.values(this.filters).some((v) => v !== '');
	}

	// Clear state
	clear() {
		this.cases = [];
		this.selectedCase = null;
		this.selectedCollection = null;
		this.facets = null;
		this.error = null;
	}
}

export const eduStore = new EduStore();
