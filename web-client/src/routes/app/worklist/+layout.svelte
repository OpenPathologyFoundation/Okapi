<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { onMount, onDestroy } from 'svelte';
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import { authStore } from '$lib/stores/auth.svelte';

	let { children } = $props();

	const canSeeAllCases = $derived(
		authStore.hasPermission('CASE_REASSIGN') || authStore.isAdmin
	);

	function isTabActive(tab: { href: string; exact?: boolean }): boolean {
		const currentPath = page.url.pathname;
		if (tab.exact) {
			return currentPath === tab.href;
		}
		return currentPath.startsWith(tab.href);
	}

	onMount(() => {
		worklistStore.init();
		worklistStore.loadWorklist();
		worklistStore.startAutoRefresh();

		// If user lacks CASE_REASSIGN and lands on All Cases, redirect to My Cases
		if (!canSeeAllCases && page.url.pathname === '/app/worklist') {
			goto('/app/worklist/my-cases');
		}
	});

	onDestroy(() => {
		worklistStore.stopAutoRefresh();
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
				{#if canSeeAllCases}
					<a
						href="/app/worklist"
						class="rounded-md px-4 py-2 text-sm font-medium transition-all {isTabActive({ href: '/app/worklist', exact: true })
							? 'bg-clinical-primary/10 text-clinical-primary'
							: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text'}"
					>
						All Cases
					</a>
				{/if}
				<a
					href="/app/worklist/my-cases"
					class="rounded-md px-4 py-2 text-sm font-medium transition-all {isTabActive({ href: '/app/worklist/my-cases' })
						? 'bg-clinical-primary/10 text-clinical-primary'
						: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text'}"
				>
					My Cases
					{#if worklistStore.counts}
						<span class="ml-1 text-xs opacity-70">({worklistStore.counts.myCases})</span>
					{/if}
				</a>
			</div>

			<!-- Stats summary -->
			{#if worklistStore.counts}
				<div class="ml-auto flex items-center gap-4 text-sm text-clinical-muted">
					<span>
						<span class="font-medium text-worklist-priority-stat">{worklistStore.counts.byPriority.STAT ?? 0}</span> STAT
					</span>
					<span>
						<span class="font-medium text-worklist-priority-urgent">{worklistStore.counts.byPriority.URGENT ?? 0}</span> Urgent
					</span>
					<span>
						<span class="font-medium text-clinical-text">{worklistStore.counts.total}</span> Total
					</span>
					<div class="h-4 w-px bg-clinical-border"></div>
					<!-- Refresh timestamp (clickable per spec §12.1) -->
					<button
						type="button"
						onclick={() => worklistStore.loadWorklist()}
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
