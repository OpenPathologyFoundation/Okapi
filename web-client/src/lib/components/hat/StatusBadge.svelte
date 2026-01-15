<script lang="ts">
	import type { AssetStatus } from '$lib/types/hat';
	import { STATUS_LABELS } from '$lib/types/hat';

	let { status, size = 'sm' }: { status: AssetStatus; size?: 'xs' | 'sm' | 'md' } = $props();

	const colorMap: Record<AssetStatus, string> = {
		AVAILABLE: 'bg-hat-status-available',
		CHECKED_OUT: 'bg-hat-status-checked-out',
		IN_TRANSIT: 'bg-hat-status-in-transit',
		IN_QA: 'bg-hat-status-in-qa',
		DISTRIBUTED: 'bg-hat-status-distributed',
		RETURNED: 'bg-hat-status-returned',
		DESTROYED: 'bg-hat-status-destroyed',
		MISSING: 'bg-hat-status-missing'
	};

	const textColorMap: Record<AssetStatus, string> = {
		AVAILABLE: 'text-hat-status-available',
		CHECKED_OUT: 'text-hat-status-checked-out',
		IN_TRANSIT: 'text-hat-status-in-transit',
		IN_QA: 'text-hat-status-in-qa',
		DISTRIBUTED: 'text-hat-status-distributed',
		RETURNED: 'text-hat-status-returned',
		DESTROYED: 'text-hat-status-destroyed',
		MISSING: 'text-hat-status-missing'
	};

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px] px-1.5 py-0.5',
		sm: 'text-xs px-2 py-0.5',
		md: 'text-sm px-2.5 py-1'
	};
</script>

<span
	class="inline-flex items-center gap-1.5 rounded-full font-medium {sizeClasses[size]} {textColorMap[status]} bg-current/10"
>
	<span class="h-1.5 w-1.5 rounded-full {colorMap[status]}"></span>
	{STATUS_LABELS[status]}
</span>
