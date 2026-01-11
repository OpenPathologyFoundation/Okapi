<script lang="ts">
    // src/routes/app/+page.svelte
    // The Zero State Dashboard
</script>

<div class="h-full w-full flex flex-col items-center justify-center p-8">
    <div class="text-center max-w-lg animate-in fade-in zoom-in duration-500">
        <!-- Hero Icon (Empty State) -->
        <div
            class="mx-auto h-24 w-24 rounded-full bg-gray-200/50 dark:bg-gray-800/50 flex items-center justify-center mb-6 border border-gray-300 dark:border-gray-700"
        >
            <svg
                class="h-10 w-10 text-gray-400 dark:text-gray-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
            >
                <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.5"
                    d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                />
            </svg>
        </div>

        <h1
            class="text-3xl font-bold text-gray-900 dark:text-white tracking-tight mb-2"
        >
            Ready to Orchestrate.
        </h1>

        <p class="text-clinical-muted mb-10 text-lg">
            Select a worklist from the left, or use the search bar above to
            begin your session.
        </p>

        <!-- Primary CTA -->
        <a
            href="/app/worklist"
            class="inline-flex items-center gap-2 rounded-md bg-clinical-primary px-6 py-3 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-clinical-primary transition-all"
        >
            <svg
                class="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
            >
                <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
                />
            </svg>
            Go to My Daily Worklist
        </a>
    </div>
</div>
