<script lang="ts">
	import { createStarlingModule } from '@starling/module-sdk';
	import { onDestroy, onMount } from 'svelte';
	import type { ReportInitPayload } from '$lib/types/report-bridge';

	let client = $state<ReturnType<typeof createStarlingModule<ReportInitPayload>> | null>(null);
	let token = $state<string | null>(null);
	let caseId = $state('');
	let accession = $state('');
	let role = $state<string | null>(null);
	let blurred = $state(false);
	let loggedOut = $state(false);
	let dirty = $state(false);
	let status = $state<'draft' | 'saved' | 'finalized'>('draft');

	function applyContext(payload: Pick<ReportInitPayload, 'caseId' | 'accession' | 'role' | 'token'>): void {
		caseId = payload.caseId;
		accession = payload.accession;
		role = payload.role ?? null;
		token = payload.token;
		loggedOut = false;
		dirty = false;
		status = 'draft';
	}

	onMount(() => {
		client = createStarlingModule<ReportInitPayload>({
			expectedOrigin: window.location.origin,
			onInit: (payload) => {
				applyContext(payload);
				client?.auditEvent({
					eventType: 'REPORT_OPENED',
					occurredAt: new Date().toISOString(),
					metadata: { accession: payload.accession },
				});
			},
			onTokenRefresh: (payload) => {
				token = payload.token;
			},
			onFocus: (payload) => {
				blurred = payload.state === 'blurred';
			},
			onContextUpdate: (payload) => {
				if (payload.key === 'case' && payload.value && typeof payload.value === 'object') {
					const value = payload.value as { caseId?: string; accession?: string; role?: string };
					applyContext({
						caseId: value.caseId ?? caseId,
						accession: value.accession ?? accession,
						role: value.role ?? role ?? undefined,
						token: token ?? '',
					});
				}
			},
			onLogout: () => {
				loggedOut = true;
			},
		});

		const handler = (event: MessageEvent) => client?.handleMessage(event as MessageEvent<any>);
		window.addEventListener('message', handler);
		client.ready();

		return () => {
			window.removeEventListener('message', handler);
		};
	});

	onDestroy(() => {
		client = null;
	});

	function markDirty(): void {
		dirty = true;
		status = 'draft';
		client?.updateState('isDirty', true);
		client?.updateState('status', status);
	}

	function saveDraft(): void {
		dirty = false;
		status = 'saved';
		client?.updateState('isDirty', false);
		client?.updateState('status', status);
		client?.auditEvent({
			eventType: 'REPORT_SAVED',
			occurredAt: new Date().toISOString(),
			metadata: { accession, status },
		});
	}

	function finalizeReport(): void {
		dirty = false;
		status = 'finalized';
		client?.updateState('isDirty', false);
		client?.updateState('status', status);
		client?.auditEvent({
			eventType: 'REPORT_FINALIZED',
			occurredAt: new Date().toISOString(),
			metadata: { accession },
		});
		client?.emit('report:finalized', { accession });
	}
</script>

<div class="relative flex h-screen flex-col overflow-hidden bg-[linear-gradient(135deg,rgba(250,248,244,1)_0%,rgba(241,236,229,1)_100%)] text-slate-900">
	<div class="border-b border-black/10 bg-white/70 px-6 py-4 backdrop-blur-sm">
		<div class="flex items-start justify-between gap-4">
			<div>
				<p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">Embedded Module Demo</p>
				<h1 class="mt-1 text-2xl font-semibold tracking-tight">Report Authoring</h1>
				<p class="mt-2 text-sm text-slate-600">
					Case <span class="font-mono text-slate-900">{accession || 'Awaiting context'}</span>
					{#if role}
						<span class="ml-2 rounded-full bg-slate-900 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-white">{role}</span>
					{/if}
				</p>
			</div>
			<div class="rounded-xl border border-black/10 bg-white px-3 py-2 text-xs text-slate-600 shadow-sm">
				<p>JWT: {token ? 'present' : 'missing'}</p>
				<p>Status: <span class="font-semibold text-slate-900">{status}</span></p>
			</div>
		</div>
	</div>

	<div class="grid flex-1 gap-6 overflow-auto p-6 lg:grid-cols-[1.4fr_0.9fr]">
		<section class="rounded-2xl border border-black/10 bg-white p-5 shadow-sm">
			<label for="report-impression" class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Diagnostic Impression</label>
			<textarea
				id="report-impression"
				class="mt-3 h-64 w-full rounded-2xl border border-black/10 bg-slate-50 px-4 py-3 text-sm outline-none ring-0 transition focus:border-slate-400"
				placeholder="Summarize the case..."
				oninput={markDirty}
			></textarea>

			<div class="mt-4 flex flex-wrap gap-3">
				<button type="button" class="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white" onclick={saveDraft}>Save Draft</button>
				<button type="button" class="rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700" onclick={finalizeReport}>Finalize</button>
			</div>
		</section>

		<aside class="space-y-4">
			<section class="rounded-2xl border border-black/10 bg-white p-5 shadow-sm">
				<p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Module State</p>
				<div class="mt-4 space-y-3 text-sm text-slate-700">
					<p><span class="font-semibold text-slate-900">Case ID:</span> {caseId || 'pending'}</p>
					<p><span class="font-semibold text-slate-900">Accession:</span> {accession || 'pending'}</p>
					<p><span class="font-semibold text-slate-900">Unsaved:</span> {dirty ? 'yes' : 'no'}</p>
					<p><span class="font-semibold text-slate-900">Focus:</span> {blurred ? 'blurred' : 'active'}</p>
				</div>
			</section>

			<section class="rounded-2xl border border-black/10 bg-white p-5 shadow-sm">
				<p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Bridge Notes</p>
				<ul class="mt-4 space-y-2 text-sm text-slate-600">
					<li>Uses `module:ready` from the shared SDK.</li>
					<li>Receives `orchestrator:init` and `orchestrator:context-update`.</li>
					<li>Publishes `module:state-update` and `report:finalized`.</li>
				</ul>
			</section>
		</aside>
	</div>

	{#if blurred}
		<div class="pointer-events-none absolute inset-0 bg-slate-900/12 backdrop-blur-[1px]"></div>
	{/if}

	{#if loggedOut}
		<div class="absolute inset-x-10 bottom-8 rounded-2xl border border-amber-300 bg-amber-50 px-5 py-4 text-sm text-amber-900 shadow-lg">
			The orchestrator session ended. This module is still open, but it should be considered read-only until relaunched.
		</div>
	{/if}
</div>
