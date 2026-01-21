<script lang="ts">
	import type { AssetEvent, AssetEventType } from '$lib/types/hat';

	let {
		events,
		onEventClick = (_event: AssetEvent) => {}
	}: {
		events: AssetEvent[];
		onEventClick?: (event: AssetEvent) => void;
	} = $props();

	const eventTypeConfig: Record<AssetEventType, { icon: string; color: string; label: string }> = {
		CREATED: { icon: 'plus', color: 'text-clinical-success', label: 'Created' },
		STATUS_CHANGED: { icon: 'refresh', color: 'text-hat-status-in-transit', label: 'Status Changed' },
		LOCATION_CHANGED: { icon: 'location', color: 'text-clinical-primary', label: 'Location Changed' },
		CUSTODY_CHANGED: { icon: 'user', color: 'text-hat-status-checked-out', label: 'Custody Changed' },
		IDENTIFIER_ADDED: { icon: 'tag', color: 'text-clinical-muted', label: 'Identifier Added' },
		CORRECTION: { icon: 'edit', color: 'text-hat-priority-urgent', label: 'Correction' },
		RECONCILED: { icon: 'check', color: 'text-clinical-success', label: 'Reconciled' },
		REQUEST_LINKED: { icon: 'link', color: 'text-clinical-primary', label: 'Request Linked' },
		MILESTONE_REACHED: { icon: 'flag', color: 'text-hat-status-distributed', label: 'Milestone' }
	};

	function formatDate(dateStr: string): string {
		const date = new Date(dateStr);
		const now = new Date();
		const diff = now.getTime() - date.getTime();
		const days = Math.floor(diff / (1000 * 60 * 60 * 24));

		if (days === 0) {
			return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
		} else if (days === 1) {
			return 'Yesterday ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
		} else if (days < 7) {
			return date.toLocaleDateString([], { weekday: 'short' }) + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
		} else {
			return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
		}
	}

	function getEventDescription(event: AssetEvent): string {
		const payload = event.payload as Record<string, unknown>;
		switch (event.type) {
			case 'STATUS_CHANGED':
				return `Status changed from ${payload.from ?? 'unknown'} to ${payload.to ?? 'unknown'}`;
			case 'LOCATION_CHANGED':
				return `Moved to ${payload.location ?? 'unknown location'}`;
			case 'CUSTODY_CHANGED':
				if (payload.to) return `Custody transferred to ${payload.to}`;
				return 'Custody released';
			case 'CORRECTION':
				return `Correction: ${payload.reason ?? event.comment ?? 'No reason provided'}`;
			case 'RECONCILED':
				return `Reconciled with ${payload.source ?? 'source system'}`;
			case 'REQUEST_LINKED':
				return `Linked to request ${event.linkedRequestId ?? 'unknown'}`;
			case 'MILESTONE_REACHED':
				return `${payload.milestone ?? 'Milestone reached'}`;
			default:
				return event.comment ?? eventTypeConfig[event.type].label;
		}
	}
</script>

<div class="relative">
	<!-- Timeline line -->
	<div class="absolute left-4 top-0 bottom-0 w-0.5 bg-clinical-border"></div>

	<div class="space-y-4">
		{#each events as event, index}
			<div
				class="relative pl-10 group"
				role="button"
				tabindex="0"
				onclick={() => onEventClick(event)}
				onkeydown={(e) => e.key === 'Enter' && onEventClick(event)}
			>
				<!-- Timeline dot -->
				<div
					class="absolute left-2 top-1.5 w-4 h-4 rounded-full border-2 border-clinical-border bg-clinical-surface flex items-center justify-center {eventTypeConfig[event.type].color}"
				>
					{#if event.type === 'CORRECTION'}
						<div class="w-2 h-2 rounded-full bg-hat-priority-urgent"></div>
					{:else if event.type === 'RECONCILED'}
						<div class="w-2 h-2 rounded-full bg-clinical-success"></div>
					{:else}
						<div class="w-1.5 h-1.5 rounded-full bg-current"></div>
					{/if}
				</div>

				<!-- Event card -->
				<div
					class="bg-clinical-surface border border-clinical-border rounded-lg p-4 hover:bg-clinical-hover transition-colors cursor-pointer {event.type === 'CORRECTION' ? 'border-hat-priority-urgent/30' : ''}"
				>
					<div class="flex items-start justify-between gap-2">
						<div class="flex-1 min-w-0">
							<div class="flex items-center gap-2 mb-1">
								<span
									class="text-xs font-medium uppercase tracking-wider {eventTypeConfig[event.type].color}"
								>
									{eventTypeConfig[event.type].label}
								</span>
								{#if event.supersedes}
									<span class="text-[10px] px-1.5 py-0.5 rounded bg-hat-priority-urgent/20 text-hat-priority-urgent">
										Supersedes
									</span>
								{/if}
							</div>

							<p class="text-sm text-clinical-text">
								{getEventDescription(event)}
							</p>

							{#if event.comment && event.type !== 'CORRECTION'}
								<p class="text-xs text-clinical-muted mt-1 italic">
									"{event.comment}"
								</p>
							{/if}

							<div class="flex items-center gap-3 mt-2 text-xs text-clinical-muted">
								<span>{event.actor.display}</span>
								{#if event.actor.role}
									<span class="text-clinical-muted/70">({event.actor.role})</span>
								{/if}
							</div>
						</div>

						<span class="text-xs text-clinical-muted whitespace-nowrap">
							{formatDate(event.timestamp)}
						</span>
					</div>

					{#if event.linkedRequestId}
						<div class="mt-2 pt-2 border-t border-clinical-border">
							<a
								href="/app/hat/requests/{event.linkedRequestId}"
								class="text-xs text-clinical-primary hover:underline"
								onclick={(e) => e.stopPropagation()}
							>
								View Request {event.linkedRequestId}
							</a>
						</div>
					{/if}
				</div>
			</div>
		{/each}
	</div>

	{#if events.length === 0}
		<div class="text-center py-8">
			<p class="text-clinical-muted">No events recorded</p>
		</div>
	{/if}
</div>
