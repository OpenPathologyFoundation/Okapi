<script lang="ts">
	import { onMount } from 'svelte';
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import WorklistTable from '$lib/components/worklist/WorklistTable.svelte';

	onMount(() => {
		// Set "My Cases" filter on mount
		worklistStore.setMyCasesOnly(true);
		worklistStore.showActiveCasesOnly();
	});

	function handleCaseClick(accessionNumber: string) {
		// TODO: Navigate to case detail view
		console.log('Case clicked:', accessionNumber);
	}
</script>

<div class="h-full overflow-hidden bg-clinical-bg">
	<!-- Toolbar -->
	<div class="flex items-center gap-4 border-b border-clinical-border bg-clinical-surface px-4 py-3">
		<div class="flex items-center gap-2">
			<h2 class="text-sm font-medium text-clinical-text">My Assigned Cases</h2>
			<span class="rounded-full bg-clinical-primary/20 px-2 py-0.5 text-xs font-medium text-clinical-primary">
				{worklistStore.sortedItems.length}
			</span>
		</div>

		<div class="flex-1"></div>

		<!-- Clear my-cases filter to see all -->
		<a
			href="/app/worklist"
			class="text-sm text-clinical-muted hover:text-clinical-primary"
			onclick={() => worklistStore.clearFilters()}
		>
			View All Cases
		</a>
	</div>

	<!-- Table -->
	<div class="h-[calc(100%-57px)] overflow-auto p-4">
		{#if worklistStore.sortedItems.length === 0 && !worklistStore.isLoading}
			<div class="flex flex-col items-center justify-center py-16 text-clinical-muted">
				<svg class="mb-4 h-12 w-12 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
					/>
				</svg>
				<p class="text-lg font-medium">No cases assigned to you</p>
				<p class="text-sm">Cases assigned to you will appear here.</p>
			</div>
		{:else}
			<div class="rounded-lg border border-clinical-border bg-clinical-surface">
				<WorklistTable onCaseClick={handleCaseClick} />
			</div>
		{/if}
	</div>
</div>
