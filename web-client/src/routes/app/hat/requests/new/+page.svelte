<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import type { RequestAction, RequestPriority, AssetCurrentState } from '$lib/types/hat';
	import { ACTION_LABELS } from '$lib/types/hat';
	import { hatStore } from '$lib/stores/hat.svelte';
	import ScanInput from '$lib/components/hat/ScanInput.svelte';
	import AssetCard from '$lib/components/hat/AssetCard.svelte';
	import PriorityBadge from '$lib/components/hat/PriorityBadge.svelte';

	// Form state
	let selectedActions = $state<RequestAction[]>([]);
	let priority = $state<RequestPriority>('ROUTINE');
	let selectedAssets = $state<AssetCurrentState[]>([]);
	let assignee = $state('');
	let dueDate = $state('');
	let notes = $state('');
	let isSubmitting = $state(false);

	// Pre-populate asset from query param
	const preselectedAssetId = $derived(page.url.searchParams.get('asset'));

	$effect(() => {
		if (preselectedAssetId) {
			const asset = hatStore.getAsset(preselectedAssetId);
			if (asset && !selectedAssets.find((a) => a.id === asset.id)) {
				selectedAssets = [...selectedAssets, asset];
			}
		}
	});

	const allActions: RequestAction[] = ['PULL', 'STAIN', 'RECUT', 'QA', 'SCAN', 'DISTRIBUTE', 'RETURN', 'DESTROY'];

	function toggleAction(action: RequestAction) {
		if (selectedActions.includes(action)) {
			selectedActions = selectedActions.filter((a) => a !== action);
		} else {
			selectedActions = [...selectedActions, action];
		}
	}

	function handleAssetLookup(result: { outcome: string; match?: AssetCurrentState }) {
		if (result.outcome === 'MATCHED' && result.match) {
			if (!selectedAssets.find((a) => a.id === result.match!.id)) {
				selectedAssets = [...selectedAssets, result.match];
			}
		}
	}

	function removeAsset(assetId: string) {
		selectedAssets = selectedAssets.filter((a) => a.id !== assetId);
	}

	async function handleSubmit() {
		if (selectedActions.length === 0 || selectedAssets.length === 0) {
			return;
		}

		isSubmitting = true;

		try {
			// Simulate API call
			await new Promise((resolve) => setTimeout(resolve, 500));

			const newRequest = {
				id: 'REQ-' + Date.now(),
				actions: selectedActions,
				priority,
				status: 'OPEN',
				assets: selectedAssets.map((a) => ({ assetId: a.id, status: 'PENDING' })),
				dueDate: dueDate || undefined,
				createdAt: new Date().toISOString(),
				updatedAt: new Date().toISOString()
			};

			console.log('Created request:', newRequest);
			goto('/app/hat/requests');
		} finally {
			isSubmitting = false;
		}
	}

	let canSubmit = $derived(selectedActions.length > 0 && selectedAssets.length > 0);
</script>

