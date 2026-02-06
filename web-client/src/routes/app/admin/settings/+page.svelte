<script lang="ts">
    import { onMount } from "svelte";
    import { adminStore } from "$lib/stores/admin.svelte";

    let saving = $state(false);
    let saved = $state(false);
    let errorMsg = $state<string | null>(null);
    let localTimeout = $state(15);

    onMount(() => {
        adminStore.loadSessionTimeout().then(() => {
            localTimeout = adminStore.sessionTimeoutMinutes;
        });
    });

    async function save() {
        saving = true;
        saved = false;
        errorMsg = null;
        try {
            await adminStore.updateSessionTimeout(localTimeout);
            saved = true;
            setTimeout(() => (saved = false), 3000);
        } catch (e) {
            errorMsg =
                e instanceof Error ? e.message : "Failed to update timeout";
        } finally {
            saving = false;
        }
    }
</script>

<div class="max-w-2xl">
    <h2 class="text-lg font-semibold text-clinical-text mb-1">Settings</h2>
    <p class="text-sm text-clinical-muted mb-6">
        Site-wide configuration for the Okapi platform.
    </p>

    <!-- Session Idle Timeout -->
    <div
        class="bg-clinical-surface border border-clinical-border rounded-lg p-6"
    >
        <div class="flex items-center gap-3 mb-4">
            <div
                class="h-9 w-9 rounded-lg bg-clinical-primary/10 flex items-center justify-center"
            >
                <svg
                    class="w-5 h-5 text-clinical-primary"
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
                <h3 class="text-sm font-semibold text-clinical-text">
                    Session Idle Timeout
                </h3>
                <p class="text-xs text-clinical-muted">
                    Users will be automatically logged out after this period of
                    inactivity.
                </p>
            </div>
        </div>

        <div class="flex items-center gap-4 mb-4">
            <input
                type="range"
                min="1"
                max="60"
                bind:value={localTimeout}
                class="flex-1 h-2 bg-clinical-border rounded-lg appearance-none cursor-pointer accent-clinical-primary"
            />
            <div class="flex items-center gap-2 shrink-0">
                <input
                    type="number"
                    min="1"
                    max="60"
                    bind:value={localTimeout}
                    class="w-16 px-2 py-1.5 text-sm text-center bg-clinical-bg border border-clinical-border rounded-md text-clinical-text focus:ring-2 focus:ring-clinical-primary/50 focus:border-clinical-primary"
                />
                <span class="text-sm text-clinical-muted">min</span>
            </div>
        </div>

        <div class="flex items-center justify-between">
            <p class="text-xs text-clinical-muted">
                Range: 1–60 minutes. Default: 15 minutes.
            </p>
            <div class="flex items-center gap-3">
                {#if saved}
                    <span class="text-xs text-green-500 font-medium">Saved</span
                    >
                {/if}
                {#if errorMsg}
                    <span class="text-xs text-red-500">{errorMsg}</span>
                {/if}
                <button
                    class="px-4 py-1.5 text-sm font-medium text-white bg-clinical-primary hover:bg-clinical-primary/90 rounded-md transition-colors disabled:opacity-50"
                    onclick={save}
                    disabled={saving}
                >
                    {saving ? "Saving..." : "Save"}
                </button>
            </div>
        </div>
    </div>
</div>
