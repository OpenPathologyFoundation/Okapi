<script lang="ts">
	import { goto } from '$app/navigation';
	import type { WorkRequest, RequestStatus, RequestPriority } from '$lib/types/hat';
	import RequestCard from '$lib/components/hat/RequestCard.svelte';

	let statusFilter = $state<RequestStatus | 'ALL'>('ALL');
	let priorityFilter = $state<RequestPriority | 'ALL'>('ALL');
	let assigneeFilter = $state<'MY' | 'TEAM' | 'ALL'>('ALL');

	// Mock data for demo
	const mockRequests: WorkRequest[] = [
		{
			id: 'REQ-2024-001',
			actions: ['PULL', 'SCAN'],
			priority: 'STAT',
			status: 'IN_PROGRESS',
			assets: [
				{ assetId: 'A1', status: 'COMPLETED' },
				{ assetId: 'A2', status: 'IN_PROGRESS' },
				{ assetId: 'A3', status: 'PENDING' }
			],
			requester: { id: 'u1', display: 'Dr. Smith' },
			assignee: { display: 'Histology Team' },
			dueDate: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
			createdAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
			updatedAt: new Date().toISOString()
		},
		{
			id: 'REQ-2024-002',
			actions: ['DISTRIBUTE'],
			priority: 'URGENT',
			status: 'OPEN',
			assets: [{ assetId: 'B1', status: 'PENDING' }],
			requester: { id: 'u2', display: 'External Consult', isExternal: true },
			assignee: { display: 'Dr. Martinez' },
			dueDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
			createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
			updatedAt: new Date().toISOString()
		},
		{
			id: 'REQ-2024-003',
			actions: ['STAIN', 'QA'],
			priority: 'ROUTINE',
			status: 'DONE',
			assets: [
				{ assetId: 'C1', status: 'COMPLETED' },
				{ assetId: 'C2', status: 'COMPLETED' }
			],
			requester: { id: 'u3', display: 'Dr. Johnson' },
			assignee: { display: 'Lab Tech A' },
			createdAt: new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString(),
			updatedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
		},
		{
			id: 'REQ-2024-004',
			actions: ['PULL', 'RETURN'],
			priority: 'ROUTINE',
			status: 'CANCELLED',
			assets: [{ assetId: 'D1', status: 'SKIPPED' }],
			requester: { id: 'u1', display: 'Dr. Smith' },
			createdAt: new Date(Date.now() - 72 * 60 * 60 * 1000).toISOString(),
			updatedAt: new Date(Date.now() - 70 * 60 * 60 * 1000).toISOString()
		}
	];

	let filteredRequests = $derived(
		mockRequests.filter((r) => {
			if (statusFilter !== 'ALL' && r.status !== statusFilter) return false;
			if (priorityFilter !== 'ALL' && r.priority !== priorityFilter) return false;
			return true;
		})
	);

	function handleRequestClick(request: WorkRequest) {
		goto(`/app/hat/requests/${request.id}`);
	}
</script>

<div class="h-full p-6 overflow-auto">
	<div class="max-w-4xl mx-auto">
		<!-- Header -->
		<div class="flex items-center justify-between mb-6">
			<h2 class="text-xl font-semibold text-clinical-text">Requests</h2>
			<a
				href="/app/hat/requests/new"
				class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-clinical-primary hover:bg-clinical-primary/90 rounded-lg transition-colors"
			>
				<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
				</svg>
				New Request
			</a>
		</div>

		<!-- Filters -->
		<div class="flex flex-wrap gap-4 mb-6">
			<!-- Status filter -->
			<div class="flex items-center gap-2">
				<span class="text-xs text-clinical-muted uppercase tracking-wider">Status:</span>
				<div class="flex gap-1">
					{#each ['ALL', 'OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'] as status}
						<button
							onclick={() => (statusFilter = status as RequestStatus | 'ALL')}
							class="px-3 py-1 text-xs font-medium rounded transition-colors {statusFilter === status
								? 'bg-clinical-primary text-white'
								: 'bg-clinical-hover text-clinical-muted hover:bg-clinical-border/50'}"
						>
							{status === 'ALL' ? 'All' : status.replace('_', ' ')}
						</button>
					{/each}
				</div>
			</div>

			<!-- Priority filter -->
			<div class="flex items-center gap-2">
				<span class="text-xs text-clinical-muted uppercase tracking-wider">Priority:</span>
				<div class="flex gap-1">
					{#each ['ALL', 'STAT', 'URGENT', 'ROUTINE'] as priority}
						<button
							onclick={() => (priorityFilter = priority as RequestPriority | 'ALL')}
							class="px-3 py-1 text-xs font-medium rounded transition-colors {priorityFilter === priority
								? 'bg-clinical-primary text-white'
								: 'bg-clinical-hover text-clinical-muted hover:bg-clinical-border/50'}"
						>
							{priority === 'ALL' ? 'All' : priority}
						</button>
					{/each}
				</div>
			</div>
		</div>

		<!-- Request list -->
		<div class="space-y-3">
			{#each filteredRequests as request}
				<RequestCard {request} onClick={() => handleRequestClick(request)} />
			{/each}

			{#if filteredRequests.length === 0}
				<div class="text-center py-12">
					<div
						class="mx-auto h-16 w-16 rounded-full bg-clinical-surface flex items-center justify-center mb-4 border border-clinical-border"
					>
						<svg class="h-8 w-8 text-clinical-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path
								stroke-linecap="round"
								stroke-linejoin="round"
								stroke-width="1.5"
								d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
							/>
						</svg>
					</div>
					<p class="text-clinical-muted">No requests match your filters</p>
				</div>
			{/if}
		</div>
	</div>
</div>
