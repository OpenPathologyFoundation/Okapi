<script lang="ts">
	import type { WorkRequest, RequestAssetExecution } from '$lib/types/hat';
	import { ACTION_LABELS } from '$lib/types/hat';
	import { hatStore } from '$lib/stores/hat.svelte';
	import ScanInput from '$lib/components/hat/ScanInput.svelte';
	import PriorityBadge from '$lib/components/hat/PriorityBadge.svelte';

	// Current execution context
	let currentRequest = $state<WorkRequest | null>(null);
	let scanResult = $state<{ success: boolean; message: string } | null>(null);

	// Mock queue data
	const mockQueue: WorkRequest[] = [
		{
			id: 'REQ-2024-001',
			actions: ['PULL', 'SCAN'],
			priority: 'STAT',
			status: 'IN_PROGRESS',
			assets: [
				{ assetId: 'S24-1001-A', status: 'COMPLETED', scanConfirmedAt: new Date().toISOString(), completedAt: new Date().toISOString() },
				{ assetId: 'S24-1001-B', status: 'PENDING' },
				{ assetId: 'S24-1001-C', status: 'PENDING' }
			],
			requester: { id: 'u1', display: 'Dr. Smith' },
			assignee: { display: 'Histology Team' },
			dueDate: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
			createdAt: new Date().toISOString(),
			updatedAt: new Date().toISOString()
		},
		{
			id: 'REQ-2024-002',
			actions: ['DISTRIBUTE'],
			priority: 'URGENT',
			status: 'OPEN',
			assets: [{ assetId: 'S24-2001-A', status: 'PENDING' }],
			requester: { id: 'u2', display: 'External Consult', isExternal: true },
			assignee: { display: 'Histology Team' },
			dueDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
			createdAt: new Date().toISOString(),
			updatedAt: new Date().toISOString()
		}
	];

	// Set initial current request
	$effect(() => {
		if (!currentRequest && mockQueue.length > 0) {
			currentRequest = mockQueue[0];
		}
	});

	function getPendingAssets(request: WorkRequest): RequestAssetExecution[] {
		return request.assets.filter((a) => a.status === 'PENDING');
	}

	function getCompletedAssets(request: WorkRequest): RequestAssetExecution[] {
		return request.assets.filter((a) => a.status === 'COMPLETED');
	}

	function handleScanConfirm(result: { outcome: string; query: { normalized: string } }) {
		if (!currentRequest) return;

		const normalized = result.query.normalized;
		const pendingAsset = currentRequest.assets.find(
			(a) => a.status === 'PENDING' && hatStore.normalizeBarcode(a.assetId) === normalized
		);

		if (pendingAsset) {
			// Mark as completed
			pendingAsset.status = 'COMPLETED';
			pendingAsset.scanConfirmedAt = new Date().toISOString();
			pendingAsset.completedAt = new Date().toISOString();

			scanResult = {
				success: true,
				message: `Asset ${pendingAsset.assetId} confirmed`
			};

			// Force reactivity
			currentRequest = { ...currentRequest };

			// Check if all assets are done
			if (getPendingAssets(currentRequest).length === 0) {
				currentRequest.status = 'DONE';
				setTimeout(() => {
					// Move to next request
					const nextRequest = mockQueue.find((r) => r.status !== 'DONE' && r.id !== currentRequest?.id);
					currentRequest = nextRequest ?? null;
					scanResult = null;
				}, 1500);
			} else {
				setTimeout(() => {
					scanResult = null;
				}, 2000);
			}
		} else {
			scanResult = {
				success: false,
				message: `Asset "${normalized}" not found in current request`
			};
			setTimeout(() => {
				scanResult = null;
			}, 3000);
		}
	}

	function selectRequest(request: WorkRequest) {
		currentRequest = request;
		scanResult = null;
	}

	function getProgress(request: WorkRequest): number {
		const completed = request.assets.filter((a) => a.status === 'COMPLETED').length;
		return (completed / request.assets.length) * 100;
	}
</script>

