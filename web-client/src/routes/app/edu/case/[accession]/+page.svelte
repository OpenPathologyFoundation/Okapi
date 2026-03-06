<script lang="ts">
	import { page } from '$app/state';
	import { eduStore } from '$lib/stores/edu.svelte';
	import { viewerStore } from '$lib/stores/viewer.svelte';
	import { authStore } from '$lib/stores/auth.svelte';
	import {
		DIFFICULTY_LABELS,
		DIFFICULTY_COLORS,
		STATUS_LABELS,
		STATUS_COLORS,
		CATEGORY_LABELS,
		ROLE_LABELS
	} from '$lib/types/edu';
	import type { DifficultyLevel, EduCaseStatus, EduCuratorRole } from '$lib/types/edu';

	const accession = $derived(page.params.accession);

	$effect(() => {
		if (accession) {
			eduStore.loadCaseDetail(accession);
		}
	});

	async function handleLaunchViewer(slideId: string): Promise<void> {
		const acc = accession ?? '';
		await viewerStore.launchViewer({
			caseId: acc,
			accession: acc,
			viewerUrl: '/viewer/orchestrated.html',
			viewerOrigin: window.location.origin,
			tileServerUrl: '/tiles',
			sessionServiceUrl: '/ws',
			userId: authStore.user?.identityId ?? 'unknown',
			mode: 'educational'
		});
	}
</script>

