<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";
    import ConfirmDialog from "$lib/components/ConfirmDialog.svelte";

    let statusFilter = $state("");
    let expandedId = $state<string | null>(null);
    let editingNotes = $state("");
    let deleteTarget = $state<string | null>(null);

    onMount(() => {
        adminStore.loadFeedback();
    });

    function handleFilter() {
        adminStore.loadFeedback(statusFilter || undefined, 0, 20);
    }

    function goToPage(p: number) {
        adminStore.loadFeedback(statusFilter || undefined, p, 20);
    }

    async function toggleExpand(id: string) {
        if (expandedId === id) {
            expandedId = null;
            adminStore.selectedFeedback = null;
            return;
        }
        expandedId = id;
        await adminStore.loadFeedbackDetail(id);
        editingNotes = adminStore.selectedFeedback?.adminNotes ?? "";
    }

    async function handleAcknowledge(id: string) {
        await adminStore.updateFeedback(id, "acknowledged");
        await adminStore.loadFeedback(statusFilter || undefined, adminStore.feedbackPage, 20);
        if (expandedId === id) await adminStore.loadFeedbackDetail(id);
    }

    async function handleArchive(id: string) {
        await adminStore.updateFeedback(id, "archived");
        await adminStore.loadFeedback(statusFilter || undefined, adminStore.feedbackPage, 20);
        if (expandedId === id) await adminStore.loadFeedbackDetail(id);
    }

    async function handleSaveNotes(id: string) {
        await adminStore.updateFeedback(id, undefined, editingNotes);
        if (expandedId === id) await adminStore.loadFeedbackDetail(id);
    }

    async function handleDelete() {
        if (!deleteTarget) return;
        await adminStore.deleteFeedback(deleteTarget);
        if (expandedId === deleteTarget) {
            expandedId = null;
            adminStore.selectedFeedback = null;
        }
        deleteTarget = null;
    }

    function formatDate(iso: string): string {
        return new Date(iso).toLocaleString("en-US", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    function categoryColor(c: string): string {
        if (c === "bug_report") return "bg-red-500/10 text-red-500";
        if (c === "suggestion") return "bg-blue-500/10 text-blue-500";
        if (c === "question") return "bg-amber-500/10 text-amber-500";
        return "bg-gray-500/10 text-gray-400";
    }

    function statusColor(s: string): string {
        if (s === "pending") return "bg-amber-500/10 text-amber-500";
        if (s === "acknowledged") return "bg-blue-500/10 text-blue-500";
        if (s === "archived") return "bg-gray-500/10 text-gray-400";
        return "bg-gray-500/10 text-gray-400";
    }

    function categoryLabel(c: string): string {
        return c.replace(/_/g, " ");
    }
</script>

<div class="space-y-4">
    <!-- Filter Bar -->
    <div class="flex items-center gap-3 flex-wrap">
        <select
            bind:value={statusFilter}
            onchange={handleFilter}
            class="rounded-md border-0 bg-clinical-bg py-1.5 pl-3 pr-8 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary"
        >
            <option value="">All Status</option>
            <option value="pending">Pending</option>
            <option value="acknowledged">Acknowledged</option>
            <option value="archived">Archived</option>
        </select>
        <span class="text-xs text-clinical-muted">
            {adminStore.feedbackTotalElements} items
        </span>
    </div>

    <!-- Feedback Table -->
    {#if adminStore.isLoading && adminStore.feedback.length === 0}
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
                            >Submitter</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Category</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Preview</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Status</th
                        >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                            >Actions</th
                        >
                    </tr>
                </thead>
                <tbody>
                    {#each adminStore.feedback as item}
                        <tr
                            class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors cursor-pointer"
                            onclick={() => toggleExpand(item.feedbackId)}
                        >
                            <td
                                class="px-3 py-2 text-clinical-muted whitespace-nowrap"
                                >{formatDate(item.createdAt)}</td
                            >
                            <td class="px-3 py-2 text-clinical-text">
                                <div>{item.submitterName}</div>
                                <div class="text-clinical-muted text-[10px]">{item.submitterEmail}</div>
                            </td>
                            <td class="px-3 py-2">
                                <span
                                    class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold capitalize {categoryColor(item.category)}"
                                >
                                    {categoryLabel(item.category)}
                                </span>
                            </td>
                            <td
                                class="px-3 py-2 text-clinical-muted truncate max-w-[250px]"
                                >{item.bodyPreview}</td
                            >
                            <td class="px-3 py-2">
                                <span
                                    class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold capitalize {statusColor(item.status)}"
                                >
                                    {item.status}
                                </span>
                            </td>
                            <td class="px-3 py-2">
                                <div class="flex gap-1" onclick={(e) => e.stopPropagation()}>
                                    {#if item.status === "pending"}
                                        <button
                                            onclick={() => handleAcknowledge(item.feedbackId)}
                                            class="px-2 py-0.5 rounded text-[10px] font-medium bg-blue-500/10 text-blue-500 hover:bg-blue-500/20 transition-colors"
                                        >
                                            Ack
                                        </button>
                                    {/if}
                                    {#if item.status !== "archived"}
                                        <button
                                            onclick={() => handleArchive(item.feedbackId)}
                                            class="px-2 py-0.5 rounded text-[10px] font-medium bg-gray-500/10 text-gray-400 hover:bg-gray-500/20 transition-colors"
                                        >
                                            Archive
                                        </button>
                                    {/if}
                                    <button
                                        onclick={() => { deleteTarget = item.feedbackId; }}
                                        class="px-2 py-0.5 rounded text-[10px] font-medium bg-red-500/10 text-red-500 hover:bg-red-500/20 transition-colors"
                                    >
                                        Delete
                                    </button>
                                </div>
                            </td>
                        </tr>
                        {#if expandedId === item.feedbackId && adminStore.selectedFeedback}
                            <tr class="bg-clinical-bg/50">
                                <td colspan="6" class="px-4 py-3">
                                    <div class="space-y-3 text-xs">
                                        <!-- Full body -->
                                        <div>
                                            <p class="font-medium text-clinical-muted uppercase mb-1">Full Message</p>
                                            <p class="text-clinical-text whitespace-pre-wrap">{adminStore.selectedFeedback.body}</p>
                                        </div>
                                        <!-- Context -->
                                        {#if adminStore.selectedFeedback.context && Object.keys(adminStore.selectedFeedback.context).length > 0}
                                            <div>
                                                <p class="font-medium text-clinical-muted uppercase mb-1">Context</p>
                                                <pre class="text-clinical-muted font-mono text-[10px] bg-clinical-bg rounded p-2 overflow-x-auto">{JSON.stringify(adminStore.selectedFeedback.context, null, 2)}</pre>
                                            </div>
                                        {/if}
                                        <!-- Admin Notes -->
                                        <div>
                                            <p class="font-medium text-clinical-muted uppercase mb-1">Admin Notes</p>
                                            <div class="flex gap-2">
                                                <textarea
                                                    bind:value={editingNotes}
                                                    class="flex-1 rounded-md border-0 bg-clinical-bg py-1.5 px-2 text-xs text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary resize-none h-16"
                                                    placeholder="Add notes..."
                                                    onclick={(e) => e.stopPropagation()}
                                                ></textarea>
                                                <button
                                                    onclick={(e) => { e.stopPropagation(); handleSaveNotes(item.feedbackId); }}
                                                    class="px-3 py-1.5 rounded-md bg-clinical-primary text-white text-[10px] font-medium hover:bg-clinical-primary/90 transition-colors self-end"
                                                >
                                                    Save
                                                </button>
                                            </div>
                                        </div>
                                        <!-- Timestamps -->
                                        <div class="flex gap-4 text-[10px] text-clinical-muted font-mono">
                                            <span>Created: {formatDate(adminStore.selectedFeedback.createdAt)}</span>
                                            {#if adminStore.selectedFeedback.acknowledgedAt}
                                                <span>Acknowledged: {formatDate(adminStore.selectedFeedback.acknowledgedAt)}</span>
                                            {/if}
                                            {#if adminStore.selectedFeedback.archivedAt}
                                                <span>Archived: {formatDate(adminStore.selectedFeedback.archivedAt)}</span>
                                            {/if}
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        {/if}
                    {/each}
                    {#if adminStore.feedback.length === 0}
                        <tr>
                            <td
                                colspan="6"
                                class="px-4 py-8 text-center text-clinical-muted"
                            >
                                No feedback found
                            </td>
                        </tr>
                    {/if}
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        {#if adminStore.feedbackTotalPages > 1}
            <div class="flex items-center justify-between text-xs">
                <span class="text-clinical-muted">
                    {adminStore.feedbackTotalElements} total items
                </span>
                <div class="flex gap-1">
                    <button
                        disabled={adminStore.feedbackPage === 0}
                        onclick={() => goToPage(adminStore.feedbackPage - 1)}
                        class="px-2 py-1 rounded border border-clinical-border text-clinical-muted hover:bg-clinical-hover disabled:opacity-40"
                    >
                        Prev
                    </button>
                    <span class="px-2 py-1 text-clinical-text">
                        {adminStore.feedbackPage + 1} / {adminStore.feedbackTotalPages}
                    </span>
                    <button
                        disabled={adminStore.feedbackPage >=
                            adminStore.feedbackTotalPages - 1}
                        onclick={() => goToPage(adminStore.feedbackPage + 1)}
                        class="px-2 py-1 rounded border border-clinical-border text-clinical-muted hover:bg-clinical-hover disabled:opacity-40"
                    >
                        Next
                    </button>
                </div>
            </div>
        {/if}
    {/if}
</div>

<ConfirmDialog
    open={deleteTarget !== null}
    title="Delete Feedback"
    message="This will permanently delete this feedback item. This action cannot be undone."
    confirmLabel="Delete"
    variant="danger"
    onconfirm={handleDelete}
    oncancel={() => { deleteTarget = null; }}
/>
