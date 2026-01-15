<script lang="ts">
	import type { LookupResult } from '$lib/types/hat';
	import { hatStore } from '$lib/stores/hat.svelte';

	let {
		onResult = (_result: LookupResult) => {},
		autofocus = true,
		placeholder = 'Scan barcode or enter identifier...',
		useMock = true
	}: {
		onResult?: (result: LookupResult) => void;
		autofocus?: boolean;
		placeholder?: string;
		useMock?: boolean;
	} = $props();

	let inputValue = $state('');
	let isLoading = $state(false);
	let inputElement: HTMLInputElement | undefined = $state();

	// Real-time normalization preview
	let normalizedPreview = $derived(
		inputValue.trim() ? hatStore.normalizeBarcode(inputValue) : ''
	);

	let showNormalized = $derived(
		inputValue.trim() !== '' && inputValue !== normalizedPreview
	);

	async function handleSubmit() {
		const query = inputValue.trim();
		if (!query || isLoading) return;

		isLoading = true;

		try {
			let result: LookupResult;

			if (useMock) {
				// Use mock lookup for UI development
				await new Promise((resolve) => setTimeout(resolve, 300));
				result = hatStore.mockLookup(query);
			} else {
				// Use real API lookup
				result = await hatStore.lookup(query);
			}

			onResult(result);

			// Clear input on successful match
			if (result.outcome === 'MATCHED') {
				inputValue = '';
			}
		} catch (e) {
			console.error('Lookup failed:', e);
		} finally {
			isLoading = false;
		}
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter') {
			e.preventDefault();
			handleSubmit();
		}
		if (e.key === 'Escape') {
			inputValue = '';
			inputElement?.blur();
		}
	}

	function focusInput() {
		inputElement?.focus();
	}

	export { focusInput };
</script>

<div class="relative">
	<div class="relative">
		<!-- Search Icon -->
		<div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
			{#if isLoading}
				<svg
					class="animate-spin h-5 w-5 text-clinical-primary"
					fill="none"
					viewBox="0 0 24 24"
				>
					<circle
						class="opacity-25"
						cx="12"
						cy="12"
						r="10"
						stroke="currentColor"
						stroke-width="4"
					/>
					<path
						class="opacity-75"
						fill="currentColor"
						d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
					/>
				</svg>
			{:else}
				<svg
					class="h-5 w-5 text-clinical-muted"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"
					/>
				</svg>
			{/if}
		</div>

		<!-- Input Field -->
		<input
			bind:this={inputElement}
			bind:value={inputValue}
			onkeydown={handleKeydown}
			type="text"
			class="block w-full pl-12 pr-12 py-4 text-lg bg-clinical-surface border border-gray-300 dark:border-gray-700 rounded-xl text-clinical-text placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary focus:border-transparent transition-all font-mono"
			{placeholder}
			{autofocus}
			autocomplete="off"
			spellcheck="false"
		/>

		<!-- Submit Button -->
		<button
			type="button"
			onclick={handleSubmit}
			disabled={!inputValue.trim() || isLoading}
			class="absolute inset-y-0 right-0 pr-4 flex items-center text-clinical-muted hover:text-clinical-primary disabled:opacity-30 disabled:hover:text-clinical-muted transition-colors"
		>
			<svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
				/>
			</svg>
		</button>
	</div>

	<!-- Normalization Preview -->
	{#if showNormalized}
		<div
			class="absolute -bottom-6 left-4 text-xs text-clinical-muted transition-opacity"
		>
			Normalized:
			<span class="font-mono text-clinical-primary">{normalizedPreview}</span>
		</div>
	{/if}
</div>
