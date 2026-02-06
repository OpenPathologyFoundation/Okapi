<script lang="ts">
    import type { Snippet } from "svelte";
    import { page } from "$app/state";
    import { onMount, onDestroy } from "svelte";
    import { goto } from "$app/navigation";
    import { browser } from "$app/environment";
    import { themeStore } from "$lib/stores/theme.svelte";
    import { authStore } from "$lib/stores/auth.svelte";
    import { idleStore } from "$lib/stores/idle.svelte";
    import IdleWarningModal from "$lib/components/IdleWarningModal.svelte";

    // Redirect to /logged-out when auth fails (covers back-button after sign-out)
    $effect(() => {
        if (browser && !authStore.isLoading && !authStore.user) {
            goto("/logged-out");
        }
    });

    let { children }: { children: Snippet } = $props();
    // src/routes/app/+layout.svelte
    // Application Shell: Top Bar + Left Nav Rail

    import Echo from "$lib/components/Echo.svelte"; // Import Echo

    // Derive user display from auth store
    const userColor = "bg-blue-600";

    // Navigation active state helper
    function isActive(path: string): boolean {
        const currentPath = page.url.pathname;
        if (path === "/app") return currentPath === "/app";
        return currentPath.startsWith(path);
    }

    function getInitials(user: typeof authStore.user): string {
        if (!user) return "??";
        if (user.displayShort) return user.displayShort.slice(0, 2).toUpperCase();
        const g = user.givenName?.[0] ?? "";
        const f = user.familyName?.[0] ?? "";
        return (g + f).toUpperCase() || "??";
    }

    onMount(async () => {
        themeStore.init();
        authStore.load();
        try {
            const res = await fetch("/auth/session-info", {
                credentials: "include",
            });
            if (res.ok) {
                const data = await res.json();
                idleStore.init(data.idleTimeoutMinutes ?? 15);
            } else {
                idleStore.init(15);
            }
        } catch {
            idleStore.init(15);
        }
    });

    onDestroy(() => {
        idleStore.destroy();
    });
</script>

<div
    class="flex flex-col h-screen w-screen bg-clinical-bg text-clinical-text font-sans overflow-hidden"
