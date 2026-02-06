<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";

    let eventType = $state("");
    let outcome = $state("");
    let targetType = $state("");
    let expandedId = $state<string | null>(null);

    onMount(() => {
        adminStore.loadAuditEvents();
    });

    function handleFilter() {
        adminStore.loadAuditEvents(
            {
                eventType: eventType || undefined,
                outcome: outcome || undefined,
                targetType: targetType || undefined,
            },
            0,
            20,
        );
    }

    function goToPage(p: number) {
        adminStore.loadAuditEvents(
            {
                eventType: eventType || undefined,
                outcome: outcome || undefined,
                targetType: targetType || undefined,
            },
            p,
            20,
        );
    }

    function toggleExpand(id: string) {
        expandedId = expandedId === id ? null : id;
    }

    function formatDate(iso: string): string {
        return new Date(iso).toLocaleString("en-US", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
        });
    }

    function outcomeColor(o: string): string {
        if (o === "SUCCESS") return "bg-green-500/10 text-green-500";
        if (o === "DENY") return "bg-red-500/10 text-red-500";
        return "bg-amber-500/10 text-amber-500";
    }
</script>

<div class="space-y-4">
    <!-- Filter Bar -->
    <div class="flex items-center gap-3 flex-wrap">
        <input
            type="text"
            bind:value={eventType}
            onkeydown={(e) => e.key === "Enter" && handleFilter()}
            placeholder="Event Type..."
            class="rounded-md border-0 bg-clinical-bg py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary w-48"
        />
        <select
            bind:value={outcome}
            onchange={handleFilter}
            class="rounded-md border-0 bg-clinical-bg py-1.5 pl-3 pr-8 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary"
        >
            <option value="">All Outcomes</option>
            <option value="SUCCESS">Success</option>
            <option value="DENY">Deny</option>
        </select>
        <input
            type="text"
            bind:value={targetType}
            onkeydown={(e) => e.key === "Enter" && handleFilter()}
            placeholder="Target Type..."
            class="rounded-md border-0 bg-clinical-bg py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary w-40"
        />
        <button
            onclick={handleFilter}
            class="px-3 py-1.5 rounded-md bg-clinical-primary text-white text-xs font-medium hover:bg-clinical-primary/90 transition-colors"
        >
            Filter
        </button>
    </div>

    <!-- Events Table -->
    {#if adminStore.isLoading && adminStore.auditEvents.length === 0}
        <div class="flex items-center justify-center py-12">
            <div
                class="h-6 w-6 animate-spin rounded-full border-4 border-clinical-primary border-t-transparent"
            ></div>
        </div>
    {:else if adminStore.error}
        <div
            class="rounded-md bg-red-500/10 border border-red-500/30 p-4 text-sm text-red-500"
        >
            {adminStore.error}
        </div>
    {:else}
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg overflow-hidden"
        >
            <table class="w-full text-xs">
                <thead>
                    <tr
                        class="border-b border-clinical-border bg-clinical-bg/50"
                    >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Time</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Event Type</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Actor</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Target</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Outcome</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Details</th
                        >
                    </tr>
                </thead>
                <tbody>
                    {#each adminStore.auditEvents as event}
                        <tr
                            class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors cursor-pointer"
                            onclick={() => toggleExpand(event.eventId)}
                        >
                            <td
                                class="px-3 py-2 text-clinical-muted whitespace-nowrap"
                                >{formatDate(event.occurredAt)}</td
                            >
                            <td
                                class="px-3 py-2 font-mono text-clinical-text"
                                >{event.eventType}</td
                            >
                            <td class="px-3 py-2 text-clinical-muted"
                                >{event.actorExternalSubject ?? "—"}</td
                            >
                            <td class="px-3 py-2 text-clinical-muted"
                                >{event.targetEntityType ?? "—"}</td
                            >
                            <td class="px-3 py-2">
                                <span
                                    class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold {outcomeColor(
                                        event.outcome,
                                    )}"
                                >
                                    {event.outcome}
                                </span>
                            </td>
                            <td
                                class="px-3 py-2 text-clinical-muted truncate max-w-[200px]"
                                >{event.details ?? "—"}</td
                            >
                        </tr>
                        {#if expandedId === event.eventId}
                            <tr class="bg-clinical-bg/50">
                                <td colspan="6" class="px-4 py-3">
                                    <div
                                        class="text-xs space-y-1 font-mono text-clinical-muted"
                                    >
                                        <p>
                                            <strong>Event ID:</strong>
                                            {event.eventId}
                                        </p>
                                        {#if event.actorIdentityId}
                                            <p>
                                                <strong
                                                    >Actor Identity ID:</strong
                                                > {event.actorIdentityId}
                                            </p>
                                        {/if}
                                        {#if event.targetEntityId}
                                            <p>
                                                <strong
                                                    >Target Entity ID:</strong
                                                > {event.targetEntityId}
                                            </p>
                                        {/if}
                                        {#if event.outcomeReason}
                                            <p>
                                                <strong>Reason:</strong>
                                                {event.outcomeReason}
                                            </p>
                                        {/if}
                                        {#if event.metadata && Object.keys(event.metadata).length > 0}
                                            <p>
                                                <strong>Metadata:</strong>
                                                {JSON.stringify(event.metadata, null, 2)}
                                            </p>
                                        {/if}
                                    </div>
                                </td>
                            </tr>
                        {/if}
                    {/each}
                    {#if adminStore.auditEvents.length === 0}
                        <tr>
                            <td
                                colspan="6"
                                class="px-4 py-8 text-center text-clinical-muted"
                            >
                                No audit events found
                            </td>
                        </tr>
                    {/if}
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        {#if adminStore.auditTotalPages > 1}
            <div class="flex items-center justify-between text-xs">
                <span class="text-clinical-muted">
                    {adminStore.auditTotalElements} total events
                </span>
                <div class="flex gap-1">
                    <button
                        disabled={adminStore.auditPage === 0}
                        onclick={() => goToPage(adminStore.auditPage - 1)}
                        class="px-2 py-1 rounded border border-clinical-border text-clinical-muted hover:bg-clinical-hover disabled:opacity-40"
                    >
                        Prev
                    </button>
                    <span class="px-2 py-1 text-clinical-text">
                        {adminStore.auditPage + 1} / {adminStore.auditTotalPages}
                    </span>
                    <button
                        disabled={adminStore.auditPage >=
                            adminStore.auditTotalPages - 1}
                        onclick={() => goToPage(adminStore.auditPage + 1)}
                        class="px-2 py-1 rounded border border-clinical-border text-clinical-muted hover:bg-clinical-hover disabled:opacity-40"
                    >
                        Next
                    </button>
                </div>
            </div>
        {/if}
    {/if}
</div>
