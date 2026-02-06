<script lang="ts">
    import { idleStore } from "$lib/stores/idle.svelte";
</script>

{#if idleStore.warningShown}
    <!-- Backdrop -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
        class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        onkeydown={(e) => {
            if (e.key === "Escape") idleStore.resetTimer();
        }}
    >
        <!-- Modal -->
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg shadow-2xl w-full max-w-sm mx-4 overflow-hidden"
            role="alertdialog"
            aria-modal="true"
            aria-label="Session expiring warning"
        >
            <!-- Header -->
            <div
                class="px-6 pt-5 pb-3 border-b border-clinical-border flex items-center gap-3"
            >
                <div
                    class="h-10 w-10 rounded-full bg-amber-500/15 flex items-center justify-center shrink-0"
                >
                    <svg
                        class="w-5 h-5 text-amber-500"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                    </svg>
                </div>
                <div>
                    <h2 class="text-base font-semibold text-clinical-text">
                        Session Expiring
                    </h2>
                    <p class="text-xs text-clinical-muted">Inactivity detected</p>
                </div>
            </div>

            <!-- Body -->
            <div class="px-6 py-5">
                <p class="text-sm text-clinical-text">
                    Your session will expire in
                    <span class="font-bold text-amber-500 tabular-nums">
                        {idleStore.secondsRemaining}s
                    </span>
                    due to inactivity.
                </p>
                <p class="text-xs text-clinical-muted mt-2">
                    Move your mouse or press any key to stay signed in.
                </p>
            </div>

            <!-- Footer -->
            <div class="px-6 pb-5">
                <button
                    class="w-full px-4 py-2.5 text-sm font-medium text-white bg-clinical-primary hover:bg-clinical-primary/90 rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-clinical-primary/50"
                    onclick={() => idleStore.resetTimer()}
                >
                    Continue Working
                </button>
            </div>
        </div>
    </div>
{/if}
