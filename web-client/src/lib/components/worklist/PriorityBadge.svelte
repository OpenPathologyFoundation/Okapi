<script lang="ts">
	import type { CasePriority } from '$lib/types/worklist';
	import { PRIORITY_LABELS, PRIORITY_COLORS } from '$lib/types/worklist';

	let { priority, size = 'sm' }: { priority: CasePriority; size?: 'xs' | 'sm' | 'md' } = $props();

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px] px-1.5 py-0.5',
		sm: 'text-xs px-2 py-0.5',
		md: 'text-sm px-2.5 py-1'
	};
</script>

<span
	class="inline-flex items-center rounded border font-semibold uppercase tracking-wide {sizeClasses[size]} {PRIORITY_COLORS[priority]}"
>
	{PRIORITY_LABELS[priority]}
</span>
