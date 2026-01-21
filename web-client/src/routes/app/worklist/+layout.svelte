<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { worklistStore, MOCK_PATHOLOGISTS } from '$lib/stores/worklist.svelte';

	let { children } = $props();

	const tabs = [
		{ href: '/app/worklist', label: 'All Cases', icon: 'list', exact: true },
		{ href: '/app/worklist/my-cases', label: 'My Cases', icon: 'user' }
	];

	function isTabActive(tab: { href: string; exact?: boolean }): boolean {
		const currentPath = page.url.pathname;
		if (tab.exact) {
			return currentPath === tab.href;
		}
		return currentPath.startsWith(tab.href);
	}

	function handleUserChange(event: Event) {
		const select = event.target as HTMLSelectElement;
		const userId = parseInt(select.value, 10);
		worklistStore.setCurrentUser(userId);
	}

	onMount(() => {
		worklistStore.init();
		// Load mock data for development (replace with API call in production)
		worklistStore.loadMockData();
	});
</script>

<div class="flex h-full flex-col">
	<!-- Worklist Sub-navigation -->
	<nav class="shrink-0 border-b border-clinical-border bg-clinical-surface px-6 py-3">
		<div class="flex items-center gap-6">
			<h1 class="flex items-center gap-2 text-lg font-semibold text-clinical-text">
				<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
					/>
				</svg>
				Worklist
			</h1>

			<div class="flex gap-1">
				{#each tabs as tab}
					<a
						href={tab.href}
						class="rounded-md px-4 py-2 text-sm font-medium transition-all {isTabActive(tab)
							? 'bg-clinical-primary/10 text-clinical-primary'
							: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text'}"
					>
						{tab.label}
						{#if tab.label === 'My Cases' && worklistStore.counts}
							<span class="ml-1 text-xs opacity-70">({worklistStore.counts.myCases})</span>
						{/if}
					</a>
				{/each}
			</div>

			<!-- Dev: User selector for testing -->
			<div class="flex items-center gap-2 rounded-md border border-dashed border-amber-500/50 bg-amber-500/5 px-3 py-1.5">
				<span class="text-[10px] font-medium uppercase tracking-wider text-amber-600 dark:text-amber-400">Dev</span>
				<select
					value={worklistStore.currentUser.id}
					onchange={handleUserChange}
					class="bg-transparent text-xs text-clinical-text focus:outline-none cursor-pointer"
				>
					{#each MOCK_PATHOLOGISTS as pathologist}
						<option value={pathologist.id} class="bg-clinical-surface text-clinical-text">
							{pathologist.display}
						</option>
					{/each}
				</select>
			</div>

			<!-- Stats summary -->
			{#if worklistStore.counts}
				<div class="ml-auto flex items-center gap-4 text-sm text-clinical-muted">
					<span>
						<span class="font-medium text-worklist-priority-stat">{worklistStore.counts.byPriority.STAT}</span> STAT
					</span>
					<span>
						<span class="font-medium text-worklist-priority-urgent">{worklistStore.counts.byPriority.URGENT}</span> Urgent
					</span>
					<span>
						<span class="font-medium text-clinical-text">{worklistStore.counts.total}</span> Total
					</span>
					<div class="h-4 w-px bg-clinical-border"></div>
					<!-- Refresh timestamp (clickable per spec §12.1) -->
					<button
						type="button"
						onclick={() => worklistStore.loadMockData()}
						class="flex items-center gap-1.5 text-xs hover:text-clinical-primary"
						title="Click to refresh"
						disabled={worklistStore.isLoading}
					>
						<svg
							class="h-3.5 w-3.5 {worklistStore.isLoading ? 'animate-spin' : ''}"
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
						{worklistStore.getLastRefreshDisplay()}
					</button>
				</div>
			{/if}
		</div>
	</nav>

	<!-- Content area -->
	<div class="flex-1 overflow-auto">
		{@render children()}
	</div>
</div>
