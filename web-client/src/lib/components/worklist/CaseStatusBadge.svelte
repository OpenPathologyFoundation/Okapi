<script lang="ts">
	import Tooltip from '$lib/components/ui/Tooltip.svelte';
	import type { CaseStatus } from '$lib/types/worklist';
	import { STATUS_LABELS, STATUS_COLORS } from '$lib/types/worklist';

	let {
		status,
		size = 'sm',
		authoringContext = null
	}: {
		status: CaseStatus;
		size?: 'xs' | 'sm' | 'md';
		authoringContext?: {
			draftBy?: string;
			draftDate?: string;
			reviewingBy?: string;
			amendedBy?: string;
		} | null;
	} = $props();

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px] px-1.5 py-0.5',
		sm: 'text-xs px-2 py-0.5',
		md: 'text-sm px-2.5 py-1'
	};

	const hasContext = $derived(authoringContext !== null);

	function getTooltipContent(): string {
		if (!authoringContext) return STATUS_LABELS[status];

		switch (status) {
			case 'UNDER_REVIEW':
				if (authoringContext.draftBy && authoringContext.draftDate) {
					return `Completed by ${authoringContext.draftBy} on ${authoringContext.draftDate}, awaiting attending review`;
				}
				return 'Draft submitted, awaiting attending review';
			case 'PENDING_SIGNOUT':
				if (authoringContext.draftBy) {
					return `Being edited by ${authoringContext.draftBy}`;
				}
				return 'Case is ready for sign-out';
			case 'AMENDED':
				if (authoringContext.amendedBy) {
					return `Amendment initiated by ${authoringContext.amendedBy}`;
				}
				return 'Amendment pending';
			default:
				return STATUS_LABELS[status];
		}
	}
</script>

{#if hasContext}
	<Tooltip position="top">
		{#snippet children()}
			<span
				class="inline-flex items-center gap-1.5 rounded-full border font-medium cursor-help {sizeClasses[size]} {STATUS_COLORS[status]}"
			>
				{STATUS_LABELS[status]}
			</span>
		{/snippet}
		{#snippet content()}
			<div class="max-w-xs">
				<div class="font-medium">{STATUS_LABELS[status]}</div>
				<div class="text-xs text-gray-300 mt-1">{getTooltipContent()}</div>
			</div>
		{/snippet}
	</Tooltip>
{:else}
	<span
		class="inline-flex items-center gap-1.5 rounded-full border font-medium {sizeClasses[size]} {STATUS_COLORS[status]}"
	>
		{STATUS_LABELS[status]}
	</span>
{/if}
