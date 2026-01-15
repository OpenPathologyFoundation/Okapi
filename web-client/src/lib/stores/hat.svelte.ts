import { browser } from '$app/environment';
import type {
	AssetCurrentState,
	WorkRequest,
	LookupResult,
	HatUser,
	HatSearchFilters,
	HatViewMode,
	HatPermission
} from '$lib/types/hat';

class HatStore {
	// Current user context
	user = $state<HatUser | null>(null);

	// Lookup state
	lastLookup = $state<LookupResult | null>(null);
	recentLookups = $state<LookupResult[]>([]);

	// Asset cache (keyed by ID)
	private _assetCache = $state<Record<string, AssetCurrentState>>({});

	// Request state
	activeRequests = $state<WorkRequest[]>([]);
	currentRequest = $state<WorkRequest | null>(null);

	// Queue state
	queueItems = $state<WorkRequest[]>([]);
	queueFilters = $state<HatSearchFilters>({});

	// UI state
	isLoading = $state(false);
	error = $state<string | null>(null);
	viewMode = $state<HatViewMode>('scan');

	constructor() {
		if (browser) {
			this.loadFromStorage();
		}
	}

	private loadFromStorage() {
		const stored = localStorage.getItem('hat_recent_lookups');
		if (stored) {
			try {
				this.recentLookups = JSON.parse(stored);
			} catch {
				// Ignore invalid stored data
			}
		}
	}

	private saveToStorage() {
		if (browser) {
			localStorage.setItem('hat_recent_lookups', JSON.stringify(this.recentLookups.slice(0, 10)));
		}
	}

	// Barcode normalization - SYS-HAT-001
	normalizeBarcode(raw: string): string {
		return raw
			.toUpperCase()
			.replace(/[\s\-]/g, '')
			.replace(/^0+/, '');
	}

	// Lookup action - SYS-HAT-001, SYS-HAT-002
	async lookup(query: string): Promise<LookupResult> {
		this.isLoading = true;
		this.error = null;

		const normalized = this.normalizeBarcode(query);

		try {
			const response = await fetch(`/api/hat/lookup?q=${encodeURIComponent(query)}`);
			if (!response.ok) {
				throw new Error(`Lookup failed: ${response.statusText}`);
			}

			const result: LookupResult = await response.json();
			this.lastLookup = result;
			this.addToRecent(result);

			if (result.match) {
				this.cacheAsset(result.match);
			}
			if (result.candidates) {
				result.candidates.forEach((c) => this.cacheAsset(c));
			}

			return result;
		} catch (e) {
			const message = e instanceof Error ? e.message : 'Unknown error';
			this.error = message;
			throw e;
		} finally {
			this.isLoading = false;
		}
	}

	// Mock lookup for UI development (no backend required)
	mockLookup(query: string): LookupResult {
		const normalized = this.normalizeBarcode(query);

		// Simulate different outcomes based on query
		if (normalized.startsWith('NOTFOUND')) {
			return {
				outcome: 'NOT_FOUND',
				query: { raw: query, normalized },
				message: 'No asset found matching this identifier'
			};
		}

		if (normalized.startsWith('AMBIG')) {
			return {
				outcome: 'AMBIGUOUS',
				query: { raw: query, normalized },
				candidates: [
					this.createMockAsset('ASSET-001', normalized + '-A'),
					this.createMockAsset('ASSET-002', normalized + '-B')
				],
				message: 'Multiple assets match this identifier'
			};
		}

		// Default: matched
		const result: LookupResult = {
			outcome: 'MATCHED',
			query: { raw: query, normalized },
			match: this.createMockAsset('ASSET-' + Date.now(), normalized)
		};

		this.lastLookup = result;
		this.addToRecent(result);
		if (result.match) {
			this.cacheAsset(result.match);
		}

		return result;
	}

	private createMockAsset(id: string, barcode: string): AssetCurrentState {
		return {
			id,
			identifiers: [
				{ type: 'BARCODE_NORMALIZED', value: barcode, source: 'OKAPI', createdAt: new Date().toISOString() },
				{ type: 'LIS_ACCESSION', value: 'S24-' + Math.floor(Math.random() * 10000), source: 'COPATH', createdAt: new Date().toISOString() }
			],
			status: 'AVAILABLE',
			location: {
				facility: 'Main Campus',
				building: 'Pathology Building',
				room: 'Archive Room A',
				storage: 'Shelf B-12',
				assertedAt: new Date().toISOString(),
				assertedBy: 'system'
			},
			provenance: {
				source: 'OKAPI',
				assertedBy: 'system',
				assertedAt: new Date().toISOString()
			},
			lifecycleFlags: {
				isPlaceholder: false,
				hasConflict: false,
				isHighRisk: false
			},
			updatedAt: new Date().toISOString()
		};
	}

	private addToRecent(result: LookupResult) {
		// Avoid duplicates
		const existing = this.recentLookups.findIndex((r) => r.query.normalized === result.query.normalized);
		if (existing >= 0) {
			this.recentLookups.splice(existing, 1);
		}
		this.recentLookups = [result, ...this.recentLookups.slice(0, 9)];
		this.saveToStorage();
	}

	private cacheAsset(asset: AssetCurrentState) {
		this._assetCache[asset.id] = asset;
	}

	getAsset(id: string): AssetCurrentState | undefined {
		return this._assetCache[id];
	}

	async loadAsset(id: string): Promise<AssetCurrentState> {
		const cached = this._assetCache[id];
		if (cached) {
			return cached;
		}

		const response = await fetch(`/api/hat/assets/${id}`);
		if (!response.ok) {
			throw new Error(`Failed to load asset: ${response.statusText}`);
		}

		const asset: AssetCurrentState = await response.json();
		this.cacheAsset(asset);
		return asset;
	}

	async loadQueue(): Promise<void> {
		this.isLoading = true;
		try {
			const response = await fetch('/api/hat/queue');
			if (!response.ok) {
				throw new Error(`Failed to load queue: ${response.statusText}`);
			}
			this.queueItems = await response.json();
		} finally {
			this.isLoading = false;
		}
	}

	async loadRequests(): Promise<void> {
		this.isLoading = true;
		try {
			const response = await fetch('/api/hat/requests');
			if (!response.ok) {
				throw new Error(`Failed to load requests: ${response.statusText}`);
			}
			this.activeRequests = await response.json();
		} finally {
			this.isLoading = false;
		}
	}

	// Permission helpers - SYS-HAT-013
	can(permission: HatPermission): boolean {
		return this.user?.permissions.includes(permission) ?? false;
	}

	canExecuteHighRisk(): boolean {
		return this.can('hat:distribute:external') || this.can('hat:distribute:research');
	}

	canCreateRequests(): boolean {
		return this.can('hat:request:create');
	}

	canExecuteRequests(): boolean {
		return this.can('hat:request:execute');
	}

	// Clear state
	clearLookup() {
		this.lastLookup = null;
		this.error = null;
	}

	clearRecentLookups() {
		this.recentLookups = [];
		if (browser) {
			localStorage.removeItem('hat_recent_lookups');
		}
	}

	// Initialize store
	init() {
		if (browser) {
			this.loadFromStorage();
		}
	}
}

export const hatStore = new HatStore();
