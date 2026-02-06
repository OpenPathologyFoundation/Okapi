<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";
    import ConfirmDialog from "$lib/components/ConfirmDialog.svelte";

    let showCreate = $state(false);
    let newProviderId = $state("keycloak");
    let newGroupName = $state("");
    let newDescription = $state("");
    let newRoleIds = $state<string[]>([]);

    let confirmOpen = $state(false);
    let confirmTitle = $state("");
    let confirmMessage = $state("");
    let confirmLabel = $state("Delete");
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
        adminStore.loadIdpMappings();
        adminStore.loadRoles();
    });

    async function handleCreate() {
        if (!newGroupName.trim()) return;
        await adminStore.createIdpMapping(
            newProviderId,
            newGroupName.trim(),
            newDescription.trim(),
            newRoleIds,
        );
        showCreate = false;
        newGroupName = "";
        newDescription = "";
        newRoleIds = [];
    }

    function handleDelete(id: string, groupName: string) {
        requestConfirm(
            "Delete IdP Mapping",
            `Are you sure you want to delete the mapping for group "${groupName}"? Users assigned through this mapping will lose their roles on next login.`,
            "Delete",
            async () => {
                await adminStore.deleteIdpMapping(id);
            },
        );
    }

    function toggleRole(roleId: string) {
        if (newRoleIds.includes(roleId)) {
            newRoleIds = newRoleIds.filter((r) => r !== roleId);
        } else {
            newRoleIds = [...newRoleIds, roleId];
        }
    }
</script>

<div class="space-y-4">
    <div class="flex items-center justify-between">
        <h2 class="text-sm font-semibold text-clinical-text">
            IdP Group Mappings
        </h2>
        <button
            onclick={() => (showCreate = !showCreate)}
            class="px-3 py-1.5 rounded-md bg-clinical-primary text-white text-xs font-medium hover:bg-clinical-primary/90 transition-colors"
        >
            + New Mapping
        </button>
    </div>

    {#if showCreate}
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg p-4 space-y-3"
        >
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label for="idp-provider-id" class="block text-xs font-medium text-clinical-muted mb-1"
                        >Provider ID</label
                    >
                    <input
                        id="idp-provider-id"
                        type="text"
                        bind:value={newProviderId}
                        class="w-full rounded-md border-0 bg-clinical-bg py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary"
                    />
                </div>
                <div>
                    <label for="idp-group-name" class="block text-xs font-medium text-clinical-muted mb-1"
                        >Group Name</label
                    >
                    <input
                        id="idp-group-name"
                        type="text"
                        bind:value={newGroupName}
                        placeholder="e.g., pathologists"
                        class="w-full rounded-md border-0 bg-clinical-bg py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary"
                    />
                </div>
            </div>
            <div>
                <label for="idp-description" class="block text-xs font-medium text-clinical-muted mb-1"
                    >Description</label
                >
                <input
                    id="idp-description"
                    type="text"
                    bind:value={newDescription}
                    class="w-full rounded-md border-0 bg-clinical-bg py-1.5 pl-3 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary"
                />
            </div>
            <div>
                <span class="block text-xs font-medium text-clinical-muted mb-1"
                    >Roles</span
                >
                <div class="flex flex-wrap gap-2">
                    {#each adminStore.roles as role}
                        <button
                            onclick={() => toggleRole(role.roleId)}
                            class="px-2 py-1 rounded text-xs border transition-colors {newRoleIds.includes(
                                role.roleId,
                            )
                                ? 'bg-clinical-primary/20 border-clinical-primary text-clinical-primary'
                                : 'border-clinical-border text-clinical-muted hover:bg-clinical-hover'}"
                        >
                            {role.name}
                        </button>
                    {/each}
                </div>
            </div>
            <div class="flex gap-2">
                <button
                    onclick={handleCreate}
                    disabled={!newGroupName.trim()}
                    class="px-3 py-1.5 rounded-md text-xs font-medium bg-clinical-primary text-white hover:bg-clinical-primary/90 disabled:opacity-40 transition-colors"
                >
                    Create
                </button>
                <button
                    onclick={() => (showCreate = false)}
                    class="px-3 py-1.5 rounded-md text-xs font-medium text-clinical-muted hover:bg-clinical-hover transition-colors"
                >
                    Cancel
                </button>
            </div>
        </div>
    {/if}

    {#if adminStore.isLoading && adminStore.idpMappings.length === 0}
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
            <table class="w-full text-sm">
                <thead>
                    <tr
                        class="border-b border-clinical-border bg-clinical-bg/50"
                    >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Provider</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Group Name</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Description</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Roles</th
                        >
                        <th
                            class="text-right px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Actions</th
                        >
                    </tr>
                </thead>
                <tbody>
                    {#each adminStore.idpMappings as mapping}
                        <tr
                            class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors"
                        >
                            <td class="px-4 py-3 text-clinical-muted text-xs"
                                >{mapping.providerId}</td
                            >
                            <td
                                class="px-4 py-3 font-medium text-clinical-text"
                                >{mapping.groupName}</td
                            >
                            <td class="px-4 py-3 text-clinical-muted text-xs"
                                >{mapping.description ?? "—"}</td
                            >
                            <td class="px-4 py-3">
                                <div class="flex flex-wrap gap-1">
                                    {#each mapping.roleNames as role}
                                        <span
                                            class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-clinical-primary/10 text-clinical-primary"
                                        >
                                            {role}
                                        </span>
                                    {/each}
                                </div>
                            </td>
                            <td class="px-4 py-3 text-right">
                                <button
                                    onclick={() =>
                                        handleDelete(mapping.idpGroupId, mapping.groupName)}
                                    class="text-xs text-red-500 hover:text-red-400 transition-colors"
                                >
                                    Delete
                                </button>
                            </td>
                        </tr>
                    {/each}
                    {#if adminStore.idpMappings.length === 0}
                        <tr>
                            <td
                                colspan="5"
                                class="px-4 py-8 text-center text-clinical-muted"
                            >
                                No IdP mappings configured
                            </td>
                        </tr>
                    {/if}
                </tbody>
            </table>
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
