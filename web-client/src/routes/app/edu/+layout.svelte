<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { eduStore } from '$lib/stores/edu.svelte';

	let { children } = $props();

	function isTabActive(tab: { href: string; exact?: boolean }): boolean {
		const currentPath = page.url.pathname;
		if (tab.exact) {
			return currentPath === tab.href;
		}
		return currentPath.startsWith(tab.href);
	}

	onMount(() => {
		eduStore.loadFacets();
	});
</script>

<div class="flex h-full flex-col">
	<!-- Education Sub-navigation -->
	<nav class="shrink-0 border-b border-clinical-border bg-clinical-surface px-6 py-3">
		<div class="flex items-center gap-6">
			<h1 class="flex items-center gap-2 text-lg font-semibold text-clinical-text">
				<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
						d="M12 14l9-5-9-5-9 5 9 5z" />
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
						d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
						d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222" />
				</svg>
				Education
			</h1>

			<div class="flex gap-1">
				<a
					href="/app/edu"
					class="rounded-md px-4 py-2 text-sm font-medium transition-all {isTabActive({ href: '/app/edu', exact: true })
						? 'bg-clinical-primary/10 text-clinical-primary'
						: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text'}"
				>
					Browse Cases
				</a>
				<a
					href="/app/edu/collections"
					class="rounded-md px-4 py-2 text-sm font-medium transition-all {isTabActive({ href: '/app/edu/collections' })
						? 'bg-clinical-primary/10 text-clinical-primary'
						: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text'}"
				>
					Collections
				</a>
				<a
					href="/app/edu/my-cases"
					class="rounded-md px-4 py-2 text-sm font-medium transition-all {isTabActive({ href: '/app/edu/my-cases' })
						? 'bg-clinical-primary/10 text-clinical-primary'
						: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text'}"
				>
					My Cases
				</a>
			</div>

			{#if eduStore.totalItems > 0}
				<div class="ml-auto flex items-center gap-4 text-sm text-clinical-muted">
					<span>
						<span class="font-medium text-clinical-text">{eduStore.totalItems}</span> Total Cases
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
