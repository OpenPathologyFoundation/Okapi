<script lang="ts">
	import { goto } from '$app/navigation';
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import { SERVICE_LABELS } from '$lib/types/worklist';
	import type { WorklistSortField, WorklistItem } from '$lib/types/worklist';
	import CaseStatusBadge from './CaseStatusBadge.svelte';
	import PriorityBadge from './PriorityBadge.svelte';
	import SlideSummary from './SlideSummary.svelte';
	import CaseAge from './CaseAge.svelte';
	import Tooltip from '$lib/components/ui/Tooltip.svelte';

	let { onCaseClick = (_accession: string) => {} }: { onCaseClick?: (accession: string) => void } =
		$props();

	function handleSort(field: WorklistSortField) {
		worklistStore.setSort(field);
	}

	function getSortIcon(field: WorklistSortField): string {
		if (worklistStore.sort.field !== field) return '↕';
		return worklistStore.sort.direction === 'asc' ? '↑' : '↓';
	}

	function getSortClass(field: WorklistSortField): string {
		return worklistStore.sort.field === field ? 'text-clinical-primary' : 'text-clinical-muted';
	}

	function openCase(accession: string, event: MouseEvent) {
		event.stopPropagation();
		goto(`/app/case/${accession}`);
	}

	function launchViewer(accession: string, event: MouseEvent) {
		event.stopPropagation();
		// TODO: Open viewer in new window or modal
		console.log('Launch viewer for:', accession);
		window.open(`/app/viewer/${accession}`, '_blank');
	}

	function handleRowClick(item: WorklistItem) {
		onCaseClick(item.accessionNumber);
		goto(`/app/case/${item.accessionNumber}`);
	}
</script>

