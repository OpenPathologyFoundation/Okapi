<script lang="ts">
	import { goto } from '$app/navigation';
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import WorklistTable from '$lib/components/worklist/WorklistTable.svelte';
	import WorklistFilters from '$lib/components/worklist/WorklistFilters.svelte';

	let showFilters = $state(true);

	function handleCaseClick(accessionNumber: string) {
		// TODO: Navigate to case detail view
		console.log('Case clicked:', accessionNumber);
		// goto(`/app/case/${accessionNumber}`);
	}
</script>

<div class="flex h-full">
	<!-- Filters sidebar -->
	{#if showFilters}
		<aside class="w-72 shrink-0 border-r border-clinical-border bg-clinical-surface p-4">
			<div class="mb-4 flex items-center justify-between">
				<h2 class="text-sm font-semibold uppercase tracking-wide text-clinical-muted">Filters</h2>
				<button
					type="button"
					onclick={() => (showFilters = false)}
					class="text-clinical-muted hover:text-clinical-text"
					title="Hide filters"
				>
					<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
					</svg>
				</button>
			</div>
			<WorklistFilters />
		</aside>
	{/if}

	<!-- Main content -->
	<main class="flex-1 overflow-hidden bg-clinical-bg">
		<!-- Toolbar -->
		<div class="flex items-center gap-4 border-b border-clinical-border bg-clinical-surface px-4 py-3">
			{#if !showFilters}
				<button
					type="button"
					onclick={() => (showFilters = true)}
					class="flex items-center gap-2 rounded-md border border-clinical-border px-3 py-1.5 text-sm text-clinical-text hover:bg-clinical-hover"
				>
					<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
						/>
					</svg>
					Filters
				</button>
			{/if}

			<div class="flex-1"></div>

			<!-- Refresh button -->
			<button
				type="button"
				onclick={() => worklistStore.loadMockData()}
				class="flex items-center gap-2 rounded-md border border-clinical-border px-3 py-1.5 text-sm text-clinical-text hover:bg-clinical-hover"
				disabled={worklistStore.isLoading}
			>
				<svg
					class="h-4 w-4 {worklistStore.isLoading ? 'animate-spin' : ''}"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
					/>
				</svg>
				Refresh
			</button>
		</div>

		<!-- Table -->
		<div class="h-[calc(100%-57px)] overflow-auto p-4">
			{#if worklistStore.error}
				<div class="rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-red-600 dark:text-red-400">
					<p class="font-medium">Error loading worklist</p>
					<p class="text-sm">{worklistStore.error}</p>
				</div>
			{:else}
				<div class="rounded-lg border border-clinical-border bg-clinical-surface">
					<WorklistTable onCaseClick={handleCaseClick} />
				</div>
			{/if}
		</div>
	</main>
</div>
