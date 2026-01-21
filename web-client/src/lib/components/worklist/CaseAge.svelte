<script lang="ts">
	import Tooltip from '$lib/components/ui/Tooltip.svelte';
	import { getCaseAgeDays, formatCaseAge } from '$lib/types/worklist';

	let {
		caseDate,
		receivedAt = null,
		collectedAt = null,
		size = 'sm'
	}: {
		caseDate: string;
		receivedAt?: string | null;
		collectedAt?: string | null;
		size?: 'xs' | 'sm' | 'md';
	} = $props();

	const days = $derived(getCaseAgeDays(caseDate));
	const ageDisplay = $derived(formatCaseAge(caseDate));

	// Color coding based on days (configurable thresholds per spec §6.3)
	// Default: green 0-2, yellow 3-4, orange 5-6, red 7+
	const ageColorClass = $derived.by(() => {
		if (days <= 2) return 'text-emerald-600 dark:text-emerald-400';
		if (days <= 4) return 'text-amber-500 dark:text-amber-400';
		if (days <= 6) return 'text-orange-500 dark:text-orange-400';
		return 'text-red-600 dark:text-red-400';
	});

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px]',
		sm: 'text-xs',
		md: 'text-sm'
	};

	function formatDate(dateStr: string | null): string {
		if (!dateStr) return '—';
		const date = new Date(dateStr);
		return date.toLocaleDateString('en-US', {
			weekday: 'short',
			month: 'short',
			day: 'numeric',
			year: 'numeric'
		});
	}

	function formatDateTime(dateStr: string | null): string {
		if (!dateStr) return '—';
		const date = new Date(dateStr);
		return date.toLocaleString('en-US', {
			weekday: 'short',
			month: 'short',
			day: 'numeric',
			year: 'numeric',
			hour: 'numeric',
			minute: '2-digit'
		});
	}
</script>

<Tooltip position="top">
	{#snippet children()}
		<span class="cursor-help {sizeClasses[size]} {ageColorClass}">
			{ageDisplay}
		</span>
	{/snippet}
	{#snippet content()}
		<div class="space-y-1">
			<div class="font-medium">Case Age: {days} day{days !== 1 ? 's' : ''}</div>
			<div class="text-xs text-gray-300 space-y-0.5">
				<div>Accessioned: {formatDateTime(caseDate)}</div>
				{#if receivedAt}
					<div>Received: {formatDateTime(receivedAt)}</div>
				{/if}
				{#if collectedAt}
					<div>Collected: {formatDateTime(collectedAt)}</div>
				{/if}
			</div>
		</div>
	{/snippet}
</Tooltip>