<div class="overflow-x-auto">
	<table class="w-full text-left text-sm">
		<thead class="border-b border-clinical-border text-xs uppercase tracking-wide text-clinical-muted">
			<tr>
				<th class="px-4 py-3">
					<button
						type="button"
						onclick={() => handleSort('accessionNumber')}
						class="flex items-center gap-1 hover:text-clinical-text"
					>
						Accession
						<span class="{getSortClass('accessionNumber')}">{getSortIcon('accessionNumber')}</span>
					</button>
				</th>
				<th class="px-4 py-3">
					<button
						type="button"
						onclick={() => handleSort('patientDisplay')}
						class="flex items-center gap-1 hover:text-clinical-text"
					>
						Patient
						<span class="{getSortClass('patientDisplay')}">{getSortIcon('patientDisplay')}</span>
					</button>
				</th>
				<th class="px-4 py-3">
					<button
						type="button"
						onclick={() => handleSort('service')}
						class="flex items-center gap-1 hover:text-clinical-text"
					>
						Service
						<span class="{getSortClass('service')}">{getSortIcon('service')}</span>
					</button>
				</th>
				<th class="px-4 py-3">Specimen</th>
				<th class="px-4 py-3">
					<button
						type="button"
						onclick={() => handleSort('status')}
						class="flex items-center gap-1 hover:text-clinical-text"
					>
						Status
						<span class="{getSortClass('status')}">{getSortIcon('status')}</span>
					</button>
				</th>
				<th class="px-4 py-3">
					<button
						type="button"
						onclick={() => handleSort('priority')}
						class="flex items-center gap-1 hover:text-clinical-text"
					>
						Priority
						<span class="{getSortClass('priority')}">{getSortIcon('priority')}</span>
					</button>
				</th>
				<th class="px-4 py-3">Slides</th>
				<th class="px-4 py-3">Assigned To</th>
				<th class="px-4 py-3">
					<button
						type="button"
						onclick={() => handleSort('caseDate')}
						class="flex items-center gap-1 hover:text-clinical-text"
					>
						Age
						<span class="{getSortClass('caseDate')}">{getSortIcon('caseDate')}</span>
					</button>
				</th>
				<th class="px-4 py-3 text-right">Actions</th>
			</tr>
		</thead>
		<tbody class="divide-y divide-clinical-border-subtle">
			{#each worklistStore.sortedItems as item (item.id)}
				<tr
					class="group cursor-pointer transition-colors hover:bg-clinical-hover"
					onclick={() => handleRowClick(item)}
				>
					<td class="px-4 py-3">
						<a
							href="/app/case/{item.accessionNumber}"
							class="font-mono font-medium text-clinical-primary hover:underline"
							onclick={(e) => e.stopPropagation()}
						>
							{item.accessionNumber}
						</a>
					</td>
					<td class="px-4 py-3">
						<div class="flex flex-col">
							<span class="text-clinical-text">{item.patientDisplay ?? '—'}</span>
							{#if item.patientMrn}
								<span class="text-xs text-clinical-muted">{item.patientMrn}</span>
							{/if}
						</div>
					</td>
					<td class="px-4 py-3 text-clinical-text">
						{SERVICE_LABELS[item.service]}
					</td>
					<td class="px-4 py-3">
						<div class="flex flex-col">
							<span class="text-clinical-text">{item.specimenType ?? '—'}</span>
							{#if item.specimenSite}
								<span class="text-xs text-clinical-muted">{item.specimenSite}</span>
							{/if}
						</div>
					</td>
					<td class="px-4 py-3">
						<CaseStatusBadge status={item.status} size="xs" />
					</td>
					<td class="px-4 py-3">
						<PriorityBadge priority={item.priority} size="xs" />
					</td>
					<td class="px-4 py-3">
						<SlideSummary
							scanned={item.slideScanned}
							total={item.slideCount}
							pending={item.slidePending}
							size="sm"
						/>
					</td>
					<td class="px-4 py-3 text-clinical-text">
						{item.assignedToDisplay ?? '—'}
					</td>
					<td class="px-4 py-3">
						<CaseAge
							caseDate={item.caseDate}
							receivedAt={item.receivedAt}
							collectedAt={item.collectedAt}
							size="sm"
						/>
					</td>
					<td class="px-4 py-3">
						<div class="flex items-center justify-end gap-1 opacity-0 transition-opacity group-hover:opacity-100">
							<Tooltip position="top">
								{#snippet children()}
									<button
										type="button"
										onclick={(e) => openCase(item.accessionNumber, e)}
										class="rounded p-1.5 text-clinical-muted hover:bg-clinical-primary/10 hover:text-clinical-primary"
										aria-label="Open case"
									>
										<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
											<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
										</svg>
									</button>
								{/snippet}
								{#snippet content()}Open Case{/snippet}
							</Tooltip>
							<Tooltip position="top">
								{#snippet children()}
									<button
										type="button"
										onclick={(e) => launchViewer(item.accessionNumber, e)}
										class="rounded p-1.5 text-clinical-muted hover:bg-clinical-primary/10 hover:text-clinical-primary"
										disabled={item.slideScanned === 0}
										aria-label={item.slideScanned === 0 ? 'No slides scanned' : 'Launch viewer'}
									>
										<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
											<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
											<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
										</svg>
									</button>
								{/snippet}
								{#snippet content()}{item.slideScanned === 0 ? 'No slides scanned' : 'Launch Viewer'}{/snippet}
							</Tooltip>
						</div>
					</td>
				</tr>
			{:else}
				<tr>
					<td colspan="10" class="px-4 py-12 text-center text-clinical-muted">
						{#if worklistStore.isLoading}
							<div class="flex items-center justify-center gap-2">
								<div
									class="h-4 w-4 animate-spin rounded-full border-2 border-clinical-muted border-t-clinical-primary"
								></div>
								Loading worklist...
							</div>
						{:else}
							No cases found matching the current filters.
						{/if}
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>

<!-- Case count -->
<div class="mt-4 flex items-center justify-between border-t border-clinical-border px-4 pt-4 text-sm">
	<span class="text-clinical-muted">
		Showing {worklistStore.sortedItems.length} of {worklistStore.items.length} cases
	</span>
	{#if worklistStore.counts}
		<span class="text-clinical-muted">
			Total: {worklistStore.counts.total} | My Cases: {worklistStore.counts.myCases}
		</span>
	{/if}
</div>
