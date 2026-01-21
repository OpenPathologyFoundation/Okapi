<script lang="ts">
	import type { LookupResult, AssetCurrentState } from '$lib/types/hat';
	import { goto } from '$app/navigation';
	import { hatStore } from '$lib/stores/hat.svelte';
	import ScanInput from '$lib/components/hat/ScanInput.svelte';
	import LookupResultCard from '$lib/components/hat/LookupResultCard.svelte';
	import StatusBadge from '$lib/components/hat/StatusBadge.svelte';

	let currentResult = $state<LookupResult | null>(null);

	function handleLookupResult(result: LookupResult) {
		currentResult = result;
	}

	function handleViewAsset(asset: AssetCurrentState) {
		goto(`/app/hat/asset/${asset.id}`);
	}

	function handleCreateRequest(asset: AssetCurrentState) {
		goto(`/app/hat/requests/new?asset=${asset.id}`);
	}

	function handleCreatePlaceholder(query: string) {
		console.log('Create placeholder for:', query);
		// TODO: Implement placeholder creation
	}

	function handleSelectCandidate(asset: AssetCurrentState) {
		currentResult = {
			outcome: 'MATCHED',
			query: currentResult?.query ?? { raw: '', normalized: '' },
			match: asset
		};
	}

	function clearResult() {
		currentResult = null;
		hatStore.clearLookup();
	}

	// Derive recent lookups for display
	let recentMatches = $derived(
		hatStore.recentLookups
			.filter((r) => r.outcome === 'MATCHED' && r.match)
			.slice(0, 5)
	);
</script>

<div class="h-full p-6">
	<div class="max-w-3xl mx-auto">
		<!-- Hero Section -->
		<div class="text-center mb-8">
			<h2 class="text-2xl font-bold text-clinical-text mb-2">Scan or Search</h2>
			<p class="text-clinical-muted">
				Scan a barcode or enter an identifier to look up an asset
			</p>
		</div>

		<!-- Scan Input -->
		<div class="mb-10">
			<ScanInput onResult={handleLookupResult} />
		</div>

		<!-- Result Area -->
		{#if currentResult}
			<div class="mb-8">
				<div class="flex items-center justify-between mb-4">
					<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider">
						Result
					</h3>
					<button
						onclick={clearResult}
						class="text-xs text-clinical-muted hover:text-clinical-text transition-colors"
					>
						Clear
					</button>
				</div>

				<LookupResultCard
					result={currentResult}
					onViewAsset={handleViewAsset}
					onCreateRequest={handleCreateRequest}
					onCreatePlaceholder={handleCreatePlaceholder}
					onSelectCandidate={handleSelectCandidate}
				/>
			</div>
		{/if}

		<!-- Recent Lookups -->
		{#if recentMatches.length > 0 && !currentResult}
			<div>
				<div class="flex items-center justify-between mb-4">
					<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider">
						Recent Lookups
					</h3>
					<button
						onclick={() => hatStore.clearRecentLookups()}
						class="text-xs text-clinical-muted hover:text-clinical-text transition-colors"
					>
						Clear History
					</button>
				</div>

				<div class="space-y-2">
					{#each recentMatches as lookup}
						{#if lookup.match}
							<button
								onclick={() => lookup.match && handleViewAsset(lookup.match)}
								class="w-full flex items-center justify-between p-4 bg-clinical-surface hover:bg-clinical-hover border border-clinical-border rounded-lg transition-colors text-left"
							>
								<div class="flex items-center gap-4">
									<div
										class="h-10 w-10 rounded-lg bg-clinical-primary/10 flex items-center justify-center"
									>
										<svg
											class="w-5 h-5 text-clinical-primary"
											fill="none"
											viewBox="0 0 24 24"
											stroke="currentColor"
										>
											<path
												stroke-linecap="round"
												stroke-linejoin="round"
												stroke-width="2"
												d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
											/>
										</svg>
									</div>
									<div>
										<p class="font-mono font-medium text-clinical-text">
											{lookup.query.normalized}
										</p>
										<p class="text-xs text-clinical-muted">
											{lookup.match.location.facility}
											{#if lookup.match.location.storage}
												&bull; {lookup.match.location.storage}
											{/if}
										</p>
									</div>
								</div>
								<div class="flex items-center gap-3">
									<StatusBadge status={lookup.match.status} size="xs" />
									<svg
										class="h-5 w-5 text-clinical-muted"
										fill="none"
										viewBox="0 0 24 24"
										stroke="currentColor"
									>
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M9 5l7 7-7 7"
										/>
									</svg>
								</div>
							</button>
						{/if}
					{/each}
				</div>
			</div>
		{/if}

		<!-- Empty State -->
		{#if !currentResult && recentMatches.length === 0}
			<div class="text-center py-12">
				<div
					class="mx-auto h-16 w-16 rounded-full bg-clinical-surface flex items-center justify-center mb-4 border border-clinical-border"
				>
					<svg
						class="h-8 w-8 text-clinical-muted"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="1.5"
							d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"
						/>
					</svg>
				</div>
				<p class="text-clinical-muted">
					Scan a barcode or type an identifier to begin
				</p>
				<p class="text-sm text-clinical-muted/70 mt-1">
					Try: <span class="font-mono">S24-001</span> or <span class="font-mono">AMBIG-TEST</span> or <span class="font-mono">NOTFOUND-TEST</span>
				</p>
			</div>
		{/if}
	</div>
</div>
