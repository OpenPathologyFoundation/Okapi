<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";

    onMount(() => {
        adminStore.loadDashboard();
    });

    const cards = $derived(
        adminStore.dashboard
            ? [
                  {
                      label: "Total Identities",
                      value: adminStore.dashboard.totalIdentities,
                      icon: "M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z",
                  },
                  {
                      label: "Active Identities",
                      value: adminStore.dashboard.activeIdentities,
                      icon: "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z",
                  },
                  {
                      label: "Roles Defined",
                      value: adminStore.dashboard.totalRoles,
                      icon: "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z",
                  },
                  {
                      label: "Active Devices",
                      value: adminStore.dashboard.activeDevices,
                      icon: "M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z",
                  },
                  {
                      label: "Break-Glass Grants",
                      value: adminStore.dashboard.activeBreakGlassGrants,
                      icon: "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z",
                  },
                  {
                      label: "Research Grants",
                      value: adminStore.dashboard.activeResearchGrants,
                      icon: "M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z",
                  },
              ]
            : [],
    );
</script>

<div>
    {#if adminStore.isLoading && !adminStore.dashboard}
        <div class="flex items-center justify-center py-20">
            <div
                class="h-8 w-8 animate-spin rounded-full border-4 border-clinical-primary border-t-transparent"
            ></div>
        </div>
    {:else if adminStore.error}
        <div
            class="rounded-md bg-red-500/10 border border-red-500/30 p-4 text-sm text-red-500"
        >
            {adminStore.error}
        </div>
    {:else}
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {#each cards as card}
                <div
                    class="bg-clinical-surface border border-clinical-border rounded-lg p-5 flex items-start gap-4"
                >
                    <div
                        class="h-10 w-10 rounded-lg bg-clinical-primary/10 flex items-center justify-center shrink-0"
                    >
                        <svg
                            class="w-5 h-5 text-clinical-primary"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                            ><path
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                stroke-width="2"
                                d={card.icon}
                            ></path></svg
                        >
                    </div>
                    <div>
                        <p class="text-2xl font-bold text-clinical-text">
                            {card.value}
                        </p>
                        <p class="text-xs text-clinical-muted mt-0.5">
                            {card.label}
                        </p>
                    </div>
                </div>
            {/each}
        </div>
    {/if}
</div>
