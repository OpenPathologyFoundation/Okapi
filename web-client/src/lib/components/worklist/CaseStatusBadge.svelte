<script lang="ts">
	import type { CaseStatus } from '$lib/types/worklist';
	import { STATUS_LABELS, STATUS_COLORS } from '$lib/types/worklist';

	let { status, size = 'sm' }: { status: CaseStatus; size?: 'xs' | 'sm' | 'md' } = $props();

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px] px-1.5 py-0.5',
		sm: 'text-xs px-2 py-0.5',
		md: 'text-sm px-2.5 py-1'
	};
</script>

<span
	class="inline-flex items-center gap-1.5 rounded-full border font-medium {sizeClasses[size]} {STATUS_COLORS[status]}"
>
	{STATUS_LABELS[status]}
</span>
