<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/state';
	import { activityStore } from '$lib/stores/activity.svelte';
	import { authStore } from '$lib/stores/auth.svelte';
	import ModuleHost from '$lib/components/ModuleHost.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

	const accession = $derived(page.params.accession);
	const viewerActivity = $derived(activityStore.getActivity('pelican-viewer'));
	const viewerState = $derived(viewerActivity?.status ?? 'idle');
	const viewerCurrentCase = $derived(viewerActivity?.accession ?? null);
	const viewerSlideCount = $derived((viewerActivity?.moduleState.slideCount as number | undefined) ?? null);
	const viewerLastError = $derived(viewerActivity?.lastError ?? null);
	const viewerIsOpen = $derived(viewerState === 'connected' || viewerState === 'launching');
	const reportActivity = $derived(activityStore.getActivity('report-module'));
	const reportIsVisible = $derived(Boolean(reportActivity?.visible));

	/** Whether to show the case-switch confirmation dialog */
	let showCaseSwitchDialog = $state(false);

	/** Whether the viewer is actively showing this case */
	const isViewerActiveForCase = $derived(
		viewerIsOpen && viewerCurrentCase === accession
	);

	/** Whether the viewer is open for a different case */
	const isViewerOpenOtherCase = $derived(
		viewerIsOpen && viewerCurrentCase !== accession
	);

	/** Handle "Launch Viewer" button click */
	async function handleLaunchViewer(): Promise<void> {
		const acc = accession;
		console.log('[CasePage] handleLaunchViewer — acc=' + acc + ', isViewerActiveForCase=' + isViewerActiveForCase + ', isViewerOpenOtherCase=' + isViewerOpenOtherCase + ', storeState=' + viewerState);
		if (!acc) return;

		if (isViewerActiveForCase) {
			// Already showing this case — no-op
			return;
		}

		if (isViewerOpenOtherCase) {
			// Viewer open for a different case — confirm before switching
			showCaseSwitchDialog = true;
			return;
		}

		// Launch new viewer window
		await activityStore.launchActivity('pelican-viewer', {
			caseId: acc,
			accession: acc,
			tileServerUrl: '/tiles',
			sessionServiceUrl: '/ws',
			userId: authStore.user?.identityId ?? 'unknown',
		});
	}

	async function handleLaunchReport(): Promise<void> {
		const acc = accession;
		if (!acc) return;

		await activityStore.launchActivity('report-module', {
			caseId: acc,
			accession: acc,
			userId: authStore.user?.identityId ?? 'unknown',
			role: authStore.roles[0] ?? 'AUTHOR',
		});
	}

	/** Button label depends on viewer state */
	const viewerButtonLabel = $derived.by(() => {
		if (viewerState === 'launching') return 'Opening Viewer...';
		if (isViewerActiveForCase) return 'Viewer Active';
		if (isViewerOpenOtherCase) return 'Switch in Viewer';
		return 'Launch Viewer';
	});

	/** Button disabled when launching or already active for this case */
	const viewerButtonDisabled = $derived(viewerState === 'launching' || isViewerActiveForCase);

	function confirmCaseSwitch(): void {
		showCaseSwitchDialog = false;
		const acc = accession;
		if (acc) {
			activityStore.sendCaseChange('pelican-viewer', acc, acc);
		}
	}

	function cancelCaseSwitch(): void {
		showCaseSwitchDialog = false;
	}

	onMount(() => {
		if (page.url.searchParams.get('launch') === 'true') {
			// Clean URL to prevent re-launch on refresh
			const cleanUrl = new URL(page.url);
			cleanUrl.searchParams.delete('launch');
			history.replaceState({}, '', cleanUrl.pathname);
			handleLaunchViewer();
		}
	});
</script>

