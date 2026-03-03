<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { eduStore } from '$lib/stores/edu.svelte';
	import { VISIBILITY_LABELS } from '$lib/types/edu';

	onMount(() => {
		eduStore.loadCollections();
	});

	function handleCollectionClick(id: string) {
		goto(`/app/edu/collections/${id}`);
	}
</script>

<div class="p-6">
	{#if eduStore.isLoading}
		<div class="flex items-center justify-center py-20">
			<div class="h-8 w-8 animate-spin rounded-full border-2 border-clinical-primary border-t-transparent"></div>
		</div>
	{:else if eduStore.error}
		<div class="py-10 text-center">
			<p class="text-sm text-red-500">{eduStore.error}</p>
			<button
				type="button"
				class="mt-3 text-sm text-clinical-primary hover:underline"
				onclick={() => eduStore.loadCollections()}
			>
				Retry
			</button>
		</div>
	{:else if eduStore.collections.length === 0}
		<div class="py-20 text-center">
			<svg class="mx-auto mb-4 h-12 w-12 text-clinical-muted/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
					d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
			</svg>
			<p class="text-sm text-clinical-muted">No collections available</p>
		</div>
	{:else}
		<div class="grid gap-4 sm:grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
			{#each eduStore.collections as collection (collection.id)}
				<button
					type="button"
					class="group rounded-lg border border-clinical-border bg-clinical-surface p-5 text-left transition-all hover:border-clinical-primary/30 hover:shadow-md"
					onclick={() => handleCollectionClick(collection.id)}
				>
					<div class="mb-2 flex items-start justify-between">
						<h3 class="text-sm font-semibold text-clinical-text group-hover:text-clinical-primary">
							{collection.name}
						</h3>
						<span class="rounded-full bg-clinical-bg px-2 py-0.5 text-[10px] font-medium text-clinical-muted">
							{VISIBILITY_LABELS[collection.visibility]}
						</span>
					</div>

					{#if collection.description}
						<p class="mb-3 line-clamp-2 text-xs text-clinical-muted">
							{collection.description}
						</p>
					{/if}

					<div class="flex items-center justify-between text-[10px] text-clinical-muted">
						<span>{collection.caseCount} case{collection.caseCount !== 1 ? 's' : ''}</span>
						{#if collection.ownerDisplay}
							<span class="truncate max-w-[150px]">{collection.ownerDisplay}</span>
						{/if}
					</div>
				</button>
			{/each}
		</div>
	{/if}
</div>
