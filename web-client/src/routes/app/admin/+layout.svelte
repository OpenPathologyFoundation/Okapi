<script lang="ts">
    import type { Snippet } from "svelte";
    import { page } from "$app/state";
    import { goto } from "$app/navigation";
    import { browser } from "$app/environment";
    import { authStore } from "$lib/stores/auth.svelte";

    let { children }: { children: Snippet } = $props();

    $effect(() => {
        if (browser && !authStore.isLoading && !authStore.isAdmin) {
            goto("/app");
        }
    });

    const tabs = [
        { label: "Dashboard", href: "/app/admin" },
        { label: "Identities", href: "/app/admin/identities" },
        { label: "Roles", href: "/app/admin/roles" },
        { label: "IdP Mappings", href: "/app/admin/mappings" },
        { label: "Audit Log", href: "/app/admin/audit" },
        { label: "Grants", href: "/app/admin/grants" },
        { label: "Settings", href: "/app/admin/settings" },
    ];

    function isTabActive(href: string): boolean {
        const p = page.url.pathname;
        if (href === "/app/admin") return p === "/app/admin";
        return p.startsWith(href);
    }
</script>

<div class="flex flex-col h-full">
    <!-- Admin Tab Bar -->
    <div
        class="flex items-center gap-1 px-6 pt-4 pb-0 border-b border-clinical-border bg-clinical-surface"
    >
        <div class="flex items-center gap-2 mr-4">
            <svg
                class="w-5 h-5 text-clinical-primary"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                ><path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                ></path></svg
            >
            <h1 class="text-sm font-semibold text-clinical-text">
                IAM
            </h1>
        </div>
        {#each tabs as tab}
            <a
                href={tab.href}
                class="px-3 py-2 text-xs font-medium rounded-t-md border-b-2 transition-colors {isTabActive(
                    tab.href,
                )
                    ? 'border-clinical-primary text-clinical-primary bg-clinical-primary/5'
                    : 'border-transparent text-clinical-muted hover:text-clinical-text hover:border-clinical-border'}"
            >
                {tab.label}
            </a>
        {/each}
    </div>

    <!-- Admin Content -->
    <div class="flex-1 overflow-auto p-6">
        {@render children()}
    </div>
</div>
