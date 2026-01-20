<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { worklistStore } from '$lib/stores/worklist.svelte';

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
				</div>
			{/if}
		</div>
	</nav>

	<!-- Content area -->
	<div class="flex-1 overflow-auto">
		{@render children()}
	</div>
</div>