<div class="h-full p-6 overflow-auto">
	<div class="max-w-3xl mx-auto">
		<!-- Header -->
		<div class="mb-6">
			<a
				href="/app/hat/requests"
				class="inline-flex items-center gap-2 text-sm text-clinical-muted hover:text-clinical-text transition-colors mb-4"
			>
				<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
				</svg>
				Back to Requests
			</a>
			<h2 class="text-xl font-semibold text-clinical-text">Create New Request</h2>
		</div>

		<form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-8">
			<!-- Actions -->
			<div class="bg-clinical-surface border border-gray-700 rounded-xl p-6">
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
					Actions <span class="text-hat-status-missing">*</span>
				</h3>

				<div class="flex flex-wrap gap-2">
					{#each allActions as action}
						<button
							type="button"
							onclick={() => toggleAction(action)}
							class="px-4 py-2 text-sm font-medium rounded-lg border transition-colors {selectedActions.includes(action)
								? 'bg-clinical-primary text-white border-clinical-primary'
								: 'bg-white/5 text-clinical-text border-gray-700 hover:bg-white/10'}"
						>
							{ACTION_LABELS[action]}
						</button>
					{/each}
				</div>
			</div>

			<!-- Priority -->
			<div class="bg-clinical-surface border border-gray-700 rounded-xl p-6">
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
					Priority
				</h3>

				<div class="flex gap-3">
					{#each ['STAT', 'URGENT', 'ROUTINE'] as p}
						<button
							type="button"
							onclick={() => (priority = p as RequestPriority)}
							class="flex-1 py-3 text-sm font-medium rounded-lg border transition-colors {priority === p
								? 'bg-clinical-primary/10 border-clinical-primary text-clinical-primary'
								: 'bg-white/5 border-gray-700 text-clinical-text hover:bg-white/10'}"
						>
							<PriorityBadge priority={p as RequestPriority} />
						</button>
					{/each}
				</div>
			</div>

			<!-- Assets -->
			<div class="bg-clinical-surface border border-gray-700 rounded-xl p-6">
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
					Assets <span class="text-hat-status-missing">*</span>
				</h3>

				<div class="mb-4">
					<ScanInput
						onResult={handleAssetLookup}
						placeholder="Scan or enter asset identifier to add..."
						autofocus={false}
					/>
				</div>

				{#if selectedAssets.length > 0}
					<div class="space-y-2">
						{#each selectedAssets as asset}
							<div class="flex items-center gap-2">
								<div class="flex-1">
									<AssetCard {asset} />
								</div>
								<button
									type="button"
									onclick={() => removeAsset(asset.id)}
									class="p-2 text-clinical-muted hover:text-hat-status-missing transition-colors"
								>
									<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
									</svg>
								</button>
							</div>
						{/each}
					</div>
				{:else}
					<p class="text-sm text-clinical-muted text-center py-4">
						No assets added yet. Scan or search to add assets.
					</p>
				{/if}
			</div>

			<!-- Optional fields -->
			<div class="bg-clinical-surface border border-gray-700 rounded-xl p-6 space-y-4">
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
					Additional Details
				</h3>

				<div>
					<label for="assignee" class="block text-sm text-clinical-muted mb-1">Assignee</label>
					<input
						id="assignee"
						type="text"
						bind:value={assignee}
						placeholder="Team or person to assign..."
						class="w-full px-4 py-2 bg-clinical-bg border border-gray-700 rounded-lg text-clinical-text placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary focus:border-transparent"
					/>
				</div>

				<div>
					<label for="dueDate" class="block text-sm text-clinical-muted mb-1">Due Date</label>
					<input
						id="dueDate"
						type="datetime-local"
						bind:value={dueDate}
						class="w-full px-4 py-2 bg-clinical-bg border border-gray-700 rounded-lg text-clinical-text focus:ring-2 focus:ring-clinical-primary focus:border-transparent"
					/>
				</div>

				<div>
					<label for="notes" class="block text-sm text-clinical-muted mb-1">Notes</label>
					<textarea
						id="notes"
						bind:value={notes}
						rows="3"
						placeholder="Additional instructions or notes..."
						class="w-full px-4 py-2 bg-clinical-bg border border-gray-700 rounded-lg text-clinical-text placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary focus:border-transparent resize-none"
					></textarea>
				</div>
			</div>

			<!-- Submit -->
			<div class="flex justify-end gap-4">
				<a
					href="/app/hat/requests"
					class="px-6 py-3 text-sm font-medium text-clinical-text bg-white/5 hover:bg-white/10 border border-gray-700 rounded-lg transition-colors"
				>
					Cancel
				</a>
				<button
					type="submit"
					disabled={!canSubmit || isSubmitting}
					class="px-6 py-3 text-sm font-medium text-white bg-clinical-primary hover:bg-clinical-primary/90 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
				>
					{#if isSubmitting}
						Creating...
					{:else}
						Create Request
					{/if}
				</button>
			</div>
		</form>
	</div>
</div>
