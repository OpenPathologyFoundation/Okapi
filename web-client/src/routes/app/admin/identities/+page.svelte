<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";

    let search = $state("");
    let activeFilter = $state<boolean | undefined>(undefined);

    onMount(() => {
        adminStore.loadIdentities();
    });

    function handleSearch() {
        adminStore.loadIdentities(
            search || undefined,
            activeFilter,
            0,
            20,
        );
    }

    function goToPage(p: number) {
        adminStore.loadIdentities(
            search || undefined,
            activeFilter,
            p,
            20,
        );
    }

    function formatDate(iso: string | null): string {
        if (!iso) return "—";
        return new Date(iso).toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
        });
    }
</script>

<div class="space-y-4">
    <!-- Search / Filter Bar -->
    <div class="flex items-center gap-3">
        <div class="relative flex-1 max-w-md">
            <input
                type="text"
                bind:value={search}
                onkeydown={(e) => e.key === "Enter" && handleSearch()}
                placeholder="Search by name, email, or username..."
                class="w-full rounded-md border-0 bg-clinical-bg py-1.5 pl-3 pr-10 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-clinical-primary"
            />
        </div>
        <select
            bind:value={activeFilter}
            onchange={handleSearch}
            class="rounded-md border-0 bg-clinical-bg py-1.5 pl-3 pr-8 text-sm text-clinical-text ring-1 ring-inset ring-clinical-border focus:ring-2 focus:ring-clinical-primary"
        >
            <option value={undefined}>All Status</option>
            <option value={true}>Active</option>
            <option value={false}>Inactive</option>
        </select>
        <button
            onclick={handleSearch}
            class="px-3 py-1.5 rounded-md bg-clinical-primary text-white text-sm font-medium hover:bg-clinical-primary/90 transition-colors"
        >
            Search
        </button>
    </div>

    <!-- Results Table -->
    {#if adminStore.isLoading && adminStore.identities.length === 0}
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
                            >Name</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Email</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Roles</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Status</th
                        >
                        <th
                            class="text-left px-4 py-2 text-xs font-medium text-clinical-muted uppercase"
                            >Last Seen</th
                        >
                    </tr>
                </thead>
                <tbody>
                    {#each adminStore.identities as identity}
                        <tr
                            class="border-b border-clinical-border-subtle hover:bg-clinical-hover transition-colors cursor-pointer"
                        >
                            <td class="px-4 py-3">
                                <a
                                    href="/app/admin/identities/{identity.identityId}"
                                    class="text-clinical-primary hover:underline font-medium"
                                >
                                    {identity.displayName ?? identity.username ?? "—"}
                                </a>
                            </td>
                            <td
                                class="px-4 py-3 text-clinical-muted"
                                >{identity.email ?? "—"}</td
                            >
                            <td class="px-4 py-3">
                                <div class="flex flex-wrap gap-1">
                                    {#each identity.roles as role}
                                        <span
                                            class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-clinical-primary/10 text-clinical-primary"
                                        >
                                            {role}
                                        </span>
                                    {/each}
                                </div>
                            </td>
                            <td class="px-4 py-3">
                                <span
                                    class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold {identity.isActive
                                        ? 'bg-green-500/10 text-green-500'
                                        : 'bg-red-500/10 text-red-500'}"
                                >
                                    {identity.isActive ? "Active" : "Inactive"}
                                </span>
                            </td>
                            <td class="px-4 py-3 text-clinical-muted text-xs">
                                {formatDate(identity.lastSeenAt)}
                            </td>
                        </tr>
                    {/each}
                    {#if adminStore.identities.length === 0}
                        <tr>
                            <td
                                colspan="5"
                                class="px-4 py-8 text-center text-clinical-muted"
                            >
                                No identities found
                            </td>
                        </tr>
                    {/if}
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        {#if adminStore.identityTotalPages > 1}
            <div class="flex items-center justify-between text-xs">
                <span class="text-clinical-muted">
                    {adminStore.identityTotalElements} total identities
                </span>
                <div class="flex gap-1">
                    <button
                        disabled={adminStore.identityPage === 0}
                        onclick={() => goToPage(adminStore.identityPage - 1)}
                        class="px-2 py-1 rounded border border-clinical-border text-clinical-muted hover:bg-clinical-hover disabled:opacity-40"
                    >
                        Prev
                    </button>
                    <span class="px-2 py-1 text-clinical-text">
                        {adminStore.identityPage + 1} / {adminStore.identityTotalPages}
                    </span>
                    <button
                        disabled={adminStore.identityPage >=
                            adminStore.identityTotalPages - 1}
                        onclick={() => goToPage(adminStore.identityPage + 1)}
                        class="px-2 py-1 rounded border border-clinical-border text-clinical-muted hover:bg-clinical-hover disabled:opacity-40"
                    >
                        Next
                    </button>
                </div>
            </div>
        {/if}
    {/if}
</div>
