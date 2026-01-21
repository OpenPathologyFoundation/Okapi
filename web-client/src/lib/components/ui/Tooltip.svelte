<script lang="ts">
	let {
		children,
		content,
		position = 'top',
		delay = 200
	}: {
		children: import('svelte').Snippet;
		content: import('svelte').Snippet;
		position?: 'top' | 'bottom' | 'left' | 'right';
		delay?: number;
	} = $props();

	let visible = $state(false);
	let timeout: ReturnType<typeof setTimeout> | null = null;

	function showTooltip() {
		timeout = setTimeout(() => {
			visible = true;
		}, delay);
	}

	function hideTooltip() {
		if (timeout) {
			clearTimeout(timeout);
			timeout = null;
		}
		visible = false;
	}

	const positionClasses: Record<string, string> = {
		top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
		bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
		left: 'right-full top-1/2 -translate-y-1/2 mr-2',
		right: 'left-full top-1/2 -translate-y-1/2 ml-2'
	};

	const arrowClasses: Record<string, string> = {
		top: 'top-full left-1/2 -translate-x-1/2 border-t-gray-800 dark:border-t-gray-700 border-x-transparent border-b-transparent',
		bottom: 'bottom-full left-1/2 -translate-x-1/2 border-b-gray-800 dark:border-b-gray-700 border-x-transparent border-t-transparent',
		left: 'left-full top-1/2 -translate-y-1/2 border-l-gray-800 dark:border-l-gray-700 border-y-transparent border-r-transparent',
		right: 'right-full top-1/2 -translate-y-1/2 border-r-gray-800 dark:border-r-gray-700 border-y-transparent border-l-transparent'
	};
</script>

<div
	class="relative inline-flex"
	role="tooltip"
	onmouseenter={showTooltip}
	onmouseleave={hideTooltip}
	onfocus={showTooltip}
	onblur={hideTooltip}
>
	{@render children()}

	{#if visible}
		<div
			class="absolute z-50 {positionClasses[position]} pointer-events-none"
			role="tooltip"
		>
			<div class="rounded-lg bg-gray-800 dark:bg-gray-700 px-3 py-2 text-sm text-white shadow-lg whitespace-nowrap">
				{@render content()}
			</div>
			<div
				class="absolute border-4 {arrowClasses[position]}"
			></div>
		</div>
	{/if}
</div>
