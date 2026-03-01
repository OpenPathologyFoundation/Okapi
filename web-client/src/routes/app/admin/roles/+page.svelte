<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";

    onMount(() => {
        adminStore.loadRolePermissionMatrix();
    });

    // Collect all unique permissions across all roles
    const allPermissions = $derived(
        [
            ...new Set(
                adminStore.rolePermissionMatrix.flatMap((r) => r.permissions),
            ),
        ].sort(),
    );
</script>

<div class="space-y-4">
    <h2 class="text-sm font-semibold text-clinical-text">
        Role-Permission Matrix
    </h2>

    {#if adminStore.isLoading && adminStore.rolePermissionMatrix.length === 0}
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
    {:else if adminStore.rolePermissionMatrix.length > 0}
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg overflow-auto"
        >
            <table class="w-full text-xs">
                <thead>
                    <tr
                        class="border-b border-clinical-border bg-clinical-bg/50"
                    >
                        <th
                            class="text-left px-3 py-2 font-medium text-clinical-muted uppercase sticky left-0 bg-clinical-bg/50 z-10"
                        >
                            Permission / Role
                        </th>
                        {#each adminStore.rolePermissionMatrix as role}
                            <th
                                class="px-3 py-2 font-medium text-clinical-muted text-center whitespace-nowrap"
                            >
                                <div>{role.roleName}</div>
                                {#if role.isSystem}
                                    <span
                                        class="text-[9px] px-1 py-0.5 rounded bg-amber-500/10 text-amber-500"
                                        >SYS</span
                                    >
                                {/if}
                            </th>
                        {/each}
                    </tr>
                </thead>
                <tbody>
                    {#each allPermissions as perm}
                        <tr
                            class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors"
                        >
                            <td
                                class="px-3 py-2 font-mono text-clinical-text sticky left-0 bg-clinical-surface z-10"
                            >
                                {perm}
                            </td>
                            {#each adminStore.rolePermissionMatrix as role}
                                <td class="px-3 py-2 text-center">
                                    {#if role.permissions.includes(perm)}
                                        <span class="text-green-500">&#10003;</span>
                                    {:else}
                                        <span class="text-clinical-muted">—</span>
                                    {/if}
                                </td>
                            {/each}
                        </tr>
                    {/each}
                    {#if allPermissions.length === 0}
                        <tr>
                            <td
                                colspan={adminStore.rolePermissionMatrix.length + 1}
                                class="px-4 py-8 text-center text-clinical-muted"
                            >
                                No permissions defined
                            </td>
                        </tr>
                    {/if}
                </tbody>
            </table>
        </div>
    {:else}
        <div class="text-center py-12 text-clinical-muted">
            No roles defined
        </div>
    {/if}
</div>
