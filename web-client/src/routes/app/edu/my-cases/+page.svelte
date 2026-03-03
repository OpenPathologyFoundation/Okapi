<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { eduStore } from '$lib/stores/edu.svelte';
	import { authStore } from '$lib/stores/auth.svelte';
	import {
		DIFFICULTY_LABELS,
		DIFFICULTY_COLORS,
		STATUS_LABELS,
		STATUS_COLORS,
		CATEGORY_LABELS,
		ROLE_LABELS
	} from '$lib/types/edu';
	import type { DifficultyLevel, EduCaseStatus, EduCuratorRole } from '$lib/types/edu';

	onMount(() => {
		if (authStore.user?.identityId) {
			eduStore.setFilter('curatorId', authStore.user.identityId);
			eduStore.loadCases();
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
	{:else if eduStore.cases.length === 0}
		<div class="py-20 text-center">
			<svg class="mx-auto mb-4 h-12 w-12 text-clinical-muted/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
					d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222" />
			</svg>
			<p class="text-sm text-clinical-muted">You are not assigned as a curator on any cases</p>
		</div>
	{:else}
		<div class="grid gap-3 sm:grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
			{#each eduStore.cases as caseItem (caseItem.id)}
				<button
					type="button"
					class="group rounded-lg border border-clinical-border bg-clinical-surface p-4 text-left transition-all hover:border-clinical-primary/30 hover:shadow-md"
					onclick={() => handleCaseClick(caseItem.caseId)}
				>
					<div class="mb-2 flex items-start justify-between">
						<div>
							<p class="text-sm font-semibold text-clinical-text group-hover:text-clinical-primary">
								{caseItem.caseId}
							</p>
							<p class="text-xs text-clinical-muted">
								{caseItem.anatomicSite ?? 'Unknown site'}
							</p>
						</div>
						<div class="flex items-center gap-1.5">
							{#if caseItem.difficultyLevel}
								<span class="rounded-full border px-2 py-0.5 text-[10px] font-medium {DIFFICULTY_COLORS[caseItem.difficultyLevel]}">
									{DIFFICULTY_LABELS[caseItem.difficultyLevel]}
								</span>
							{/if}
							{#if caseItem.status}
								<span class="rounded-full border px-2 py-0.5 text-[10px] font-medium {STATUS_COLORS[caseItem.status]}">
									{STATUS_LABELS[caseItem.status]}
								</span>
							{/if}
						</div>
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

					{#if caseItem.curriculumTags && caseItem.curriculumTags.length > 0}
						<div class="mt-2 flex flex-wrap gap-1">
							{#each caseItem.curriculumTags.slice(0, 3) as tag}
								<span class="rounded bg-clinical-bg px-1.5 py-0.5 text-[10px] text-clinical-muted">
									{tag}
								</span>
							{/each}
						</div>
					{/if}
				</button>
			{/each}
		</div>
	{/if}
</div>
