<script lang="ts">
	import { page } from '$app/state';
	import type { WorkRequest } from '$lib/types/hat';
	import { ACTION_LABELS, REQUEST_STATUS_LABELS } from '$lib/types/hat';
	import PriorityBadge from '$lib/components/hat/PriorityBadge.svelte';
	import StatusBadge from '$lib/components/hat/StatusBadge.svelte';

	const requestId = $derived(page.params.id ?? '');

	// Mock request data
	let request = $state<WorkRequest>({
		id: 'REQ-2024-001',
		actions: ['PULL', 'SCAN'],
		priority: 'STAT',
		status: 'IN_PROGRESS',
		assets: [
			{
				assetId: 'ASSET-001',
				status: 'COMPLETED',
				scanConfirmedAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
				completedAt: new Date(Date.now() - 25 * 60 * 1000).toISOString()
			},
			{
				assetId: 'ASSET-002',
				status: 'IN_PROGRESS',
				scanConfirmedAt: new Date(Date.now() - 10 * 60 * 1000).toISOString()
			},
			{ assetId: 'ASSET-003', status: 'PENDING' }
		],
		requester: { id: 'u1', display: 'Dr. Smith' },
		assignee: { display: 'Histology Team' },
		dueDate: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
		createdAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
		updatedAt: new Date().toISOString()
	});

	const statusColorMap: Record<string, string> = {
		OPEN: 'bg-hat-request-open',
		IN_PROGRESS: 'bg-hat-request-in-progress',
		DONE: 'bg-hat-request-done',
		CANCELLED: 'bg-hat-request-cancelled'
	};

	const assetStatusConfig: Record<string, { color: string; label: string }> = {
		PENDING: { color: 'text-clinical-muted', label: 'Pending' },
		IN_PROGRESS: { color: 'text-hat-request-in-progress', label: 'In Progress' },
		COMPLETED: { color: 'text-clinical-success', label: 'Completed' },
		SKIPPED: { color: 'text-clinical-muted', label: 'Skipped' },
		MISSING: { color: 'text-hat-status-missing', label: 'Missing' },
		SUBSTITUTED: { color: 'text-hat-status-in-transit', label: 'Substituted' }
	};

	function formatDate(dateStr: string): string {
		return new Date(dateStr).toLocaleString();
	}

	function getProgress(): { completed: number; total: number } {
		const total = request.assets.length;
		const completed = request.assets.filter((a) => a.status === 'COMPLETED').length;
		return { completed, total };
	}

	const progress = $derived(getProgress());
</script>

<div class="h-full p-6 overflow-auto">
	<div class="max-w-4xl mx-auto">
		<!-- Back navigation -->
		<div class="mb-6">
			<a
				href="/app/hat/requests"
				class="inline-flex items-center gap-2 text-sm text-clinical-muted hover:text-clinical-text transition-colors"
			>
				<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
				</svg>
				Back to Requests
			</a>
		</div>

		<!-- Header -->
		<div class="bg-clinical-surface border border-clinical-border rounded-xl p-6 mb-6">
			<div class="flex items-start justify-between">
				<div>
					<div class="flex items-center gap-3 mb-2">
						<h2 class="text-xl font-mono font-semibold text-clinical-text">{request.id}</h2>
						<PriorityBadge priority={request.priority} />
						<span
							class="text-xs font-medium px-2.5 py-1 rounded text-white {statusColorMap[request.status]}"
						>
							{REQUEST_STATUS_LABELS[request.status]}
						</span>
					</div>

					<div class="flex flex-wrap gap-2 mb-4">
						{#each request.actions as action}
							<span class="text-sm px-3 py-1 rounded bg-clinical-hover text-clinical-text">
								{ACTION_LABELS[action]}
							</span>
						{/each}
					</div>

					<div class="flex items-center gap-6 text-sm text-clinical-muted">
						<div>
							<span class="text-clinical-muted/70">Requester:</span>
							<span class="text-clinical-text ml-1">{request.requester.display}</span>
						</div>
						{#if request.assignee}
							<div>
								<span class="text-clinical-muted/70">Assigned:</span>
								<span class="text-clinical-text ml-1">{request.assignee.display}</span>
							</div>
						{/if}
						{#if request.dueDate}
							<div>
								<span class="text-clinical-muted/70">Due:</span>
								<span class="text-clinical-text ml-1">{formatDate(request.dueDate)}</span>
							</div>
						{/if}
					</div>
				</div>

				<!-- Actions -->
				{#if request.status === 'OPEN' || request.status === 'IN_PROGRESS'}
					<div class="flex gap-2">
						<button
							class="px-4 py-2 text-sm font-medium text-white bg-clinical-primary hover:bg-clinical-primary/90 rounded-lg transition-colors"
						>
							Execute
						</button>
						<button
							class="px-4 py-2 text-sm font-medium text-clinical-text bg-clinical-hover hover:bg-clinical-border/50 border border-clinical-border rounded-lg transition-colors"
						>
							Edit
						</button>
					</div>
				{/if}
			</div>
		</div>

		<!-- Progress -->
		<div class="bg-clinical-surface border border-clinical-border rounded-xl p-6 mb-6">
			<div class="flex items-center justify-between mb-2">
				<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider">Progress</h3>
				<span class="text-sm text-clinical-text">
					{progress.completed}/{progress.total} assets completed
				</span>
			</div>
			<div class="h-2 bg-clinical-border rounded-full overflow-hidden">
				<div
					class="h-full bg-clinical-primary rounded-full transition-all"
					style="width: {(progress.completed / progress.total) * 100}%"
				></div>
			</div>
		</div>

		<!-- Assets -->
		<div class="bg-clinical-surface border border-clinical-border rounded-xl p-6">
			<h3 class="text-sm font-medium text-clinical-muted uppercase tracking-wider mb-4">
				Assets ({request.assets.length})
			</h3>

			<div class="space-y-3">
				{#each request.assets as assetExec, index}
					<div
						class="flex items-center justify-between p-4 bg-clinical-bg border border-clinical-border rounded-lg"
					>
						<div class="flex items-center gap-4">
							<div
								class="h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold {assetExec.status === 'COMPLETED'
									? 'bg-clinical-success/20 text-clinical-success'
									: assetExec.status === 'IN_PROGRESS'
										? 'bg-hat-request-in-progress/20 text-hat-request-in-progress'
										: 'bg-clinical-border text-clinical-muted'}"
							>
								{#if assetExec.status === 'COMPLETED'}
									<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
									</svg>
								{:else}
									{index + 1}
								{/if}
							</div>

							<div>
								<a
									href="/app/hat/asset/{assetExec.assetId}"
									class="font-mono text-clinical-text hover:text-clinical-primary transition-colors"
								>
									{assetExec.assetId}
								</a>
								<p class="text-xs {assetStatusConfig[assetExec.status].color}">
									{assetStatusConfig[assetExec.status].label}
								</p>
							</div>
						</div>

						<div class="text-right text-xs text-clinical-muted">
							{#if assetExec.scanConfirmedAt}
								<p>Scanned: {formatDate(assetExec.scanConfirmedAt)}</p>
							{/if}
							{#if assetExec.completedAt}
								<p>Completed: {formatDate(assetExec.completedAt)}</p>
							{/if}
						</div>
					</div>
				{/each}
			</div>
		</div>

		<!-- Timestamps -->
		<div class="mt-6 text-xs text-clinical-muted flex items-center gap-6">
			<span>Created: {formatDate(request.createdAt)}</span>
			<span>Updated: {formatDate(request.updatedAt)}</span>
		</div>
	</div>
</div>
