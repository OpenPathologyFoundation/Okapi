<script lang="ts">
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import {
		ALL_SERVICES,
		ALL_PRIORITIES,
		ACTIVE_STATUSES,
		SERVICE_LABELS,
		STATUS_LABELS,
		PRIORITY_LABELS,
		SERVICE_COLORS,
		STATUS_COLORS,
		PRIORITY_COLORS
	} from '$lib/types/worklist';
	import type { PathologyService, CaseStatus, CasePriority } from '$lib/types/worklist';

	function isServiceSelected(service: PathologyService): boolean {
		return worklistStore.filters.services.includes(service);
	}

	function isStatusSelected(status: CaseStatus): boolean {
		return worklistStore.filters.statuses.includes(status);
	}

	function isPrioritySelected(priority: CasePriority): boolean {
		return worklistStore.filters.priorities.includes(priority);
	}

	function hasActiveFilters(): boolean {
		return (
			worklistStore.filters.services.length > 0 ||
			worklistStore.filters.statuses.length > 0 ||
			worklistStore.filters.priorities.length > 0 ||
			worklistStore.filters.search !== ''
		);
	}
</script>

<div class="space-y-4">
	<!-- Search -->
	<div>
		<input
			type="text"
			placeholder="Search accession, patient, MRN..."
			value={worklistStore.filters.search}
			oninput={(e) => worklistStore.setSearch(e.currentTarget.value)}
			class="w-full rounded-lg border border-clinical-border bg-clinical-bg px-4 py-2 text-sm text-clinical-text placeholder-clinical-muted focus:border-clinical-primary focus:outline-none focus:ring-1 focus:ring-clinical-primary"
		/>
	</div>

	<!-- Service filters -->
	<div>
		<div class="mb-2 text-xs font-medium uppercase tracking-wide text-clinical-muted">Service</div>
		<div class="flex flex-wrap gap-2">
			{#each ALL_SERVICES as service}
				<button
					type="button"
					onclick={() => worklistStore.toggleService(service)}
					class="rounded-full border px-3 py-1 text-xs font-medium transition-colors {isServiceSelected(
						service
					)
						? SERVICE_COLORS[service]
						: 'border-clinical-border text-clinical-muted hover:border-clinical-primary/50 hover:text-clinical-text'}"
				>
					{SERVICE_LABELS[service]}
					{#if worklistStore.counts?.byService[service]}
						<span class="ml-1 opacity-70">({worklistStore.counts.byService[service]})</span>
					{/if}
				</button>
			{/each}
		</div>
	</div>

	<!-- Status filters -->
	<div>
		<div class="mb-2 text-xs font-medium uppercase tracking-wide text-clinical-muted">Status</div>
		<div class="flex flex-wrap gap-2">
			{#each ACTIVE_STATUSES as status}
				<button
					type="button"
					onclick={() => worklistStore.toggleStatus(status)}
					class="rounded-full border px-3 py-1 text-xs font-medium transition-colors {isStatusSelected(
						status
					)
						? STATUS_COLORS[status]
						: 'border-clinical-border text-clinical-muted hover:border-clinical-primary/50 hover:text-clinical-text'}"
				>
					{STATUS_LABELS[status]}
					{#if worklistStore.counts?.byStatus[status]}
						<span class="ml-1 opacity-70">({worklistStore.counts.byStatus[status]})</span>
					{/if}
				</button>
			{/each}
		</div>
	</div>

	<!-- Priority filters -->
	<div>
		<div class="mb-2 text-xs font-medium uppercase tracking-wide text-clinical-muted">Priority</div>
		<div class="flex flex-wrap gap-2">
			{#each ALL_PRIORITIES as priority}
				<button
					type="button"
					onclick={() => worklistStore.togglePriority(priority)}
					class="rounded-full border px-3 py-1 text-xs font-medium transition-colors {isPrioritySelected(
						priority
					)
						? PRIORITY_COLORS[priority]
						: 'border-clinical-border text-clinical-muted hover:border-clinical-primary/50 hover:text-clinical-text'}"
				>
					{PRIORITY_LABELS[priority]}
					{#if worklistStore.counts?.byPriority[priority]}
						<span class="ml-1 opacity-70">({worklistStore.counts.byPriority[priority]})</span>
					{/if}
				</button>
			{/each}
		</div>
	</div>

	<!-- Clear filters -->
	{#if hasActiveFilters()}
		<div>
			<button
				type="button"
				onclick={() => worklistStore.clearFilters()}
				class="text-xs text-clinical-muted hover:text-clinical-primary"
			>
				Clear all filters
			</button>
		</div>
	{/if}
</div>
