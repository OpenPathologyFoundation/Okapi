<script lang="ts">
	import type { LookupResult, AssetCurrentState } from '$lib/types/hat';
	import StatusBadge from './StatusBadge.svelte';

	let {
		result,
		onViewAsset = (_asset: AssetCurrentState) => {},
		onCreateRequest = (_asset: AssetCurrentState) => {},
		onCreatePlaceholder = (_query: string) => {},
		onSelectCandidate = (_asset: AssetCurrentState) => {}
	}: {
		result: LookupResult;
		onViewAsset?: (asset: AssetCurrentState) => void;
		onCreateRequest?: (asset: AssetCurrentState) => void;
		onCreatePlaceholder?: (query: string) => void;
		onSelectCandidate?: (asset: AssetCurrentState) => void;
	} = $props();

	function getPrimaryIdentifier(asset: AssetCurrentState): string {
		const normalized = asset.identifiers.find((i) => i.type === 'BARCODE_NORMALIZED');
		if (normalized) return normalized.value;
		const accession = asset.identifiers.find((i) => i.type === 'LIS_ACCESSION');
		if (accession) return accession.value;
		return asset.identifiers[0]?.value ?? asset.id;
	}

	function getSecondaryIdentifier(asset: AssetCurrentState): string | null {
		const accession = asset.identifiers.find((i) => i.type === 'LIS_ACCESSION');
		const normalized = asset.identifiers.find((i) => i.type === 'BARCODE_NORMALIZED');
		if (accession && normalized) return accession.value;
		return null;
	}
</script>

{#if result.outcome === 'MATCHED' && result.match}
	<!-- MATCHED: Show asset card with actions -->
	<div
		class="bg-clinical-surface border border-clinical-success/30 rounded-xl p-6 animate-in fade-in slide-in-from-bottom-2 duration-300"
	>
		<div class="flex items-start justify-between gap-4">
			<div class="flex-1">
				<div class="flex items-center gap-3 mb-2">
					<span class="text-xs uppercase tracking-wider text-clinical-success font-medium"
						>Match Found</span
					>
					<StatusBadge status={result.match.status} />
				</div>

				<h3 class="text-xl font-mono font-semibold text-clinical-text mb-1">
					{getPrimaryIdentifier(result.match)}
				</h3>

				{#if getSecondaryIdentifier(result.match)}
					<p class="text-sm text-clinical-muted font-mono">
						{getSecondaryIdentifier(result.match)}
					</p>
				{/if}

				<div class="mt-4 flex items-center gap-2 text-sm text-clinical-muted">
					<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
						/>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
						/>
					</svg>
					<span>
						{result.match.location.facility}
						{#if result.match.location.storage}
							&bull; {result.match.location.storage}
						{/if}
					</span>
				</div>

				{#if result.match.custody}
					<div class="mt-2 flex items-center gap-2 text-sm text-clinical-muted">
						<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path
								stroke-linecap="round"
								stroke-linejoin="round"
								stroke-width="2"
								d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
							/>
						</svg>
						<span>Custody: {result.match.custody.holderDisplay}</span>
					</div>
				{/if}
			</div>

			<!-- Actions -->
			<div class="flex flex-col gap-2">
				<button
					onclick={() => result.match && onViewAsset(result.match)}
					class="px-4 py-2 text-sm font-medium text-clinical-primary bg-clinical-primary/10 hover:bg-clinical-primary/20 rounded-lg transition-colors"
				>
					View Details
				</button>
				<button
					onclick={() => result.match && onCreateRequest(result.match)}
					class="px-4 py-2 text-sm font-medium text-clinical-text bg-clinical-hover hover:bg-clinical-border/50 border border-clinical-border rounded-lg transition-colors"
				>
					Create Request
				</button>
			</div>
		</div>
	</div>
{:else if result.outcome === 'NOT_FOUND'}
	<!-- NOT_FOUND: Offer to create placeholder -->
	<div
		class="bg-clinical-surface border border-hat-status-missing/30 rounded-xl p-6 animate-in fade-in slide-in-from-bottom-2 duration-300"
	>
		<div class="flex items-start justify-between gap-4">
			<div class="flex-1">
				<div class="flex items-center gap-3 mb-2">
					<span class="text-xs uppercase tracking-wider text-hat-status-missing font-medium"
						>Not Found</span
					>
				</div>

				<h3 class="text-lg font-mono text-clinical-text mb-1">
					"{result.query.raw}"
				</h3>

				<p class="text-sm text-clinical-muted">
					{result.message ?? 'No asset matches this identifier in the system.'}
				</p>

				{#if result.query.raw !== result.query.normalized}
					<p class="mt-2 text-xs text-clinical-muted">
						Searched as: <span class="font-mono">{result.query.normalized}</span>
					</p>
				{/if}
			</div>

			<!-- Actions -->
			<div class="flex flex-col gap-2">
				<button
					onclick={() => onCreatePlaceholder(result.query.normalized)}
					class="px-4 py-2 text-sm font-medium text-hat-status-missing bg-hat-status-missing/10 hover:bg-hat-status-missing/20 rounded-lg transition-colors"
				>
					Create Placeholder
				</button>
			</div>
		</div>
	</div>
{:else if result.outcome === 'AMBIGUOUS' && result.candidates}
	<!-- AMBIGUOUS: Show candidate list -->
	<div
		class="bg-clinical-surface border border-hat-status-in-transit/30 rounded-xl p-6 animate-in fade-in slide-in-from-bottom-2 duration-300"
	>
		<div class="mb-4">
			<div class="flex items-center gap-3 mb-2">
				<span class="text-xs uppercase tracking-wider text-hat-status-in-transit font-medium"
					>Multiple Matches</span
				>
			</div>

			<p class="text-sm text-clinical-muted">
				{result.message ?? 'Multiple assets match this identifier. Please select one:'}
			</p>
		</div>

		<div class="space-y-2">
			{#each result.candidates as candidate}
				<button
					onclick={() => onSelectCandidate(candidate)}
					class="w-full flex items-center justify-between p-4 bg-clinical-hover hover:bg-clinical-border/50 border border-clinical-border rounded-lg transition-colors text-left"
				>
					<div>
						<div class="flex items-center gap-2">
							<span class="font-mono font-medium text-clinical-text">
								{getPrimaryIdentifier(candidate)}
							</span>
							<StatusBadge status={candidate.status} size="xs" />
						</div>
						{#if getSecondaryIdentifier(candidate)}
							<p class="text-xs text-clinical-muted font-mono mt-1">
								{getSecondaryIdentifier(candidate)}
							</p>
						{/if}
						<p class="text-xs text-clinical-muted mt-1">
							{candidate.location.facility}
							{#if candidate.location.storage}
								&bull; {candidate.location.storage}
							{/if}
						</p>
					</div>
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
				</button>
			{/each}
		</div>
	</div>
{/if}
