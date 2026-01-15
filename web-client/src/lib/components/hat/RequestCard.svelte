<script lang="ts">
	import type { WorkRequest } from '$lib/types/hat';
	import { ACTION_LABELS, REQUEST_STATUS_LABELS } from '$lib/types/hat';
	import PriorityBadge from './PriorityBadge.svelte';

	let {
		request,
		onClick = undefined
	}: {
		request: WorkRequest;
		onClick?: () => void;
	} = $props();

	const statusColorMap: Record<string, string> = {
		OPEN: 'text-hat-request-open',
		IN_PROGRESS: 'text-hat-request-in-progress',
		DONE: 'text-hat-request-done',
		CANCELLED: 'text-hat-request-cancelled'
	};

	const statusBgMap: Record<string, string> = {
		OPEN: 'bg-hat-request-open/10',
		IN_PROGRESS: 'bg-hat-request-in-progress/10',
		DONE: 'bg-hat-request-done/10',
		CANCELLED: 'bg-hat-request-cancelled/10'
	};

	function getProgress(): { completed: number; total: number } {
		const total = request.assets.length;
		const completed = request.assets.filter((a) => a.status === 'COMPLETED').length;
		return { completed, total };
	}

	function formatDueDate(): string | null {
		if (!request.dueDate) return null;
		const due = new Date(request.dueDate);
		const now = new Date();
		const diff = due.getTime() - now.getTime();
		const hours = Math.floor(diff / (1000 * 60 * 60));
		const days = Math.floor(diff / (1000 * 60 * 60 * 24));

		if (diff < 0) {
			return 'Overdue';
		} else if (hours < 24) {
			return `Due in ${hours}h`;
		} else if (days === 1) {
			return 'Due tomorrow';
		} else {
			return `Due in ${days} days`;
		}
	}

	function isOverdue(): boolean {
		if (!request.dueDate) return false;
		return new Date(request.dueDate).getTime() < Date.now();
	}

	const progress = $derived(getProgress());
	const dueText = $derived(formatDueDate());
	const overdue = $derived(isOverdue());
</script>

<div
	class="bg-clinical-surface border border-gray-700 rounded-lg p-4 transition-colors {onClick
		? 'hover:bg-white/5 cursor-pointer'
		: ''}"
	onclick={onClick}
	onkeydown={(e) => e.key === 'Enter' && onClick?.()}
	role={onClick ? 'button' : undefined}
	tabindex={onClick ? 0 : undefined}
>
	<div class="flex items-start justify-between gap-4">
		<div class="flex-1 min-w-0">
			<!-- Header row -->
			<div class="flex items-center gap-2 mb-2">
				<span class="font-mono text-sm text-clinical-muted">{request.id}</span>
				<PriorityBadge priority={request.priority} size="xs" />
				<span
					class="text-xs font-medium px-2 py-0.5 rounded {statusBgMap[request.status]} {statusColorMap[request.status]}"
				>
					{REQUEST_STATUS_LABELS[request.status]}
				</span>
			</div>

			<!-- Actions -->
			<div class="flex flex-wrap gap-1.5 mb-2">
				{#each request.actions as action}
					<span class="text-xs px-2 py-0.5 rounded bg-white/5 text-clinical-text">
						{ACTION_LABELS[action]}
					</span>
				{/each}
			</div>

			<!-- Progress bar -->
			{#if progress.total > 0}
				<div class="mb-2">
					<div class="flex items-center justify-between text-xs text-clinical-muted mb-1">
						<span>{progress.completed}/{progress.total} assets</span>
						<span>{Math.round((progress.completed / progress.total) * 100)}%</span>
					</div>
					<div class="h-1.5 bg-gray-700 rounded-full overflow-hidden">
						<div
							class="h-full bg-clinical-primary rounded-full transition-all"
							style="width: {(progress.completed / progress.total) * 100}%"
						></div>
					</div>
				</div>
			{/if}

			<!-- Footer info -->
			<div class="flex items-center gap-4 text-xs text-clinical-muted">
				{#if request.requester}
					<span>From: {request.requester.display}</span>
				{/if}
				{#if request.assignee}
					<span>Assigned: {request.assignee.display}</span>
				{/if}
				{#if dueText}
					<span class={overdue ? 'text-hat-status-missing font-medium' : ''}>
						{dueText}
					</span>
				{/if}
			</div>
		</div>

		{#if onClick}
			<svg
				class="h-5 w-5 text-clinical-muted shrink-0"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M9 5l7 7-7 7"
				/>
			</svg>
		{/if}
	</div>
</div>
