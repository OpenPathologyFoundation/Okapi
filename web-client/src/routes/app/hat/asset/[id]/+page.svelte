<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import type { AssetCurrentState, AssetEvent } from '$lib/types/hat';
	import { hatStore } from '$lib/stores/hat.svelte';
	import AssetDetail from '$lib/components/hat/AssetDetail.svelte';
	import EventTimeline from '$lib/components/hat/EventTimeline.svelte';

	let asset = $state<AssetCurrentState | null>(null);
	let events = $state<AssetEvent[]>([]);
	let isLoading = $state(true);
	let error = $state<string | null>(null);
	let activeTab = $state<'details' | 'history'>('details');

	const assetId = $derived(page.params.id ?? '');

	onMount(async () => {
		await loadAsset();
	});

	async function loadAsset() {
		if (!assetId) {
			error = 'No asset ID provided';
			isLoading = false;
			return;
		}

		isLoading = true;
		error = null;

		try {
			// Try to get from cache first
			const cached = hatStore.getAsset(assetId);
			if (cached) {
				asset = cached;
				events = generateMockEvents(cached);
			} else {
				// Generate mock data for demo
				asset = {
					id: assetId,
					identifiers: [
						{ type: 'BARCODE_NORMALIZED', value: assetId.toUpperCase(), source: 'OKAPI', createdAt: new Date().toISOString() },
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
				if (asset) {
					events = generateMockEvents(asset);
				}
			}
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to load asset';
		} finally {
			isLoading = false;
		}
	}

	function generateMockEvents(asset: AssetCurrentState): AssetEvent[] {
		const now = new Date();
		return [
			{
				id: 'evt-1',
				assetId: asset.id,
				type: 'CREATED',
				timestamp: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString(),
				actor: { id: 'user-1', display: 'Dr. Smith', role: 'Pathologist' },
				payload: {},
				comment: 'Initial registration from CoPath'
			},
			{
				id: 'evt-2',
				assetId: asset.id,
				type: 'LOCATION_CHANGED',
				timestamp: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000).toISOString(),
				actor: { id: 'user-2', display: 'Tech Johnson', role: 'Histotech' },
				payload: { location: 'Archive Room A, Shelf B-12' },
				comment: 'Filed after QA'
			},
			{
				id: 'evt-3',
				assetId: asset.id,
				type: 'CUSTODY_CHANGED',
				timestamp: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString(),
				actor: { id: 'user-3', display: 'Dr. Martinez', role: 'Pathologist' },
				payload: { to: 'Dr. Martinez' },
				comment: 'Pulled for second opinion'
			},
			{
				id: 'evt-4',
				assetId: asset.id,
				type: 'CUSTODY_CHANGED',
				timestamp: new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000).toISOString(),
				actor: { id: 'user-3', display: 'Dr. Martinez', role: 'Pathologist' },
				payload: { from: 'Dr. Martinez' },
				comment: 'Returned to archive'
			},
			{
				id: 'evt-5',
				assetId: asset.id,
				type: 'STATUS_CHANGED',
				timestamp: new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000).toISOString(),
				actor: { id: 'user-2', display: 'Tech Johnson', role: 'Histotech' },
				payload: { from: 'CHECKED_OUT', to: 'AVAILABLE' },
				comment: undefined
			}
		];
	}

	function handleCreateRequest() {
		if (asset) {
			goto(`/app/hat/requests/new?asset=${asset.id}`);
		}
	}

	function handleUpdateStatus() {
		console.log('Update status');
		// TODO: Implement status update modal
	}

	function handleTransferCustody() {
		console.log('Transfer custody');
		// TODO: Implement custody transfer modal
	}

	function handleEventClick(event: AssetEvent) {
		console.log('Event clicked:', event);
		// TODO: Implement event detail modal
	}
</script>

<div class="h-full p-6 overflow-auto">
	<div class="max-w-5xl mx-auto">
		<!-- Back navigation -->
		<div class="mb-6">
			<a
				href="/app/hat"
				class="inline-flex items-center gap-2 text-sm text-clinical-muted hover:text-clinical-text transition-colors"
			>
				<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
				</svg>
				Back to Lookup
			</a>
		</div>

		{#if isLoading}
			<div class="flex items-center justify-center py-12">
				<svg class="animate-spin h-8 w-8 text-clinical-primary" fill="none" viewBox="0 0 24 24">
					<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
					<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
				</svg>
			</div>
		{:else if error}
			<div class="bg-hat-status-missing/10 border border-hat-status-missing/30 rounded-xl p-6 text-center">
				<p class="text-hat-status-missing">{error}</p>
				<button
					onclick={loadAsset}
					class="mt-4 px-4 py-2 text-sm font-medium text-clinical-text bg-clinical-hover hover:bg-clinical-border/50 border border-clinical-border rounded-lg transition-colors"
				>
					Retry
				</button>
			</div>
		{:else if asset}
			<!-- Tabs -->
			<div class="flex gap-4 mb-6 border-b border-clinical-border">
				<button
					onclick={() => (activeTab = 'details')}
					class="px-4 py-2 text-sm font-medium border-b-2 transition-colors {activeTab === 'details'
						? 'border-clinical-primary text-clinical-primary'
						: 'border-transparent text-clinical-muted hover:text-clinical-text'}"
				>
					Current State
				</button>
				<button
					onclick={() => (activeTab = 'history')}
					class="px-4 py-2 text-sm font-medium border-b-2 transition-colors {activeTab === 'history'
						? 'border-clinical-primary text-clinical-primary'
						: 'border-transparent text-clinical-muted hover:text-clinical-text'}"
				>
					Event History
					<span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-full bg-clinical-border/50">
						{events.length}
					</span>
				</button>
			</div>

			<!-- Tab content -->
			{#if activeTab === 'details'}
				<AssetDetail
					{asset}
					onCreateRequest={handleCreateRequest}
					onUpdateStatus={handleUpdateStatus}
					onTransferCustody={handleTransferCustody}
				/>
			{:else}
				<div class="bg-clinical-surface border border-clinical-border rounded-xl p-6">
					<EventTimeline {events} onEventClick={handleEventClick} />
				</div>
			{/if}
		{/if}
	</div>
</div>
