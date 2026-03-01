<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import type { WorklistItem, WorklistPageResponse } from '$lib/types/worklist';
	import {
		SERVICE_LABELS,
		STATUS_LABELS,
		SERVICE_COLORS,
		STATUS_COLORS
	} from '$lib/types/worklist';

	let query = $state('');
	let results = $state<WorklistItem[]>([]);
	let totalItems = $state(0);
	let isLoading = $state(false);
	let isOpen = $state(false);
	let activeIndex = $state(-1);
	let inputEl = $state<HTMLInputElement | null>(null);
	let containerEl = $state<HTMLDivElement | null>(null);
	let debounceTimer: ReturnType<typeof setTimeout> | null = null;

	onMount(() => {
		function handleClickOutside(e: MouseEvent) {
			if (containerEl && !containerEl.contains(e.target as Node)) {
				isOpen = false;
			}
		}

		document.addEventListener('mousedown', handleClickOutside);
		return () => {
			document.removeEventListener('mousedown', handleClickOutside);
			if (debounceTimer) clearTimeout(debounceTimer);
		};
	});

	function handleInput() {
		if (debounceTimer) clearTimeout(debounceTimer);

		if (query.trim().length < 2) {
			results = [];
			totalItems = 0;
			isOpen = false;
			return;
		}

		isLoading = true;
		isOpen = true;
		activeIndex = -1;

		debounceTimer = setTimeout(async () => {
			await searchCases(query.trim());
		}, 300);
	}

	async function searchCases(term: string) {
		try {
			const params = new URLSearchParams({
				search: term,
				pageSize: '8'
			});
			const res = await fetch(`/api/worklist?${params.toString()}`);
			if (!res.ok) throw new Error('Search failed');
			const data: WorklistPageResponse = await res.json();
			results = data.items;
			totalItems = data.totalItems;
		} catch {
			results = [];
			totalItems = 0;
		} finally {
			isLoading = false;
		}
	}

	function navigateToCase(item: WorklistItem) {
		isOpen = false;
		query = '';
		results = [];
		goto(`/app/case/${item.accessionNumber}`);
	}

	function viewAllResults() {
		isOpen = false;
		const searchTerm = query.trim();
		query = '';
		results = [];
		worklistStore.setSearch(searchTerm);
		worklistStore.loadWorklist();
		goto('/app/worklist');
	}

	function clearSearch() {
		query = '';
		results = [];
		totalItems = 0;
		isOpen = false;
		activeIndex = -1;
		inputEl?.focus();
	}

	function handleKeydown(e: KeyboardEvent) {
		if (!isOpen) return;

		const hasFooter = totalItems > results.length;
		const maxIndex = results.length + (hasFooter ? 0 : -1);

		switch (e.key) {
			case 'ArrowDown':
				e.preventDefault();
				activeIndex = Math.min(activeIndex + 1, maxIndex);
				break;
			case 'ArrowUp':
				e.preventDefault();
				activeIndex = Math.max(activeIndex - 1, -1);
				break;
			case 'Enter':
				e.preventDefault();
				if (activeIndex >= 0 && activeIndex < results.length) {
					navigateToCase(results[activeIndex]);
				} else if (activeIndex === results.length && hasFooter) {
					viewAllResults();
				}
				break;
			case 'Escape':
				e.preventDefault();
				isOpen = false;
				inputEl?.blur();
				break;
		}
	}
</script>

<div bind:this={containerEl} class="flex-1 max-w-2xl relative">
	<div class="relative group">
		{#if isLoading}
			<div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
				<div
					class="h-4 w-4 animate-spin rounded-full border-2 border-clinical-muted border-t-clinical-primary"
				></div>
			</div>
		{:else}
			<div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
				<svg
					class="h-4 w-4 text-clinical-muted group-focus-within:text-clinical-primary transition-colors"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
					/>
				</svg>
			</div>
		{/if}
		<input
			bind:this={inputEl}
			bind:value={query}
			oninput={handleInput}
			onkeydown={handleKeydown}
			onfocus={() => {
				if (query.trim().length >= 2 && results.length > 0) isOpen = true;
			}}
			type="text"
			placeholder="Search cases by accession, patient, or MRN..."
			class="block w-full rounded-md border-0 bg-clinical-bg py-1.5 pl-10 pr-8 text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 transition-all"
		/>
		{#if query.length > 0}
			<button
				type="button"
				onclick={clearSearch}
				aria-label="Clear search"
				class="absolute inset-y-0 right-0 pr-3 flex items-center text-clinical-muted hover:text-clinical-text transition-colors"
			>
				<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M6 18L18 6M6 6l12 12"
					/>
				</svg>
			</button>
		{/if}
	</div>

	{#if isOpen}
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div
			class="absolute left-0 top-full z-50 mt-1 w-full rounded-lg border border-clinical-border bg-clinical-surface shadow-lg overflow-hidden"
			onkeydown={handleKeydown}
		>
			{#if isLoading && results.length === 0}
				<div class="flex items-center justify-center px-4 py-6">
					<div
						class="h-4 w-4 animate-spin rounded-full border-2 border-clinical-muted border-t-clinical-primary"
					></div>
					<span class="ml-2 text-sm text-clinical-muted">Searching...</span>
				</div>
			{:else if results.length > 0}
				<ul class="max-h-[400px] overflow-y-auto">
					{#each results as item, i (item.id)}
						<li>
							<button
								type="button"
								onclick={() => navigateToCase(item)}
								onmouseenter={() => (activeIndex = i)}
								class="flex w-full items-center gap-3 px-4 py-2.5 text-left transition-colors {activeIndex ===
								i
									? 'bg-clinical-hover'
									: 'hover:bg-clinical-hover'}"
							>
								<span class="shrink-0 font-mono text-sm font-semibold text-clinical-primary">
									{item.accessionNumber}
								</span>
								<span class="min-w-0 flex-1 truncate text-sm text-clinical-text">
									{item.patientDisplay ?? '—'}
									{#if item.patientMrn}
										<span class="text-clinical-muted">({item.patientMrn})</span>
									{/if}
								</span>
								<span
									class="shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-medium {SERVICE_COLORS[
										item.service
									]}"
								>
									{SERVICE_LABELS[item.service]}
								</span>
								<span
									class="shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-medium {STATUS_COLORS[
										item.status
									]}"
								>
									{STATUS_LABELS[item.status]}
								</span>
							</button>
						</li>
					{/each}
				</ul>
				{#if totalItems > results.length}
					<button
						type="button"
						onclick={viewAllResults}
						onmouseenter={() => (activeIndex = results.length)}
						class="flex w-full items-center justify-center gap-1 border-t border-clinical-border px-4 py-2.5 text-sm font-medium transition-colors {activeIndex ===
						results.length
							? 'bg-clinical-hover text-clinical-primary'
							: 'text-clinical-muted hover:bg-clinical-hover hover:text-clinical-primary'}"
					>
						View all {totalItems} results in Worklist
						<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path
								stroke-linecap="round"
								stroke-linejoin="round"
								stroke-width="2"
								d="M9 5l7 7-7 7"
							/>
						</svg>
					</button>
				{/if}
			{:else if query.trim().length >= 2 && !isLoading}
				<div class="px-4 py-6 text-center text-sm text-clinical-muted">
					No cases found for "{query.trim()}"
				</div>
			{/if}
		</div>
	{/if}
</div>
