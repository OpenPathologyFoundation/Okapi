<script lang="ts">
    import { page } from "$app/state";
    import { onMount } from "svelte";
    import { themeStore } from "$lib/stores/theme.svelte";
    // src/routes/app/+layout.svelte
    // Application Shell: Top Bar + Left Nav Rail

    import Echo from "$lib/components/Echo.svelte"; // Import Echo

    // Mock User State
    const user = {
        initials: "EL",
        role: "Pathologist",
        color: "bg-blue-600",
    };

    onMount(() => {
        themeStore.init();
    });
</script>

<div
    class="flex flex-col h-screen w-screen bg-clinical-bg text-gray-200 font-sans overflow-hidden"
>
    <!-- [A] TOP GLOBAL NAVIGATION BAR (Fixed Height: 60px) -->
    <header
        class="h-[60px] flex items-center justify-between px-4 border-b border-gray-800 bg-clinical-surface shrink-0 z-20"
    >
        <!-- Left: Wordmark -->
        <div class="flex items-center gap-2 w-64">
            <img
                src="/logo-nb.png"
                alt="Okapi"
                class="h-10 w-auto opacity-80"
            />
            <span
                class="font-bold tracking-tight text-gray-900 dark:text-gray-300"
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
                        class="h-4 w-4 text-gray-500 group-hover:text-clinical-primary transition-colors"
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
                    class="block w-full rounded-md border-0 bg-gray-100 dark:bg-gray-900/50 py-1.5 pl-10 text-gray-900 dark:text-gray-300 ring-1 ring-inset ring-gray-300 dark:ring-gray-700 placeholder:text-gray-500 dark:placeholder:text-gray-600 focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 transition-all"
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
                            class="text-xs font-medium text-white group-hover:text-clinical-primary transition-colors"
                        >
                            Dr. Elena Li
                        </p>
                        <p
                            class="text-[10px] text-gray-500 uppercase tracking-wider"
                        >
                            Surgical Path
                        </p>
                    </div>
                    <div
                        class={`h-8 w-8 rounded-full ${user.color} flex items-center justify-center text-xs font-bold text-white shadow-lg ring-2 ring-transparent group-hover:ring-clinical-primary/50 transition-all`}
                    >
                        {user.initials}
                    </div>
                </button>

                <!-- Dropdown Menu -->
                <div
                    class="absolute right-0 top-full mt-2 w-56 bg-clinical-surface border border-gray-700 rounded-md shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 transform origin-top-right overflow-hidden"
                >
                    <div class="py-1">
                        <!-- Appearance Submenu in Main Menu -->
                        <div
                            class="px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider border-b border-gray-700/50 mb-1"
                        >
                            Appearance
                        </div>
                        <div
                            class="px-2 pb-2 border-b border-gray-700/50 mb-1 flex gap-1 justify-between"
                        >
                            {#each ["system", "dark", "light"] as m}
                                <button
                                    class="flex-1 px-2 py-1.5 text-xs rounded-md border transition-all capitalize {themeStore.mode ===
                                    m
                                        ? 'bg-clinical-primary/20 border-clinical-primary text-clinical-primary'
                                        : 'border-transparent text-gray-400 hover:bg-white/5'}"
                                    on:click={() => themeStore.setMode(m)}
                                >
                                    {m}
                                </button>
                            {/each}
                        </div>

                        <a
                            href="/app/profile"
                            class="block px-4 py-2 text-sm text-gray-300 hover:bg-gray-700 hover:text-white"
                        >
                            Profile Settings
                        </a>
                        <a
                            href="/logout"
                            class="block px-4 py-2 text-sm text-red-400 hover:bg-red-900/20 hover:text-red-300"
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
            class="w-[60px] flex flex-col items-center py-4 bg-clinical-surface border-r border-gray-800 shrink-0 z-10 gap-6"
        >
            <!-- Nav Icons -->
            <a
                href="/app"
                class="p-2 rounded-lg text-clinical-primary bg-clinical-primary/10 hover:bg-clinical-primary/20 transition-all group relative"
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
                class="p-2 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-white/5 transition-all group relative"
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
                class="p-2 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-white/5 transition-all group relative"
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
                href="/app/ai"
                class="p-2 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-white/5 transition-all group relative"
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

            <div class="flex-1"></div>

            <!-- Echo (Feedback) -->
            <div class="mb-4">
                <Echo />
            </div>

            <!-- Settings -->
            <a
                href="/app/settings"
                class="p-2 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-white/5 transition-all"
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
                href="/logout"
                class="p-2 rounded-lg text-gray-500 hover:text-red-400 hover:bg-red-500/10 transition-all mb-2"
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
            <slot />
        </main>
    </div>
</div>
