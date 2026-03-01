<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";
    import ConfirmDialog from "$lib/components/ConfirmDialog.svelte";

    let revokeReason = $state("");
    let revokeTargetId = $state<string | null>(null);

    let confirmOpen = $state(false);
    let confirmTitle = $state("");
    let confirmMessage = $state("");
    let confirmLabel = $state("Revoke");
    let confirmAction = $state<(() => void) | null>(null);

    function requestConfirm(title: string, message: string, label: string, action: () => void) {
        confirmTitle = title;
        confirmMessage = message;
        confirmLabel = label;
        confirmAction = action;
        confirmOpen = true;
    }

    function handleConfirm() {
        confirmOpen = false;
        confirmAction?.();
        confirmAction = null;
    }

    function handleCancel() {
        confirmOpen = false;
        confirmAction = null;
    }

    onMount(() => {
        adminStore.loadBreakGlassGrants();
        adminStore.loadResearchGrants();
    });

    function revokeBreakGlass(id: string) {
        requestConfirm(
            "Revoke Break-Glass Grant",
            "Are you sure you want to revoke this break-glass grant? The user will immediately lose emergency access.",
            "Revoke",
            async () => {
                await adminStore.revokeBreakGlassGrant(id);
            },
        );
    }

    async function revokeResearch() {
        if (!revokeTargetId) return;
        await adminStore.revokeResearchGrant(revokeTargetId, revokeReason);
        revokeTargetId = null;
        revokeReason = "";
    }

    function formatDate(iso: string): string {
        return new Date(iso).toLocaleString("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    function timeUntil(iso: string): string {
        const diff = new Date(iso).getTime() - Date.now();
        if (diff <= 0) return "Expired";
        const hours = Math.floor(diff / 3600000);
        const mins = Math.floor((diff % 3600000) / 60000);
        if (hours > 24)
            return `${Math.floor(hours / 24)}d ${hours % 24}h remaining`;
        return `${hours}h ${mins}m remaining`;
    }
</script>

<div class="space-y-8">
    {#if adminStore.isLoading}
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
        <!-- Break-Glass Grants -->
        <div>
            <h2 class="text-sm font-semibold text-clinical-text mb-3">
                Active Break-Glass Grants
            </h2>
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
                                >Identity</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Scope</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Reason</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Granted</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Expires</th
                            >
                            <th
                                class="text-right px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Actions</th
                            >
                        </tr>
                    </thead>
                    <tbody>
                        {#each adminStore.breakGlassGrants as grant}
                            <tr
                                class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors"
                            >
                                <td class="px-3 py-2 font-mono text-clinical-text"
                                    >{grant.identityId.slice(0, 8)}...</td
                                >
                                <td class="px-3 py-2 text-clinical-muted">
                                    {grant.scopeEntityType}
                                </td>
                                <td class="px-3 py-2 text-clinical-text"
                                    >{grant.reasonCode}</td
                                >
                                <td
                                    class="px-3 py-2 text-clinical-muted whitespace-nowrap"
                                    >{formatDate(grant.grantedAt)}</td
                                >
                                <td class="px-3 py-2">
                                    <span
                                        class="text-amber-500 font-medium whitespace-nowrap"
                                        >{timeUntil(grant.expiresAt)}</span
                                    >
                                </td>
                                <td class="px-3 py-2 text-right">
                                    <button
                                        onclick={() =>
                                            revokeBreakGlass(grant.grantId)}
                                        class="text-red-500 hover:text-red-400 font-medium transition-colors"
                                    >
                                        Revoke
                                    </button>
                                </td>
                            </tr>
                        {/each}
                        {#if adminStore.breakGlassGrants.length === 0}
                            <tr>
                                <td
                                    colspan="6"
                                    class="px-4 py-6 text-center text-clinical-muted"
                                >
                                    No active break-glass grants
                                </td>
                            </tr>
                        {/if}
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Research Grants -->
        <div>
            <h2 class="text-sm font-semibold text-clinical-text mb-3">
                Active Research Grants
            </h2>
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
                                >Identity</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Protocol</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >PHI Level</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Reason</th
                            >
                            <th
                                class="text-left px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Expires</th
                            >
                            <th
                                class="text-right px-3 py-2 font-medium text-clinical-muted uppercase"
                                >Actions</th
                            >
                        </tr>
                    </thead>
                    <tbody>
                        {#each adminStore.researchGrants as grant}
                            <tr
                                class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors"
                            >
                                <td class="px-3 py-2 font-mono text-clinical-text"
                                    >{grant.identityId.slice(0, 8)}...</td
                                >
                                <td class="px-3 py-2 text-clinical-text"
                                    >{grant.protocolId ?? "—"}</td
                                >
                                <td class="px-3 py-2">
                                    <span
                                        class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-500/10 text-amber-500"
                                    >
                                        {grant.phiAccessLevel}
                                    </span>
                                </td>
                                <td
                                    class="px-3 py-2 text-clinical-muted truncate max-w-[200px]"
                                    >{grant.reason}</td
                                >
                                <td class="px-3 py-2">
                                    <span
                                        class="text-amber-500 font-medium whitespace-nowrap"
                                        >{timeUntil(grant.expiresAt)}</span
                                    >
                                </td>
                                <td class="px-3 py-2 text-right">
                                    {#if revokeTargetId === grant.grantId}
                                        <div class="flex items-center gap-1 justify-end">
                                            <input
                                                type="text"
                                                bind:value={revokeReason}
                                                placeholder="Reason..."
                                                class="w-32 rounded border-0 bg-clinical-bg py-0.5 px-2 text-xs ring-1 ring-inset ring-clinical-border focus:ring-clinical-primary"
                                            />
                                            <button
                                                onclick={revokeResearch}
                                                class="text-red-500 hover:text-red-400 font-medium"
                                                >OK</button
                                            >
                                            <button
                                                onclick={() =>
                                                    (revokeTargetId = null)}
                                                class="text-clinical-muted hover:text-clinical-text"
                                                >X</button
                                            >
                                        </div>
                                    {:else}
                                        <button
                                            onclick={() =>
                                                (revokeTargetId =
                                                    grant.grantId)}
                                            class="text-red-500 hover:text-red-400 font-medium transition-colors"
                                        >
                                            Revoke
                                        </button>
                                    {/if}
                                </td>
                            </tr>
                        {/each}
                        {#if adminStore.researchGrants.length === 0}
                            <tr>
                                <td
                                    colspan="6"
                                    class="px-4 py-6 text-center text-clinical-muted"
                                >
                                    No active research grants
                                </td>
                            </tr>
                        {/if}
                    </tbody>
                </table>
            </div>
        </div>
    {/if}
</div>

<ConfirmDialog
    open={confirmOpen}
    title={confirmTitle}
    message={confirmMessage}
    confirmLabel={confirmLabel}
    variant="danger"
    onconfirm={handleConfirm}
    oncancel={handleCancel}
/>