<div class="h-full flex">
	<!-- Main execution area -->
	<div class="flex-1 p-6 overflow-auto">
		{#if currentRequest}
			<div class="max-w-2xl mx-auto">
				<!-- Scan to confirm -->
				<div class="bg-clinical-surface border border-gray-700 rounded-xl p-6 mb-6">
					<h2 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
						Scan to Confirm
					</h2>

					<ScanInput
						onResult={handleScanConfirm}
						placeholder="Scan asset barcode to confirm..."
						autofocus={true}
					/>

					{#if scanResult}
						<div
							class="mt-4 p-4 rounded-lg {scanResult.success
								? 'bg-clinical-success/10 border border-clinical-success/30'
								: 'bg-hat-status-missing/10 border border-hat-status-missing/30'}"
						>
							<div class="flex items-center gap-2">
								{#if scanResult.success}
									<svg
										class="h-5 w-5 text-clinical-success"
										fill="none"
										viewBox="0 0 24 24"
										stroke="currentColor"
									>
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M5 13l4 4L19 7"
										/>
									</svg>
								{:else}
									<svg
										class="h-5 w-5 text-hat-status-missing"
										fill="none"
										viewBox="0 0 24 24"
										stroke="currentColor"
									>
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
										/>
									</svg>
								{/if}
								<span class={scanResult.success ? 'text-clinical-success' : 'text-hat-status-missing'}>
									{scanResult.message}
								</span>
							</div>
						</div>
					{/if}
				</div>

				<!-- Current request details -->
				<div class="bg-clinical-surface border border-gray-700 rounded-xl p-6 mb-6">
					<div class="flex items-center justify-between mb-4">
						<div class="flex items-center gap-3">
							<h3 class="font-mono text-lg text-clinical-text">{currentRequest.id}</h3>
							<PriorityBadge priority={currentRequest.priority} />
						</div>
						<div class="flex flex-wrap gap-2">
							{#each currentRequest.actions as action}
								<span class="text-xs px-2 py-1 rounded bg-clinical-primary/10 text-clinical-primary">
									{ACTION_LABELS[action]}
								</span>
							{/each}
						</div>
					</div>

					<!-- Progress -->
					<div class="mb-4">
						<div class="flex items-center justify-between text-xs text-clinical-muted mb-1">
							<span>
								{getCompletedAssets(currentRequest).length}/{currentRequest.assets.length} completed
							</span>
							<span>{Math.round(getProgress(currentRequest))}%</span>
						</div>
						<div class="h-2 bg-gray-700 rounded-full overflow-hidden">
							<div
								class="h-full bg-clinical-primary rounded-full transition-all"
								style="width: {getProgress(currentRequest)}%"
							></div>
						</div>
					</div>

					<!-- Asset list -->
					<div class="space-y-2">
						{#each currentRequest.assets as asset}
							<div
								class="flex items-center justify-between p-3 rounded-lg {asset.status === 'COMPLETED'
									? 'bg-clinical-success/5 border border-clinical-success/20'
									: asset.status === 'PENDING'
										? 'bg-white/5 border border-gray-700'
										: 'bg-hat-request-in-progress/5 border border-hat-request-in-progress/20'}"
							>
								<div class="flex items-center gap-3">
									<div
										class="h-6 w-6 rounded-full flex items-center justify-center {asset.status === 'COMPLETED'
											? 'bg-clinical-success text-white'
											: 'bg-gray-700 text-clinical-muted'}"
									>
										{#if asset.status === 'COMPLETED'}
											<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
												<path
													stroke-linecap="round"
													stroke-linejoin="round"
													stroke-width="2"
													d="M5 13l4 4L19 7"
												/>
											</svg>
										{:else}
											<span class="text-xs">?</span>
										{/if}
									</div>
									<span
										class="font-mono {asset.status === 'COMPLETED'
											? 'text-clinical-success'
											: 'text-clinical-text'}"
									>
										{asset.assetId}
									</span>
								</div>

								<span
									class="text-xs {asset.status === 'COMPLETED'
										? 'text-clinical-success'
										: 'text-clinical-muted'}"
								>
									{asset.status === 'COMPLETED' ? 'Confirmed' : 'Awaiting scan'}
								</span>
							</div>
						{/each}
					</div>
				</div>
			</div>
		{:else}
			<div class="h-full flex items-center justify-center">
				<div class="text-center">
					<div
						class="mx-auto h-16 w-16 rounded-full bg-clinical-surface flex items-center justify-center mb-4 border border-gray-700"
					>
						<svg class="h-8 w-8 text-clinical-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path
								stroke-linecap="round"
								stroke-linejoin="round"
								stroke-width="1.5"
								d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
							/>
						</svg>
					</div>
					<p class="text-clinical-muted text-lg">Queue is empty</p>
					<p class="text-clinical-muted/70 text-sm mt-1">No pending requests to execute</p>
				</div>
			</div>
		{/if}
	</div>

	<!-- Queue sidebar -->
	<div class="w-80 border-l border-gray-700 bg-clinical-surface p-4 overflow-auto">
		<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
			Queue ({mockQueue.filter((r) => r.status !== 'DONE').length})
		</h3>

		<div class="space-y-2">
			{#each mockQueue as request}
				<button
					onclick={() => selectRequest(request)}
					class="w-full text-left p-3 rounded-lg border transition-colors {currentRequest?.id === request.id
						? 'bg-clinical-primary/10 border-clinical-primary'
						: 'bg-white/5 border-gray-700 hover:bg-white/10'}"
				>
					<div class="flex items-center justify-between mb-1">
						<span class="font-mono text-sm text-clinical-text">{request.id}</span>
						<PriorityBadge priority={request.priority} size="xs" />
					</div>

					<div class="flex flex-wrap gap-1 mb-2">
						{#each request.actions as action}
							<span class="text-[10px] px-1.5 py-0.5 rounded bg-white/10 text-clinical-muted">
								{ACTION_LABELS[action]}
							</span>
						{/each}
					</div>

					<div class="h-1 bg-gray-700 rounded-full overflow-hidden">
						<div
							class="h-full bg-clinical-primary rounded-full"
							style="width: {getProgress(request)}%"
						></div>
					</div>

					<div class="mt-1 text-[10px] text-clinical-muted">
						{getCompletedAssets(request).length}/{request.assets.length} assets
					</div>
				</button>
			{/each}
		</div>
	</div>
</div>
