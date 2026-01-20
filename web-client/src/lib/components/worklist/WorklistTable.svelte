<script lang="ts">
	import { worklistStore } from '$lib/stores/worklist.svelte';
	import { SERVICE_LABELS, formatCaseAge } from '$lib/types/worklist';
	import type { WorklistSortField } from '$lib/types/worklist';
	import CaseStatusBadge from './CaseStatusBadge.svelte';
	import PriorityBadge from './PriorityBadge.svelte';
	import SlideSummary from './SlideSummary.svelte';

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
			</tr>
		</thead>
		<tbody class="divide-y divide-clinical-border-subtle">
			{#each worklistStore.sortedItems as item (item.id)}
				<tr
					class="cursor-pointer transition-colors hover:bg-clinical-hover"
					onclick={() => onCaseClick(item.accessionNumber)}
				>
					<td class="px-4 py-3">
						<span class="font-mono font-medium text-clinical-text">{item.accessionNumber}</span>
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
						<SlideSummary scanned={item.slideScanned} total={item.slideCount} size="sm" />
					</td>
					<td class="px-4 py-3 text-clinical-text">
						{item.assignedToDisplay ?? '—'}
					</td>
					<td class="px-4 py-3 text-clinical-muted">
						{formatCaseAge(item.caseDate)}
					</td>
				</tr>
			{:else}
				<tr>
					<td colspan="9" class="px-4 py-12 text-center text-clinical-muted">
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
