<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { eduStore } from '$lib/stores/edu.svelte';
	import {
		DIFFICULTY_LABELS,
		DIFFICULTY_COLORS,
		STATUS_LABELS,
		STATUS_COLORS,
		CATEGORY_LABELS,
		VISIBILITY_LABELS
	} from '$lib/types/edu';

	const collectionId = $derived(page.params.id);

	onMount(() => {
		if (collectionId) {
			eduStore.loadCollectionDetail(collectionId);
		}
	});

	function handleCaseClick(accession: string) {
		goto(`/app/edu/case/${accession}`);
	}
</script>

<div class="p-6">
	{#if eduStore.isLoading}
		<div class="flex items-center justify-center py-20">
			<div class="h-8 w-8 animate-spin rounded-full border-2 border-clinical-primary border-t-transparent"></div>
		</div>
	{:else if eduStore.selectedCollection}
		{@const col = eduStore.selectedCollection}
		<!-- Header -->
		<div class="mb-6">
			<a
				href="/app/edu/collections"
				class="mb-2 inline-flex items-center gap-1 text-xs text-clinical-muted hover:text-clinical-primary"
			>
				<svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
				</svg>
				Back to Collections
			</a>
			<h2 class="text-xl font-semibold text-clinical-text">{col.name}</h2>
			{#if col.description}
				<p class="mt-1 text-sm text-clinical-muted">{col.description}</p>
			{/if}
			<div class="mt-2 flex items-center gap-3 text-xs text-clinical-muted">
				<span class="rounded-full bg-clinical-bg px-2 py-0.5 font-medium">
					{VISIBILITY_LABELS[col.visibility]}
				</span>
				{#if col.ownerDisplay}
					<span>By {col.ownerDisplay}</span>
				{/if}
				<span>{col.cases.length} case{col.cases.length !== 1 ? 's' : ''}</span>
			</div>
		</div>

		<!-- Cases List -->
		{#if col.cases.length === 0}
			<p class="py-10 text-center text-sm text-clinical-muted">No cases in this collection</p>
		{:else}
			<div class="grid gap-3 sm:grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
				{#each col.cases as caseItem, i (caseItem.id)}
					<button
						type="button"
						class="group rounded-lg border border-clinical-border bg-clinical-surface p-4 text-left transition-all hover:border-clinical-primary/30 hover:shadow-md"
						onclick={() => handleCaseClick(caseItem.caseId)}
					>
						<div class="mb-2 flex items-start justify-between">
							<div>
								<p class="text-xs text-clinical-muted">#{i + 1}</p>
								<p class="text-sm font-semibold text-clinical-text group-hover:text-clinical-primary">
									{caseItem.caseId}
								</p>
								<p class="text-xs text-clinical-muted">{caseItem.anatomicSite ?? 'Unknown site'}</p>
							</div>
							{#if caseItem.difficultyLevel}
								<span class="rounded-full border px-2 py-0.5 text-[10px] font-medium {DIFFICULTY_COLORS[caseItem.difficultyLevel]}">
									{DIFFICULTY_LABELS[caseItem.difficultyLevel]}
								</span>
							{/if}
						</div>
						<p class="mb-2 line-clamp-2 text-xs text-clinical-text">
							{caseItem.primaryDiagnosis ?? 'No diagnosis'}
						</p>
						<div class="flex items-center gap-3 text-[10px] text-clinical-muted">
							{#if caseItem.teachingCategory}
								<span>{CATEGORY_LABELS[caseItem.teachingCategory] ?? caseItem.teachingCategory}</span>
							{/if}
							<span>{caseItem.slideCount} slide{caseItem.slideCount !== 1 ? 's' : ''}</span>
						</div>
					</button>
				{/each}
			</div>
		{/if}
	{:else}
		<p class="py-10 text-center text-sm text-clinical-muted">Collection not found</p>
	{/if}
</div>
