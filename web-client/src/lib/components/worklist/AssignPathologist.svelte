<script lang="ts">
	import { onMount } from 'svelte';
	import { csrfHeaders } from '$lib/csrf';

	interface IdentityResult {
		identityId: string;
		displayName: string;
		displayShort: string;
		email: string;
		roles: string[];
	}

	let {
		caseUuid,
		onAssigned,
		onClose
	}: {
		caseUuid: string;
		onAssigned: () => void;
		onClose: () => void;
	} = $props();

	let query = $state('');
	let results = $state<IdentityResult[]>([]);
	let isSearching = $state(false);
	let isAssigning = $state(false);
	let error = $state<string | null>(null);
	let inputEl = $state<HTMLInputElement | null>(null);
	let containerEl = $state<HTMLDivElement | null>(null);
	let debounceTimer: ReturnType<typeof setTimeout> | null = null;

	onMount(() => {
		inputEl?.focus();

		function handleClickOutside(e: MouseEvent) {
			if (containerEl && !containerEl.contains(e.target as Node)) {
				onClose();
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
		error = null;

		if (query.trim().length < 2) {
			results = [];
			return;
		}

		debounceTimer = setTimeout(async () => {
			await searchIdentities(query.trim());
		}, 300);
	}

	async function searchIdentities(term: string) {
		isSearching = true;
		try {
			const res = await fetch(`/api/identities/search?q=${encodeURIComponent(term)}&limit=10`);
			if (!res.ok) throw new Error('Search failed');
			results = await res.json();
		} catch {
			results = [];
		} finally {
			isSearching = false;
		}
	}

	async function assignPathologist(identity: IdentityResult) {
		isAssigning = true;
		error = null;
		try {
			const res = await fetch(`/api/cases/${caseUuid}/assignments`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					...csrfHeaders()
				},
				body: JSON.stringify({
					identityId: identity.identityId,
					designation: 'PRIMARY'
				})
			});
			if (!res.ok) {
				const text = await res.text();
				throw new Error(text || `Assignment failed (${res.status})`);
			}
			onAssigned();
		} catch (e) {
			error = e instanceof Error ? e.message : 'Assignment failed';
		} finally {
			isAssigning = false;
		}
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') {
			onClose();
		}
	}
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
	bind:this={containerEl}
	class="absolute left-0 top-full z-50 mt-1 w-72 rounded-lg border border-clinical-border bg-clinical-surface shadow-lg"
	onkeydown={handleKeydown}
>
	<div class="p-2">
		<input
			bind:this={inputEl}
			bind:value={query}
			oninput={handleInput}
			type="text"
			placeholder="Search by name or email..."
			class="w-full rounded-md border border-clinical-border bg-clinical-bg px-3 py-1.5 text-sm text-clinical-text placeholder:text-clinical-muted focus:border-clinical-primary focus:outline-none"
			disabled={isAssigning}
		/>
	</div>

	{#if error}
		<div class="px-2 pb-2">
			<p class="text-xs text-red-500">{error}</p>
		</div>
	{/if}

	{#if isSearching}
		<div class="flex items-center justify-center px-2 pb-3 pt-1">
			<div class="h-4 w-4 animate-spin rounded-full border-2 border-clinical-muted border-t-clinical-primary"></div>
			<span class="ml-2 text-xs text-clinical-muted">Searching...</span>
		</div>
	{:else if results.length > 0}
		<ul class="max-h-48 overflow-y-auto border-t border-clinical-border">
			{#each results as identity (identity.identityId)}
				<li>
					<button
						type="button"
						onclick={(e) => { e.stopPropagation(); assignPathologist(identity); }}
						class="flex w-full flex-col gap-0.5 px-3 py-2 text-left hover:bg-clinical-hover"
						disabled={isAssigning}
					>
						<span class="text-sm font-medium text-clinical-text">{identity.displayName}</span>
						{#if identity.displayShort && identity.displayShort !== identity.displayName}
							<span class="text-xs text-clinical-muted">{identity.displayShort}</span>
						{/if}
					</button>
				</li>
			{/each}
		</ul>
	{:else if query.trim().length >= 2}
		<div class="border-t border-clinical-border px-3 py-3 text-center text-xs text-clinical-muted">
			No matching identities found
		</div>
	{/if}
</div>
