<script lang="ts">
	import Tooltip from '$lib/components/ui/Tooltip.svelte';
	import type { CasePriority } from '$lib/types/worklist';
	import { PRIORITY_LABELS, PRIORITY_COLORS } from '$lib/types/worklist';

	let { priority, size = 'sm' }: { priority: CasePriority; size?: 'xs' | 'sm' | 'md' } = $props();

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px] px-1.5 py-0.5',
		sm: 'text-xs px-2 py-0.5',
		md: 'text-sm px-2.5 py-1'
	};

	const priorityDescriptions: Record<CasePriority, string> = {
		STAT: 'Immediate attention required. Results needed urgently for patient care.',
		URGENT: 'High priority case requiring expedited processing.',
		ROUTINE: 'Standard processing timeline applies.'
	};
</script>

<Tooltip position="top">
	{#snippet children()}
		<span
			class="inline-flex cursor-help items-center rounded border font-semibold uppercase tracking-wide {sizeClasses[size]} {PRIORITY_COLORS[priority]}"
		>
			{PRIORITY_LABELS[priority]}
		</span>
	{/snippet}
	{#snippet content()}
		<div class="max-w-xs">
			<div class="font-medium">{PRIORITY_LABELS[priority]} Priority</div>
			<div class="mt-1 text-xs text-gray-300">{priorityDescriptions[priority]}</div>
		</div>
	{/snippet}
</Tooltip>
