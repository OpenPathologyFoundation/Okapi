<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { hatStore } from '$lib/stores/hat.svelte';

	let { children } = $props();

	const tabs = [
		{ href: '/app/hat', label: 'Scan / Lookup', icon: 'barcode', exact: true },
		{ href: '/app/hat/requests', label: 'Requests', icon: 'clipboard' },
		{ href: '/app/hat/queue', label: 'Work Queue', icon: 'queue' }
	];

	function isTabActive(tab: { href: string; exact?: boolean }): boolean {
		const currentPath = page.url.pathname;
		if (tab.exact) {
			return currentPath === tab.href;
		}
		return currentPath.startsWith(tab.href);
	}

	onMount(() => {
		hatStore.init();
	});
</script>

<div class="h-full flex flex-col">
	<!-- HAT Sub-navigation -->
	<nav class="shrink-0 px-6 py-3 bg-clinical-surface border-b border-gray-800">
		<div class="flex items-center gap-6">
			<h1 class="text-lg font-semibold text-clinical-text flex items-center gap-2">
				<svg class="w-5 h-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
					/>
				</svg>
				Asset Tracking
			</h1>

			<div class="flex gap-1">
				{#each tabs as tab}
					<a
						href={tab.href}
						class="px-4 py-2 text-sm font-medium rounded-md transition-all {isTabActive(tab)
							? 'bg-clinical-primary/10 text-clinical-primary'
							: 'text-clinical-muted hover:text-clinical-text hover:bg-white/5'}"
					>
						{tab.label}
					</a>
				{/each}
			</div>

			<!-- Quick actions -->
			<div class="ml-auto flex items-center gap-2">
				<a
					href="/app/hat/requests/new"
					class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-clinical-text bg-white/5 hover:bg-white/10 border border-gray-700 rounded-md transition-colors"
				>
					<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
					</svg>
					New Request
				</a>
			</div>
		</div>
	</nav>

	<!-- Content area -->
	<div class="flex-1 overflow-auto">
		{@render children()}
	</div>
</div>