>
    <!-- [A] TOP GLOBAL NAVIGATION BAR (Fixed Height: 60px) -->
    <header
        class="h-[60px] flex items-center justify-between px-4 border-b border-clinical-border bg-clinical-surface shrink-0 z-20"
    >
        <!-- Left: Wordmark -->
        <div class="flex items-center gap-2 w-64">
            <img
                src="/logo-nb.png"
                alt="Okapi"
                class="h-10 w-auto opacity-80"
            />
            <span
                class="font-bold tracking-tight text-clinical-text"
                >Okapi</span
            >
        </div>

        <!-- Center: Omni-Search -->
        <div class="flex-1 max-w-2xl">
            <div class="relative group">
                <div
                    class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"
                >
                    <svg
                        class="h-4 w-4 text-clinical-muted group-hover:text-clinical-primary transition-colors"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                        />
                    </svg>
                </div>
                <input
                    type="text"
                    placeholder="Search Cases, Patients, or Archive..."
                    class="block w-full rounded-md border-0 bg-clinical-bg py-1.5 pl-10 text-clinical-text ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 transition-all"
                />
            </div>
        </div>

        <!-- Right: User Avatar Badge (Artifact 2) -->
        <div class="w-64 flex justify-end items-center gap-4">
            <!-- Badge -->
            <!-- Badge & Dropdown -->
            <div class="relative group">
                <button class="flex items-center gap-2 focus:outline-none">
                    <div class="text-right hidden sm:block">
                        <p
                            class="text-xs font-medium text-clinical-text group-hover:text-clinical-primary transition-colors"
                        >
                            {authStore.user?.displayName ?? "Loading..."}
                        </p>
                        <p
                            class="text-[10px] text-clinical-muted uppercase tracking-wider"
                        >
                            {authStore.user?.roles?.[0] ?? ""}
                        </p>
                    </div>
                    <div
                        class={`h-8 w-8 rounded-full ${userColor} flex items-center justify-center text-xs font-bold text-white shadow-lg ring-2 ring-transparent group-hover:ring-clinical-primary/50 transition-all`}
                    >
                        {getInitials(authStore.user)}
                    </div>
                </button>

                <!-- Dropdown Menu -->
                <div
                    class="absolute right-0 top-full mt-2 w-56 bg-clinical-surface border border-clinical-border rounded-md shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 transform origin-top-right overflow-hidden"
                >
                    <div class="py-1">
                        <!-- Appearance Submenu in Main Menu -->
                        <div
                            class="px-4 py-2 text-xs font-semibold text-clinical-muted uppercase tracking-wider border-b border-clinical-border-subtle mb-1"
                        >
                            Appearance
                        </div>
                        <div
                            class="px-2 pb-2 border-b border-clinical-border-subtle mb-1 flex gap-1 justify-between"
                        >
                            {#each ["system", "dark", "light"] as m}
                                <button
                                    class="flex-1 px-2 py-1.5 text-xs rounded-md border transition-all capitalize {themeStore.mode ===
                                    m
                                        ? 'bg-clinical-primary/20 border-clinical-primary text-clinical-primary'
                                        : 'border-transparent text-clinical-muted hover:bg-clinical-hover'}"
                                    onclick={() =>
                                        themeStore.setMode(
                                            m as "system" | "dark" | "light",
                                        )}
                                >
                                    {m}
                                </button>
                            {/each}
                        </div>

                        <a
                            href="/app/profile"
                            class="block px-4 py-2 text-sm text-clinical-text hover:bg-clinical-hover"
                        >
                            Profile Settings
                        </a>
                        <a
                            href="/auth/logout"
                            class="block px-4 py-2 text-sm text-red-500 dark:text-red-400 hover:bg-red-500/10"
                        >
                            Sign Out
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </header>

    <div class="flex flex-1 overflow-hidden relative">
        <!-- [B] LEFT NAVIGATION RAIL (Slim Fixed Width) -->
        <nav
            class="w-[60px] flex flex-col items-center py-4 bg-clinical-surface border-r border-clinical-border shrink-0 z-10 gap-6"
        >
            <!-- Nav Icons -->
            <a
                href="/app"
                class="p-2 rounded-lg transition-all group relative {isActive('/app') && !isActive('/app/')
                    ? 'text-clinical-primary bg-clinical-primary/10 hover:bg-clinical-primary/20'
                    : 'text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover'}"
                title="Dashboard"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                    ></path></svg
                >
            </a>

            <a
                href="/app/worklist"
                class="p-2 rounded-lg transition-all group relative {isActive('/app/worklist')
                    ? 'text-clinical-primary bg-clinical-primary/10'
                    : 'text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover'}"
                title="Worklist"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
                    ></path></svg
                >
            </a>

            <a
                href="/app/archive"
                class="p-2 rounded-lg transition-all group relative {isActive('/app/archive')
                    ? 'text-clinical-primary bg-clinical-primary/10'
                    : 'text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover'}"
                title="Archive Search"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01"
                    ></path></svg
                >
            </a>

            <a
                href="/app/hat"
                class="p-2 rounded-lg transition-all group relative {isActive('/app/hat')
                    ? 'text-clinical-primary bg-clinical-primary/10'
                    : 'text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover'}"
                title="Asset Tracking"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                    ></path></svg
                >
            </a>

            <a
                href="/app/ai"
                class="p-2 rounded-lg transition-all group relative {isActive('/app/ai')
                    ? 'text-clinical-primary bg-clinical-primary/10'
                    : 'text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover'}"
                title="AI Tools"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M13 10V3L4 14h7v7l9-11h-7z"
                    ></path></svg
                >
            </a>

            {#if authStore.isAdmin}
                <a
                    href="/app/admin"
                    class="p-2 rounded-lg transition-all group relative {isActive('/app/admin')
                        ? 'text-clinical-primary bg-clinical-primary/10'
                        : 'text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover'}"
                    title="Admin"
                >
                    <svg
                        class="w-6 h-6"
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
                </a>
            {/if}

            <div class="flex-1"></div>

            <!-- Echo (Feedback) -->
            <div class="mb-4">
                <Echo />
            </div>

            <!-- Settings -->
            <a
                href="/app/settings"
                class="p-2 rounded-lg text-clinical-muted hover:text-clinical-text hover:bg-clinical-hover transition-all"
                title="Settings"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                    ></path><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                    ></path></svg
                >
            </a>

            <!-- Sign Out -->
            <a
                href="/auth/logout"
                class="p-2 rounded-lg text-clinical-muted hover:text-red-500 dark:hover:text-red-400 hover:bg-red-500/10 transition-all mb-2"
                title="Sign Out"
            >
                <svg
                    class="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    ><path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                    ></path></svg
                >
            </a>
        </nav>

        <!-- [D] MAIN CONTENT AREA (Canvas) -->
        <main class="flex-1 overflow-auto bg-clinical-bg relative">
            {@render children()}
        </main>
    </div>
</div>

<IdleWarningModal />