<div class="flex h-full flex-col bg-clinical-bg">
	<!-- Case Header -->
	<header class="shrink-0 border-b border-clinical-border bg-clinical-surface px-6 py-4">
		<div class="flex items-center justify-between">
			<div class="flex items-center gap-4">
				<a
					href="/app/worklist"
					class="flex items-center gap-1 text-sm text-clinical-muted hover:text-clinical-primary"
				>
					<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
					</svg>
					Back to Worklist
				</a>
				<div class="h-6 w-px bg-clinical-border"></div>
				<h1 class="text-xl font-semibold text-clinical-text">
					Case: <span class="font-mono">{accession}</span>
				</h1>

				{#if isViewerActiveForCase}
					<span class="inline-flex items-center gap-1.5 rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-400">
						<span class="h-1.5 w-1.5 rounded-full bg-green-400"></span>
						Viewer Active
						{#if viewerSlideCount !== null}
							<span class="text-green-500/60">&middot; {viewerSlideCount} slides</span>
						{/if}
					</span>
				{/if}
			</div>

			<div class="flex items-center gap-3">
				<button
					type="button"
					class="flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium transition-colors {isViewerActiveForCase
						? 'border-green-500/30 bg-green-500/10 text-green-400'
						: 'border-clinical-border bg-clinical-surface text-clinical-text hover:bg-clinical-hover'}"
					disabled={viewerButtonDisabled}
					onclick={handleLaunchViewer}
				>
					{#if viewerState === 'launching'}
						<svg class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
							<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
							<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
						</svg>
					{:else}
						<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
						</svg>
					{/if}
					{viewerButtonLabel}
				</button>
				<button
					type="button"
					class="flex items-center gap-2 rounded-md bg-clinical-primary px-4 py-2 text-sm font-medium text-white hover:bg-clinical-primary/90"
					onclick={handleLaunchReport}
				>
					<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
					</svg>
					{reportIsVisible ? 'Report Open' : 'Edit Report'}
				</button>
			</div>
		</div>
	</header>

	<!-- Viewer error toast -->
	{#if viewerLastError}
		<div class="mx-6 mt-4 rounded-md border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400">
			<span class="font-medium">Viewer error:</span> {viewerLastError.message}
			<span class="text-red-500/60">({viewerLastError.code})</span>
		</div>
	{/if}

	<!-- Main Content Area -->
	<main class="flex-1 overflow-auto p-6">
		<div class="mx-auto grid max-w-7xl gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(22rem,0.9fr)]">
			<div class="min-w-0">
			<!-- Placeholder content -->
			<div class="rounded-lg border border-clinical-border bg-clinical-surface p-8 text-center">
				<svg class="mx-auto h-16 w-16 text-clinical-muted opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
				</svg>
				<h2 class="mt-4 text-lg font-medium text-clinical-text">Case View Coming Soon</h2>
				<p class="mt-2 text-sm text-clinical-muted">
					This page will display case details, patient information, specimen data,
					report authoring tools, and integration with the digital slide viewer.
				</p>
				<p class="mt-4 text-xs text-clinical-muted">
					Accession Number: <span class="font-mono font-medium text-clinical-text">{accession}</span>
				</p>
			</div>

			<!-- Feature preview cards -->
				<div class="mt-8 grid grid-cols-1 gap-4 md:grid-cols-3">
				<div class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
					<h3 class="flex items-center gap-2 font-medium text-clinical-text">
						<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
						</svg>
						Patient Info
					</h3>
					<p class="mt-2 text-sm text-clinical-muted">Demographics, clinical history, and prior cases</p>
				</div>
				<div class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
					<h3 class="flex items-center gap-2 font-medium text-clinical-text">
						<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
						</svg>
						Specimens
					</h3>
					<p class="mt-2 text-sm text-clinical-muted">Specimen details, blocks, and slides</p>
				</div>
				<div class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
					<h3 class="flex items-center gap-2 font-medium text-clinical-text">
						<svg class="h-5 w-5 text-clinical-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
						</svg>
						Report Authoring
					</h3>
					<p class="mt-2 text-sm text-clinical-muted">Draft, review, and sign-out workflow</p>
				</div>
			</div>
			</div>

			<div class="min-h-0">
				<ModuleHost activityId="report-module" className="sticky top-0" />
			</div>
		</div>
	</main>
</div>

<ConfirmDialog
	open={showCaseSwitchDialog}
	title="Switch Viewer Case"
	message="The viewer is currently showing case {viewerCurrentCase}. Switch to {accession}?"
	confirmLabel="Switch Case"
	variant="warning"
	onconfirm={confirmCaseSwitch}
	oncancel={cancelCaseSwitch}
/>
