<script lang="ts">
	import { ACTIVITY_REGISTRY } from '$lib/activity-registry';
	import { activityStore } from '$lib/stores/activity.svelte';

	let { activityId, className = '' }: { activityId: string; className?: string } = $props();

	let iframeEl = $state<HTMLIFrameElement | null>(null);

	const definition = $derived(ACTIVITY_REGISTRY[activityId]);
	const activity = $derived(activityStore.getActivity(activityId));
	const visible = $derived(Boolean(activity?.visible && definition?.mount?.strategy === 'iframe'));
	const statusLabel = $derived(activity?.status ?? 'idle');
	const isDirty = $derived(Boolean(activity?.moduleState.isDirty));

	$effect(() => {
		if (!visible || !iframeEl?.contentWindow) return;
		activityStore.attachIframeTarget(activityId, iframeEl.contentWindow);
	});

	function closeModule(): void {
		activityStore.closeActivity(activityId);
	}
</script>

{#if visible && definition}
	<section class={`flex h-full min-h-[28rem] flex-col overflow-hidden rounded-xl border border-clinical-border bg-clinical-surface shadow-sm ${className}`}>
		<header class="flex items-center justify-between border-b border-clinical-border-subtle bg-clinical-bg/70 px-4 py-3">
			<div>
				<p class="text-sm font-semibold text-clinical-text">{definition.label}</p>
				<p class="text-xs text-clinical-muted">
					Status: <span class="font-medium text-clinical-text">{statusLabel}</span>
					{#if isDirty}
						<span class="ml-2 rounded bg-amber-500/10 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-amber-500">Unsaved</span>
					{/if}
				</p>
			</div>
			<button
				type="button"
				class="rounded-md border border-clinical-border px-2.5 py-1 text-xs font-medium text-clinical-muted transition-colors hover:bg-clinical-hover hover:text-clinical-text"
				onclick={closeModule}
			>
				Close
			</button>
		</header>

		<iframe
			bind:this={iframeEl}
			title={definition.label}
			src={definition.path}
			class="h-full min-h-0 w-full flex-1 border-0 bg-white"
			loading="eager"
		></iframe>
	</section>
{/if}
