<script lang="ts">
	import type { RequestPriority } from '$lib/types/hat';
	import { PRIORITY_LABELS } from '$lib/types/hat';

	let { priority, size = 'sm' }: { priority: RequestPriority; size?: 'xs' | 'sm' | 'md' } = $props();

	const colorMap: Record<RequestPriority, string> = {
		STAT: 'bg-hat-priority-stat text-white',
		URGENT: 'bg-hat-priority-urgent text-black',
		ROUTINE: 'bg-hat-priority-routine/20 text-hat-priority-routine'
	};

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px] px-1.5 py-0.5',
		sm: 'text-xs px-2 py-0.5',
		md: 'text-sm px-2.5 py-1'
	};
</script>

<span
	class="inline-flex items-center rounded font-bold uppercase tracking-wide {sizeClasses[size]} {colorMap[priority]}"
>
	{PRIORITY_LABELS[priority]}
</span>
