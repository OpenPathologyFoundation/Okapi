<script lang="ts">
	import type { AssetCurrentState } from '$lib/types/hat';
	import StatusBadge from './StatusBadge.svelte';

	let {
		asset,
		onClick = undefined,
		showActions = false
	}: {
		asset: AssetCurrentState;
		onClick?: () => void;
		showActions?: boolean;
	} = $props();

	function getPrimaryIdentifier(): string {
		const normalized = asset.identifiers.find((i) => i.type === 'BARCODE_NORMALIZED');
		if (normalized) return normalized.value;
		const accession = asset.identifiers.find((i) => i.type === 'LIS_ACCESSION');
		if (accession) return accession.value;
		return asset.identifiers[0]?.value ?? asset.id;
	}

	function getSecondaryIdentifier(): string | null {
		const accession = asset.identifiers.find((i) => i.type === 'LIS_ACCESSION');
		const normalized = asset.identifiers.find((i) => i.type === 'BARCODE_NORMALIZED');
		if (accession && normalized) return accession.value;
		return null;
	}
</script>

<div
	class="bg-clinical-surface border border-gray-700 rounded-lg p-4 transition-colors {onClick
		? 'hover:bg-white/5 cursor-pointer'
		: ''}"
	onclick={onClick}
	onkeydown={(e) => e.key === 'Enter' && onClick?.()}
	role={onClick ? 'button' : undefined}
	tabindex={onClick ? 0 : undefined}
>
	<div class="flex items-start justify-between gap-4">
		<div class="flex-1 min-w-0">
			<div class="flex items-center gap-2 mb-1">
				<h3 class="font-mono font-semibold text-clinical-text truncate">
					{getPrimaryIdentifier()}
				</h3>
				<StatusBadge status={asset.status} size="xs" />
			</div>

			{#if getSecondaryIdentifier()}
				<p class="text-xs text-clinical-muted font-mono truncate">
					{getSecondaryIdentifier()}
				</p>
			{/if}

			<div class="mt-2 flex items-center gap-1 text-xs text-clinical-muted">
				<svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
					/>
				</svg>
				<span class="truncate">
					{asset.location.facility}
					{#if asset.location.storage}
						&bull; {asset.location.storage}
					{/if}
				</span>
			</div>

			{#if asset.custody}
				<div class="mt-1 flex items-center gap-1 text-xs text-clinical-muted">
					<svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
						/>
					</svg>
					<span class="truncate">{asset.custody.holderDisplay}</span>
				</div>
			{/if}

			<!-- Lifecycle flags -->
			{#if asset.lifecycleFlags.isPlaceholder || asset.lifecycleFlags.hasConflict || asset.lifecycleFlags.isHighRisk}
				<div class="mt-2 flex items-center gap-1.5">
					{#if asset.lifecycleFlags.isPlaceholder}
						<span
							class="text-[10px] px-1.5 py-0.5 rounded bg-hat-status-in-transit/20 text-hat-status-in-transit"
						>
							Placeholder
						</span>
					{/if}
					{#if asset.lifecycleFlags.hasConflict}
						<span
							class="text-[10px] px-1.5 py-0.5 rounded bg-hat-status-missing/20 text-hat-status-missing"
						>
							Conflict
						</span>
					{/if}
					{#if asset.lifecycleFlags.isHighRisk}
						<span
							class="text-[10px] px-1.5 py-0.5 rounded bg-hat-priority-urgent/20 text-hat-priority-urgent"
						>
							High Risk
						</span>
					{/if}
				</div>
			{/if}
		</div>

		{#if onClick}
			<svg
				class="h-5 w-5 text-clinical-muted shrink-0"
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
		{/if}
	</div>
</div>
