<script lang="ts">
	import Tooltip from '$lib/components/ui/Tooltip.svelte';

	let {
		scanned,
		total,
		pending = 0,
		qcStatus = null,
		size = 'sm'
	}: {
		scanned: number;
		total: number;
		pending?: number;
		qcStatus?: 'PASSED' | 'FAILED' | 'PENDING' | null;
		size?: 'xs' | 'sm' | 'md';
	} = $props();

	const allScanned = $derived(scanned === total && total > 0);
	const noneScanned = $derived(scanned === 0 && total > 0);
	const noSlides = $derived(total === 0);
	const hasQcIssue = $derived(qcStatus === 'FAILED');

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px]',
		sm: 'text-xs',
		md: 'text-sm'
	};

	function getStatusText(): string {
		if (allScanned && qcStatus === 'PASSED') return 'All slides digitized & verified';
		if (allScanned && qcStatus === 'PENDING') return 'All slides digitized, QC pending';
		if (allScanned && qcStatus === 'FAILED') return 'All slides digitized, QC issues detected';
		if (allScanned) return 'All slides digitized';
		if (noneScanned) return 'No slides digitized yet';
		return `${scanned} of ${total} slides digitized`;
	}
</script>

{#if noSlides}
	<span class="text-clinical-muted {sizeClasses[size]}">—</span>
{:else}
	<Tooltip position="top">
		{#snippet children()}
			<span
				class="inline-flex items-center gap-1 cursor-help {sizeClasses[size]} {allScanned
					? 'text-emerald-600 dark:text-emerald-400'
					: noneScanned
						? 'text-clinical-muted'
						: 'text-amber-600 dark:text-amber-400'}"
			>
				{scanned}/{total}
				{#if allScanned && qcStatus === 'PASSED'}
					<svg class="h-3 w-3 text-emerald-600 dark:text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
					</svg>
				{:else if hasQcIssue}
					<svg class="h-3 w-3 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
					</svg>
				{/if}
			</span>
		{/snippet}
		{#snippet content()}
			<div class="space-y-1">
				<div class="font-medium">{getStatusText()}</div>
				<div class="text-xs text-gray-300 space-y-0.5">
					<div>Total slides: {total}</div>
					<div>Digitized: {scanned}</div>
					{#if pending > 0}
						<div>Pending scan: {pending}</div>
					{/if}
					{#if qcStatus}
						<div class="flex items-center gap-1">
							QC Status:
							<span class="{qcStatus === 'PASSED' ? 'text-emerald-400' : qcStatus === 'FAILED' ? 'text-red-400' : 'text-amber-400'}">
								{qcStatus === 'PASSED' ? 'Verified' : qcStatus === 'FAILED' ? 'Issues Found' : 'Pending'}
							</span>
						</div>
					{/if}
				</div>
			</div>
		{/snippet}
	</Tooltip>
{/if}
