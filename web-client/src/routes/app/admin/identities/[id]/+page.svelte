<script lang="ts">
    import { page } from "$app/state";
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";
    import ConfirmDialog from "$lib/components/ConfirmDialog.svelte";

    const identityId = $derived(page.params.id ?? "");

    let showRoleAssign = $state(false);
    let selectedRoleId = $state("");
    let justification = $state("");

    let confirmOpen = $state(false);
    let confirmTitle = $state("");
    let confirmMessage = $state("");
    let confirmLabel = $state("Confirm");
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
        if (!identityId) return;
        adminStore.loadIdentity(identityId);
        adminStore.loadRoles();
        adminStore.loadDevices(identityId);
    });

    function toggleActive() {
        if (!adminStore.selectedIdentity) return;
        const newActive = !adminStore.selectedIdentity.isActive;
        const label = newActive ? "Activate" : "Deactivate";
        requestConfirm(
            `${label} Identity`,
            `Are you sure you want to ${label.toLowerCase()} ${adminStore.selectedIdentity.displayName ?? "this identity"}?`,
            label,
            async () => {
                await adminStore.setIdentityActive(identityId, newActive);
            },
        );
    }

    async function handleAssignRole() {
        if (!selectedRoleId) return;
        await adminStore.assignRole(identityId, selectedRoleId, justification);
        showRoleAssign = false;
        selectedRoleId = "";
        justification = "";
        await adminStore.loadIdentity(identityId);
    }

    function handleRevokeRole(roleId: string, roleName: string) {
        requestConfirm(
            "Revoke Role",
            `Are you sure you want to revoke the ${roleName} role from this user?`,
            "Revoke",
            async () => {
                await adminStore.revokeRole(identityId, roleId);
                await adminStore.loadIdentity(identityId);
            },
        );
    }

    function handleRevokeDevice(deviceId: string) {
        requestConfirm(
            "Revoke Device",
            "Are you sure you want to revoke trust for this device?",
            "Revoke",
            async () => {
                await adminStore.revokeDevice(deviceId);
            },
        );
    }

    function formatDate(iso: string | null): string {
        if (!iso) return "—";
        return new Date(iso).toLocaleString("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    const availableRoles = $derived(
        adminStore.roles.filter(
            (r) =>
                !adminStore.selectedIdentity?.roles.some(
                    (ir) => ir.roleId === r.roleId,
                ),
        ),
    );
</script>

<div class="space-y-6 max-w-4xl">
    <!-- Back Link -->
    <a
        href="/app/admin/identities"
        class="inline-flex items-center gap-1 text-xs text-clinical-muted hover:text-clinical-primary transition-colors"
    >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
            ><path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M15 19l-7-7 7-7"
            ></path></svg
        >
        Back to Identities
    </a>

    {#if adminStore.isLoading && !adminStore.selectedIdentity}
        <div class="flex items-center justify-center py-12">
            <div
                class="h-6 w-6 animate-spin rounded-full border-4 border-clinical-primary border-t-transparent"
            ></div>
        </div>
    {:else if adminStore.selectedIdentity}
        {@const identity = adminStore.selectedIdentity}

        <!-- Identity Header -->
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg p-5"
        >
            <div class="flex items-start justify-between">
                <div>
                    <h2 class="text-lg font-semibold text-clinical-text">
                        {identity.displayName ?? "Unknown"}
                    </h2>
                    <p class="text-sm text-clinical-muted mt-0.5">
                        {identity.email ?? "No email"}
                    </p>
                    <div class="mt-2 flex items-center gap-4 text-xs text-clinical-muted">
                        <span>Provider: {identity.providerId}</span>
                        <span>Created: {formatDate(identity.createdAt)}</span>
                        <span>Last seen: {formatDate(identity.lastSeenAt)}</span>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <span
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold {identity.isActive
                            ? 'bg-green-500/10 text-green-500'
                            : 'bg-red-500/10 text-red-500'}"
                    >
                        {identity.isActive ? "Active" : "Inactive"}
                    </span>
                    <button
                        onclick={toggleActive}
                        class="px-3 py-1 rounded-md text-xs font-medium border transition-colors {identity.isActive
                            ? 'border-red-500/30 text-red-500 hover:bg-red-500/10'
                            : 'border-green-500/30 text-green-500 hover:bg-green-500/10'}"
                    >
                        {identity.isActive ? "Deactivate" : "Activate"}
                    </button>
                </div>
            </div>
        </div>

        <!-- Roles Section -->
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg p-5"
        >
            <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-clinical-text">
                    Assigned Roles
                </h3>
                <button
                    onclick={() => (showRoleAssign = !showRoleAssign)}
                    class="px-2 py-1 rounded-md text-xs font-medium bg-clinical-primary text-white hover:bg-clinical-primary/90 transition-colors"
                >
                    + Assign Role
                </button>
            </div>

            {#if showRoleAssign}
                <div
                    class="mb-4 p-3 rounded-md bg-clinical-bg border border-clinical-border-subtle space-y-2"
                >
                    <select
                        bind:value={selectedRoleId}
                        class="w-full rounded-md border-0 bg-clinical-surface py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary"
                    >
                        <option value="">Select a role...</option>
                        {#each availableRoles as role}
                            <option value={role.roleId}>
                                {role.name}
                                {role.description
                                    ? `— ${role.description}`
                                    : ""}
                            </option>
                        {/each}
                    </select>
                    <input
                        type="text"
                        bind:value={justification}
                        placeholder="Justification (optional)"
                        class="w-full rounded-md border-0 bg-clinical-surface py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary"
                    />
                    <div class="flex gap-2">
                        <button
                            onclick={handleAssignRole}
                            disabled={!selectedRoleId}
                            class="px-3 py-1 rounded-md text-xs font-medium bg-clinical-primary text-white hover:bg-clinical-primary/90 disabled:opacity-40 transition-colors"
                        >
                            Assign
                        </button>
                        <button
                            onclick={() => (showRoleAssign = false)}
                            class="px-3 py-1 rounded-md text-xs font-medium text-clinical-muted hover:bg-clinical-hover transition-colors"
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            {/if}

            <div class="space-y-2">
                {#each identity.roles as role}
                    <div
                        class="flex items-center justify-between py-2 px-3 rounded-md bg-clinical-bg"
                    >
                        <div>
                            <span class="text-sm font-medium text-clinical-text"
                                >{role.name}</span
                            >
                            {#if role.description}
                                <span class="text-xs text-clinical-muted ml-2"
                                    >— {role.description}</span
                                >
                            {/if}
                            {#if role.isSystem}
                                <span
                                    class="ml-2 text-[10px] px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-500 font-medium"
                                    >SYSTEM</span
                                >
                            {/if}
                        </div>
                        <button
                            onclick={() => handleRevokeRole(role.roleId, role.name)}
                            class="text-xs text-red-500 hover:text-red-400 transition-colors"
                        >
                            Revoke
                        </button>
                    </div>
                {:else}
                    <p class="text-xs text-clinical-muted py-2">
                        No roles assigned
                    </p>
                {/each}
            </div>
        </div>

        <!-- Devices Section -->
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg p-5"
        >
            <h3 class="text-sm font-semibold text-clinical-text mb-3">
                Devices
            </h3>
            {#if adminStore.devices.length > 0}
                <div class="space-y-2">
                    {#each adminStore.devices as device}
                        <div
                            class="flex items-center justify-between py-2 px-3 rounded-md bg-clinical-bg"
                        >
                            <div class="text-xs">
                                <span
                                    class="font-mono text-clinical-text"
                                    >{device.deviceFingerprintHash.slice(0, 16)}...</span
                                >
                                <span class="text-clinical-muted ml-2">
                                    First: {formatDate(device.firstSeenAt)} | Last: {formatDate(device.lastSeenAt)}
                                </span>
                                {#if device.revokedAt}
                                    <span
                                        class="ml-2 text-[10px] px-1.5 py-0.5 rounded bg-red-500/10 text-red-500 font-medium"
                                        >REVOKED</span
                                    >
                                {/if}
                            </div>
                            {#if !device.revokedAt}
                                <button
                                    onclick={() => handleRevokeDevice(device.deviceId)}
                                    class="text-xs text-red-500 hover:text-red-400 transition-colors"
                                >
                                    Revoke
                                </button>
                            {/if}
                        </div>
                    {/each}
                </div>
            {:else}
                <p class="text-xs text-clinical-muted">No devices recorded</p>
            {/if}
        </div>
    {:else}
        <div class="text-center py-12 text-clinical-muted">
            Identity not found
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
