<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { eduStore } from '$lib/stores/edu.svelte';
	import {
		DIFFICULTY_LABELS,
		DIFFICULTY_COLORS,
		STATUS_LABELS,
		STATUS_COLORS,
		CATEGORY_LABELS
	} from '$lib/types/edu';
	import type { DifficultyLevel, EduCaseStatus } from '$lib/types/edu';

	let showFilters = $state(true);

	onMount(() => {
		eduStore.loadCases();
	});

	function handleCaseClick(accession: string) {
		goto(`/app/edu/case/${accession}`);
	}

	function handleSearch(e: Event) {
		const target = e.target as HTMLInputElement;
		eduStore.setFilter('query', target.value);
		eduStore.loadCases();
	}

	function handleFilterChange(key: 'anatomicSite' | 'difficulty' | 'stain', value: string) {
		eduStore.setFilter(key, value);
		eduStore.loadCases();
	}

	function handleClearFilters() {
		eduStore.clearFilters();
		eduStore.loadCases();
	}

	function handlePageChange(newPage: number) {
		eduStore.page = newPage;
		eduStore.loadCases();
	}
</script>

<div class="flex h-full">
	<!-- Filters Sidebar -->
	{#if showFilters}
		<aside class="w-72 shrink-0 overflow-y-auto border-r border-clinical-border bg-clinical-surface p-4">
			<div class="mb-4 flex items-center justify-between">
				<h2 class="text-sm font-semibold text-clinical-text">Filters</h2>
				{#if eduStore.hasActiveFilters}
					<button
						type="button"
						class="text-xs text-clinical-primary hover:underline"
						onclick={handleClearFilters}
					>
						Clear all
					</button>
				{/if}
			</div>

			<!-- Search -->
			<div class="mb-4">
				<label for="edu-search" class="mb-1 block text-xs font-medium text-clinical-muted">Search</label>
				<input
					id="edu-search"
					type="text"
					placeholder="Case ID, diagnosis, organ..."
					value={eduStore.filters.query}
					oninput={handleSearch}
					class="w-full rounded-md border border-clinical-border bg-clinical-bg px-3 py-2 text-sm text-clinical-text placeholder:text-clinical-muted focus:border-clinical-primary focus:outline-none focus:ring-1 focus:ring-clinical-primary"
				/>
			</div>

			<!-- Anatomic Site -->
			{#if eduStore.facets?.byAnatomicSite}
				<div class="mb-4">
					<h3 class="mb-2 text-xs font-medium text-clinical-muted">Anatomic Site</h3>
					<div class="flex flex-col gap-1">
						{#each Object.entries(eduStore.facets.byAnatomicSite) as [site, count]}
							<button
								type="button"
								class="flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-all {eduStore.filters.anatomicSite === site
									? 'bg-clinical-primary/10 text-clinical-primary'
									: 'text-clinical-text hover:bg-clinical-hover'}"
								onclick={() => handleFilterChange('anatomicSite', eduStore.filters.anatomicSite === site ? '' : site)}
							>
								<span>{site}</span>
								<span class="text-clinical-muted">{count}</span>
							</button>
						{/each}
					</div>
				</div>
			{/if}

			<!-- Difficulty -->
			{#if eduStore.facets?.byDifficulty}
				<div class="mb-4">
					<h3 class="mb-2 text-xs font-medium text-clinical-muted">Difficulty</h3>
					<div class="flex flex-col gap-1">
						{#each Object.entries(eduStore.facets.byDifficulty) as [level, count]}
							<button
								type="button"
								class="flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-all {eduStore.filters.difficulty === level
									? 'bg-clinical-primary/10 text-clinical-primary'
									: 'text-clinical-text hover:bg-clinical-hover'}"
								onclick={() => handleFilterChange('difficulty', eduStore.filters.difficulty === level ? '' : level)}
							>
								<span>{DIFFICULTY_LABELS[level as DifficultyLevel] ?? level}</span>
								<span class="text-clinical-muted">{count}</span>
							</button>
						{/each}
					</div>
				</div>
			{/if}

			<!-- Stain -->
			{#if eduStore.facets?.byStain}
				<div class="mb-4">
					<h3 class="mb-2 text-xs font-medium text-clinical-muted">Stain</h3>
					<div class="flex flex-col gap-1">
						{#each Object.entries(eduStore.facets.byStain) as [stain, count]}
							<button
								type="button"
								class="flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-all {eduStore.filters.stain === stain
									? 'bg-clinical-primary/10 text-clinical-primary'
									: 'text-clinical-text hover:bg-clinical-hover'}"
								onclick={() => handleFilterChange('stain', eduStore.filters.stain === stain ? '' : stain)}
							>
								<span>{stain}</span>
								<span class="text-clinical-muted">{count}</span>
							</button>
						{/each}
					</div>
				</div>
			{/if}
		</aside>
	{/if}

	<!-- Main Content -->
	<div class="flex-1 overflow-auto bg-clinical-bg">
		<!-- Toolbar -->
		<div class="flex items-center gap-3 border-b border-clinical-border bg-clinical-surface px-4 py-2">
			<button
				type="button"
				class="rounded-md p-1.5 text-clinical-muted hover:bg-clinical-hover hover:text-clinical-text"
				title={showFilters ? 'Hide filters' : 'Show filters'}
				onclick={() => (showFilters = !showFilters)}
			>
				<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
						d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
				</svg>
			</button>

			{#if eduStore.hasActiveFilters}
				<div class="flex flex-wrap gap-1.5">
					{#if eduStore.filters.query}
						<span class="inline-flex items-center gap-1 rounded-full bg-clinical-primary/10 px-2.5 py-0.5 text-xs font-medium text-clinical-primary">
							"{eduStore.filters.query}"
							<button type="button" onclick={() => { eduStore.setFilter('query', ''); eduStore.loadCases(); }} class="hover:text-clinical-text">&times;</button>
						</span>
					{/if}
					{#if eduStore.filters.anatomicSite}
						<span class="inline-flex items-center gap-1 rounded-full bg-clinical-primary/10 px-2.5 py-0.5 text-xs font-medium text-clinical-primary">
							{eduStore.filters.anatomicSite}
							<button type="button" onclick={() => { eduStore.setFilter('anatomicSite', ''); eduStore.loadCases(); }} class="hover:text-clinical-text">&times;</button>
						</span>
					{/if}
					{#if eduStore.filters.difficulty}
						<span class="inline-flex items-center gap-1 rounded-full bg-clinical-primary/10 px-2.5 py-0.5 text-xs font-medium text-clinical-primary">
							{DIFFICULTY_LABELS[eduStore.filters.difficulty as DifficultyLevel] ?? eduStore.filters.difficulty}
							<button type="button" onclick={() => { eduStore.setFilter('difficulty', ''); eduStore.loadCases(); }} class="hover:text-clinical-text">&times;</button>
						</span>
					{/if}
					{#if eduStore.filters.stain}
						<span class="inline-flex items-center gap-1 rounded-full bg-clinical-primary/10 px-2.5 py-0.5 text-xs font-medium text-clinical-primary">
							{eduStore.filters.stain}
							<button type="button" onclick={() => { eduStore.setFilter('stain', ''); eduStore.loadCases(); }} class="hover:text-clinical-text">&times;</button>
						</span>
					{/if}
				</div>
			{/if}

			<div class="ml-auto text-xs text-clinical-muted">
				{eduStore.totalItems} case{eduStore.totalItems !== 1 ? 's' : ''}
			</div>
		</div>

		<!-- Loading State -->
		{#if eduStore.isLoading}
			<div class="flex items-center justify-center py-20">
				<div class="h-8 w-8 animate-spin rounded-full border-2 border-clinical-primary border-t-transparent"></div>
			</div>
		{:else if eduStore.error}
			<div class="px-6 py-10 text-center">
				<p class="text-sm text-red-500">{eduStore.error}</p>
				<button
					type="button"
					class="mt-3 text-sm text-clinical-primary hover:underline"
					onclick={() => eduStore.loadCases()}
				>
					Retry
				</button>
			</div>
		{:else if eduStore.cases.length === 0}
			<div class="px-6 py-20 text-center">
				<svg class="mx-auto mb-4 h-12 w-12 text-clinical-muted/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
						d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222" />
				</svg>
				<p class="text-sm text-clinical-muted">No educational cases found</p>
				{#if eduStore.hasActiveFilters}
					<button
						type="button"
						class="mt-2 text-sm text-clinical-primary hover:underline"
						onclick={handleClearFilters}
					>
						Clear filters
					</button>
				{/if}
			</div>
		{:else}
			<!-- Case Cards Grid -->
			<div class="grid gap-3 p-4 sm:grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
				{#each eduStore.cases as caseItem (caseItem.id)}
					<button
						type="button"
						class="group rounded-lg border border-clinical-border bg-clinical-surface p-4 text-left transition-all hover:border-clinical-primary/30 hover:shadow-md"
						onclick={() => handleCaseClick(caseItem.caseId)}
					>
						<!-- Header -->
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

						<!-- Diagnosis -->
						<p class="mb-2 line-clamp-2 text-xs text-clinical-text">
							{caseItem.primaryDiagnosis ?? 'No diagnosis'}
						</p>

						<!-- Footer -->
						<div class="flex items-center justify-between text-[10px] text-clinical-muted">
							<div class="flex items-center gap-3">
								{#if caseItem.teachingCategory}
									<span>{CATEGORY_LABELS[caseItem.teachingCategory] ?? caseItem.teachingCategory}</span>
								{/if}
								<span>{caseItem.slideCount} slide{caseItem.slideCount !== 1 ? 's' : ''}</span>
							</div>
							{#if caseItem.primaryCuratorDisplay}
								<span class="truncate max-w-[120px]">{caseItem.primaryCuratorDisplay}</span>
							{/if}
						</div>

						<!-- Tags -->
						{#if caseItem.curriculumTags && caseItem.curriculumTags.length > 0}
							<div class="mt-2 flex flex-wrap gap-1">
								{#each caseItem.curriculumTags.slice(0, 3) as tag}
									<span class="rounded bg-clinical-bg px-1.5 py-0.5 text-[10px] text-clinical-muted">
										{tag}
									</span>
								{/each}
								{#if caseItem.curriculumTags.length > 3}
									<span class="rounded bg-clinical-bg px-1.5 py-0.5 text-[10px] text-clinical-muted">
										+{caseItem.curriculumTags.length - 3}
									</span>
								{/if}
							</div>
						{/if}
					</button>
				{/each}
			</div>

			<!-- Pagination -->
			{#if eduStore.totalPages > 1}
				<div class="flex items-center justify-center gap-2 border-t border-clinical-border bg-clinical-surface px-4 py-3">
					<button
						type="button"
						disabled={eduStore.page === 0}
						class="rounded-md px-3 py-1.5 text-xs font-medium transition-all {eduStore.page === 0
							? 'cursor-not-allowed text-clinical-muted'
							: 'text-clinical-text hover:bg-clinical-hover'}"
						onclick={() => handlePageChange(eduStore.page - 1)}
					>
						Previous
					</button>
					<span class="text-xs text-clinical-muted">
						Page {eduStore.page + 1} of {eduStore.totalPages}
					</span>
					<button
						type="button"
						disabled={eduStore.page >= eduStore.totalPages - 1}
						class="rounded-md px-3 py-1.5 text-xs font-medium transition-all {eduStore.page >= eduStore.totalPages - 1
							? 'cursor-not-allowed text-clinical-muted'
							: 'text-clinical-text hover:bg-clinical-hover'}"
						onclick={() => handlePageChange(eduStore.page + 1)}
					>
						Next
					</button>
				</div>
			{/if}
		{/if}
	</div>
</div>