<div class="p-6">
	{#if eduStore.isLoading}
		<div class="flex items-center justify-center py-20">
			<div class="h-8 w-8 animate-spin rounded-full border-2 border-clinical-primary border-t-transparent"></div>
		</div>
	{:else if eduStore.selectedCase}
		{@const detail = eduStore.selectedCase}

		<!-- Back Link & Header -->
		<div class="mb-6">
			<a
				href="/app/edu"
				class="mb-2 inline-flex items-center gap-1 text-xs text-clinical-muted hover:text-clinical-primary"
			>
				<svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
				</svg>
				Back to Browse
			</a>

			<div class="flex items-start justify-between">
				<div>
					<h2 class="text-xl font-semibold text-clinical-text">{detail.caseId}</h2>
					<p class="mt-1 text-sm text-clinical-muted">{detail.specimenType ?? 'Unknown specimen'}</p>
				</div>
				<div class="flex items-center gap-2">
					{#if detail.difficultyLevel}
						<span class="rounded-full border px-2.5 py-1 text-xs font-medium {DIFFICULTY_COLORS[detail.difficultyLevel]}">
							{DIFFICULTY_LABELS[detail.difficultyLevel]}
						</span>
					{/if}
					{#if detail.status}
						<span class="rounded-full border px-2.5 py-1 text-xs font-medium {STATUS_COLORS[detail.status]}">
							{STATUS_LABELS[detail.status]}
						</span>
					{/if}
				</div>
			</div>
		</div>

		<div class="grid gap-6 lg:grid-cols-3">
			<!-- Left Column: Clinical Info + Hierarchy -->
			<div class="lg:col-span-2 space-y-6">
				<!-- Clinical History -->
				{#if detail.clinicalHistory}
					<section class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
						<h3 class="mb-2 text-sm font-semibold text-clinical-text">Clinical History</h3>
						<p class="text-sm text-clinical-text">{detail.clinicalHistory}</p>
					</section>
				{/if}

				<!-- Specimen Hierarchy -->
				<section class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
					<h3 class="mb-3 text-sm font-semibold text-clinical-text">Specimen Hierarchy</h3>
					{#each detail.parts as part}
						<div class="mb-3 rounded-md border border-clinical-border-subtle bg-clinical-bg p-3">
							<div class="mb-1 flex items-center gap-2">
								<span class="rounded bg-clinical-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-clinical-primary">
									Part {part.partLabel}
								</span>
								{#if part.anatomicSite}
									<span class="text-xs text-clinical-muted">{part.anatomicSite}</span>
								{/if}
								{#if part.provenance && part.provenance !== 'ACCESSIONED'}
									<span class="text-[10px] text-clinical-muted italic">({part.provenance})</span>
								{/if}
							</div>
							{#if part.finalDiagnosis}
								<p class="mb-2 text-xs font-medium text-clinical-text">{part.finalDiagnosis}</p>
							{/if}

							{#each part.blocks as block}
								<div class="ml-4 mb-2 rounded border border-clinical-border-subtle bg-clinical-surface p-2">
									<p class="mb-1 text-[10px] font-medium text-clinical-muted">
										Block {block.blockLabel}
										{#if block.blockDescription}
											<span class="font-normal"> — {block.blockDescription}</span>
										{/if}
									</p>
									<div class="ml-2 flex flex-col gap-1">
										{#each block.slides as slide}
											<div class="flex items-center justify-between rounded bg-clinical-bg px-2 py-1">
												<div class="flex items-center gap-2">
													<svg class="h-3 w-3 text-clinical-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
														<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
															d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
													</svg>
													<span class="text-[10px] text-clinical-text">{slide.slideId}</span>
													{#if slide.stain}
														<span class="rounded bg-clinical-primary/5 px-1 py-0.5 text-[10px] text-clinical-muted">{slide.stain}</span>
													{/if}
												</div>
												<button
													type="button"
													class="rounded px-2 py-0.5 text-[10px] font-medium text-clinical-primary hover:bg-clinical-primary/10"
													onclick={() => handleLaunchViewer(slide.slideId)}
												>
													View
												</button>
											</div>
										{/each}
									</div>
								</div>
							{/each}
						</div>
					{/each}
				</section>
			</div>

			<!-- Right Column: Metadata -->
			<div class="space-y-6">
				<!-- Teaching Info -->
				<section class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
					<h3 class="mb-3 text-sm font-semibold text-clinical-text">Teaching Info</h3>
					<dl class="space-y-2 text-xs">
						{#if detail.teachingCategory}
							<div>
								<dt class="text-clinical-muted">Category</dt>
								<dd class="font-medium text-clinical-text">
									{CATEGORY_LABELS[detail.teachingCategory] ?? detail.teachingCategory}
								</dd>
							</div>
						{/if}
						<div>
							<dt class="text-clinical-muted">Slides</dt>
							<dd class="font-medium text-clinical-text">{detail.slideCount}</dd>
						</div>
						{#if detail.curriculumTags && detail.curriculumTags.length > 0}
							<div>
								<dt class="mb-1 text-clinical-muted">Curriculum Tags</dt>
								<dd class="flex flex-wrap gap-1">
									{#each detail.curriculumTags as tag}
										<span class="rounded bg-clinical-bg px-1.5 py-0.5 text-[10px] text-clinical-muted">{tag}</span>
									{/each}
								</dd>
							</div>
						{/if}
					</dl>
				</section>

				<!-- ICD Codes -->
				{#if detail.icdCodes && detail.icdCodes.length > 0}
					<section class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
						<h3 class="mb-3 text-sm font-semibold text-clinical-text">ICD Codes</h3>
						<div class="space-y-2">
							{#each detail.icdCodes as icd}
								<div class="rounded bg-clinical-bg px-2 py-1.5">
									<p class="text-xs font-medium text-clinical-text">{icd.icdCode}</p>
									<p class="text-[10px] text-clinical-muted">
										{icd.codeSystem} — {icd.codeDescription ?? ''}
									</p>
								</div>
							{/each}
						</div>
					</section>
				{/if}

				<!-- Curators -->
				{#if detail.curators && detail.curators.length > 0}
					<section class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
						<h3 class="mb-3 text-sm font-semibold text-clinical-text">Curators</h3>
						<div class="space-y-2">
							{#each detail.curators as curator}
								<div class="flex items-center justify-between rounded bg-clinical-bg px-2 py-1.5">
									<span class="text-xs text-clinical-text">{curator.identityDisplay ?? curator.identityId}</span>
									<span class="rounded bg-clinical-primary/10 px-1.5 py-0.5 text-[10px] font-medium text-clinical-primary">
										{ROLE_LABELS[curator.role as EduCuratorRole] ?? curator.role}
									</span>
								</div>
							{/each}
						</div>
					</section>
				{/if}

				<!-- Source Lineage -->
				{#if detail.sourceLineage && Object.keys(detail.sourceLineage).length > 0}
					<section class="rounded-lg border border-clinical-border bg-clinical-surface p-4">
						<h3 class="mb-3 text-sm font-semibold text-clinical-text">Source Lineage</h3>
						<dl class="space-y-1 text-xs">
							{#each Object.entries(detail.sourceLineage) as [key, value]}
								{#if typeof value === 'string'}
									<div class="flex justify-between">
										<dt class="text-clinical-muted">{key}</dt>
										<dd class="text-clinical-text">{value}</dd>
									</div>
								{/if}
							{/each}
						</dl>
					</section>
				{/if}
			</div>
		</div>
	{:else}
		<div class="py-20 text-center">
			<p class="text-sm text-clinical-muted">Case not found</p>
			<a href="/app/edu" class="mt-2 inline-block text-sm text-clinical-primary hover:underline">
				Back to Browse
			</a>
		</div>
	{/if}
</div>
