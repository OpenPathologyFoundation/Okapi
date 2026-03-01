<script lang="ts">
    // src/routes/app/atlas/+page.svelte
    // Atlas — Educational Portal placeholder
</script>

<div class="h-full w-full flex flex-col items-center justify-center p-8">
    <div class="text-center max-w-3xl animate-in fade-in zoom-in duration-500">
        <!-- Hero Icon -->
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
                    d="M5 20C5 20 4 14 5 11C6 8 6 7.5 5.5 6L3 3C3 3 6 5 8.5 5.5C10 5.8 11 6 12 6C13 6 14 5.8 15.5 5.5C18 5 21 3 21 3L18.5 6C18 7.5 18 8 19 11C20 14 19 20 19 20C17 21.5 14.5 22 12 22C9.5 22 7 21.5 5 20Z"
                />
                <circle cx="8.5" cy="12" r="3" stroke-width="1.5" />
                <circle cx="15.5" cy="12" r="3" stroke-width="1.5" />
                <circle cx="8.5" cy="12.5" r="1.5" fill="currentColor" stroke="none" />
                <circle cx="15.5" cy="12.5" r="1.5" fill="currentColor" stroke="none" />
                <circle cx="7.8" cy="11.5" r="0.6" class="fill-white dark:fill-gray-800" stroke="none" />
                <circle cx="14.8" cy="11.5" r="0.6" class="fill-white dark:fill-gray-800" stroke="none" />
                <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.5"
                    d="M11 16l1 2 1-2"
                />
            </svg>
        </div>

        <h1
            class="text-3xl font-bold text-gray-900 dark:text-white tracking-tight mb-2"
        >
            Atlas
        </h1>

        <p class="text-clinical-muted mb-10 text-lg">
            Pathology reference library, training modules, and annotated case
            studies — coming soon.
        </p>

        <!-- Feature Preview Cards -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-left">
            <div
                class="rounded-lg border border-clinical-border bg-clinical-surface p-5"
            >
                <div
                    class="h-9 w-9 rounded-md bg-blue-500/10 flex items-center justify-center mb-3"
                >
                    <svg
                        class="h-5 w-5 text-blue-500"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                        />
                    </svg>
                </div>
                <h3
                    class="text-sm font-semibold text-gray-900 dark:text-white mb-1"
                >
                    Pathology Reference
                </h3>
                <p class="text-xs text-clinical-muted">
                    Tissue types, staining protocols, and diagnostic criteria.
                </p>
            </div>

            <div
                class="rounded-lg border border-clinical-border bg-clinical-surface p-5"
            >
                <div
                    class="h-9 w-9 rounded-md bg-emerald-500/10 flex items-center justify-center mb-3"
                >
                    <svg
                        class="h-5 w-5 text-emerald-500"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                        />
                    </svg>
                </div>
                <h3
                    class="text-sm font-semibold text-gray-900 dark:text-white mb-1"
                >
                    Training Modules
                </h3>
                <p class="text-xs text-clinical-muted">
                    Guided learning paths and competency tracking.
                </p>
            </div>

            <div
                class="rounded-lg border border-clinical-border bg-clinical-surface p-5"
            >
                <div
                    class="h-9 w-9 rounded-md bg-amber-500/10 flex items-center justify-center mb-3"
                >
                    <svg
                        class="h-5 w-5 text-amber-500"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                        />
                    </svg>
                </div>
                <h3
                    class="text-sm font-semibold text-gray-900 dark:text-white mb-1"
                >
                    Case Library
                </h3>
                <p class="text-xs text-clinical-muted">
                    Annotated reference cases for education and review.
                </p>
            </div>
        </div>
    </div>
</div>
