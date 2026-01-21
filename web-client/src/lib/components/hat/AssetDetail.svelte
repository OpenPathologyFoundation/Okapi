<script lang="ts">
	import type { AssetCurrentState } from '$lib/types/hat';
	import { STATUS_LABELS } from '$lib/types/hat';
	import StatusBadge from './StatusBadge.svelte';

	let {
		asset,
		onCreateRequest = () => {},
		onUpdateStatus = () => {},
		onTransferCustody = () => {}
	}: {
		asset: AssetCurrentState;
		onCreateRequest?: () => void;
		onUpdateStatus?: () => void;
		onTransferCustody?: () => void;
	} = $props();

	function formatDate(dateStr: string): string {
		return new Date(dateStr).toLocaleString();
	}
</script>

<div class="space-y-6">
	<!-- Header with identifiers -->
	<div class="bg-clinical-surface border border-clinical-border rounded-xl p-6">
		<div class="flex items-start justify-between">
			<div>
				<div class="flex items-center gap-3 mb-3">
					<StatusBadge status={asset.status} size="md" />

					{#if asset.lifecycleFlags.isPlaceholder}
						<span class="text-xs px-2 py-1 rounded bg-hat-status-in-transit/20 text-hat-status-in-transit">
							Placeholder
						</span>
					{/if}
					{#if asset.lifecycleFlags.hasConflict}
						<span class="text-xs px-2 py-1 rounded bg-hat-status-missing/20 text-hat-status-missing">
							Needs Reconciliation
						</span>
					{/if}
					{#if asset.lifecycleFlags.isHighRisk}
						<span class="text-xs px-2 py-1 rounded bg-hat-priority-urgent/20 text-hat-priority-urgent">
							High Risk
						</span>
					{/if}
				</div>

				<h2 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-2">
					Identifiers
				</h2>

				<div class="space-y-1">
					{#each asset.identifiers as identifier}
						<div class="flex items-center gap-2">
							<span class="text-xs text-clinical-muted w-24">{identifier.type.replace(/_/g, ' ')}:</span>
							<span class="font-mono text-clinical-text">{identifier.value}</span>
							<span class="text-xs text-clinical-muted">({identifier.source})</span>
						</div>
					{/each}
				</div>
			</div>

			<!-- Actions -->
			<div class="flex flex-col gap-2">
				<button
					onclick={onCreateRequest}
					class="px-4 py-2 text-sm font-medium text-white bg-clinical-primary hover:bg-clinical-primary/90 rounded-lg transition-colors"
				>
					Create Request
				</button>
				<button
					onclick={onUpdateStatus}
					class="px-4 py-2 text-sm font-medium text-clinical-text bg-clinical-hover hover:bg-clinical-border/50 border border-clinical-border rounded-lg transition-colors"
				>
					Update Status
				</button>
				<button
					onclick={onTransferCustody}
					class="px-4 py-2 text-sm font-medium text-clinical-text bg-clinical-hover hover:bg-clinical-border/50 border border-clinical-border rounded-lg transition-colors"
				>
					Transfer Custody
				</button>
			</div>
		</div>
	</div>

	<!-- Current State Grid -->
	<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
		<!-- Location -->
		<div class="bg-clinical-surface border border-clinical-border rounded-xl p-5">
			<div class="flex items-center gap-2 mb-4">
				<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider">
					Location
				</h3>
			</div>

			<div class="space-y-2">
				<div>
					<p class="text-lg font-medium text-clinical-text">{asset.location.facility}</p>
					{#if asset.location.building}
						<p class="text-sm text-clinical-muted">{asset.location.building}</p>
					{/if}
				</div>

				{#if asset.location.room || asset.location.storage}
					<div class="pt-2 border-t border-clinical-border">
						{#if asset.location.room}
							<p class="text-sm text-clinical-muted">Room: {asset.location.room}</p>
						{/if}
						{#if asset.location.storage}
							<p class="text-sm text-clinical-text font-medium">{asset.location.storage}</p>
						{/if}
					</div>
				{/if}

				<p class="text-xs text-clinical-muted pt-2">
					Updated: {formatDate(asset.location.assertedAt)}
				</p>
			</div>
		</div>

		<!-- Custody -->
		<div class="bg-clinical-surface border border-clinical-border rounded-xl p-5">
			<div class="flex items-center gap-2 mb-4">
				<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
					/>
				</svg>
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider">
					Custody
				</h3>
			</div>

			{#if asset.custody}
				<div class="space-y-2">
					<p class="text-lg font-medium text-clinical-text">{asset.custody.holderDisplay}</p>

					<p class="text-sm text-clinical-muted">
						Since: {formatDate(asset.custody.checkedOutAt)}
					</p>

					{#if asset.custody.dueBackAt}
						<p class="text-sm text-hat-priority-urgent">
							Due back: {formatDate(asset.custody.dueBackAt)}
						</p>
					{/if}

					{#if asset.custody.purpose}
						<p class="text-sm text-clinical-muted pt-2 border-t border-clinical-border">
							Purpose: {asset.custody.purpose}
						</p>
					{/if}
				</div>
			{:else}
				<div class="text-center py-4">
					<p class="text-clinical-muted">No active custody</p>
					<p class="text-sm text-clinical-muted/70">Asset is available</p>
				</div>
			{/if}
		</div>
	</div>

	<!-- Provenance -->
	<div class="bg-clinical-surface border border-clinical-border rounded-xl p-5">
		<div class="flex items-center gap-2 mb-4">
			<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
				/>
			</svg>
			<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider">
				Provenance
			</h3>
		</div>

		<div class="flex items-center justify-between">
			<div>
				<p class="text-sm text-clinical-muted">
					Source: <span class="text-clinical-text font-medium">{asset.provenance.source}</span>
				</p>
				<p class="text-sm text-clinical-muted">
					Asserted by: <span class="text-clinical-text">{asset.provenance.assertedBy}</span>
				</p>
				{#if asset.provenance.comment}
					<p class="text-sm text-clinical-muted mt-2">
						Note: {asset.provenance.comment}
					</p>
				{/if}
			</div>
			<p class="text-xs text-clinical-muted">
				{formatDate(asset.provenance.assertedAt)}
			</p>
		</div>
	</div>
</div>
