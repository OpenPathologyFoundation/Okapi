<script lang="ts">
	let {
		scanned,
		total,
		size = 'sm'
	}: { scanned: number; total: number; size?: 'xs' | 'sm' | 'md' } = $props();

	const allScanned = $derived(scanned === total && total > 0);
	const noneScanned = $derived(scanned === 0 && total > 0);
	const noSlides = $derived(total === 0);

	const sizeClasses: Record<string, string> = {
		xs: 'text-[10px]',
		sm: 'text-xs',
		md: 'text-sm'
	};
</script>

{#if noSlides}
	<span class="text-clinical-muted {sizeClasses[size]}">—</span>
{:else}
	<span
		class="{sizeClasses[size]} {allScanned
			? 'text-emerald-600 dark:text-emerald-400'
			: noneScanned
				? 'text-clinical-muted'
				: 'text-amber-600 dark:text-amber-400'}"
	>
		{scanned}/{total}
	</span>
{/if}
