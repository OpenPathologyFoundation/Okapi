<script lang="ts">
    let {
        open = false,
        title = "Confirm",
        message = "Are you sure?",
        confirmLabel = "Confirm",
        variant = "danger",
        onconfirm,
        oncancel,
    }: {
        open: boolean;
        title?: string;
        message?: string;
        confirmLabel?: string;
        variant?: "danger" | "warning";
        onconfirm: () => void;
        oncancel: () => void;
    } = $props();

    const confirmClass = $derived(
        variant === "danger"
            ? "bg-red-600 hover:bg-red-700 text-white"
            : "bg-amber-500 hover:bg-amber-600 text-white",
    );
</script>

{#if open}
    <!-- Backdrop -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
        onkeydown={(e) => {
            if (e.key === "Escape") oncancel();
        }}
    >
        <!-- Dialog -->
        <div
            class="bg-clinical-surface border border-clinical-border rounded-lg shadow-xl max-w-md w-full mx-4 p-6"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="confirm-title"
            aria-describedby="confirm-message"
        >
            <h3
                id="confirm-title"
                class="text-sm font-semibold text-clinical-text mb-2"
            >
                {title}
            </h3>
            <p id="confirm-message" class="text-xs text-clinical-muted mb-5">
                {message}
            </p>
            <div class="flex justify-end gap-2">
                <button
                    onclick={oncancel}
                    class="px-3 py-1.5 rounded-md text-xs font-medium text-clinical-muted hover:bg-clinical-hover border border-clinical-border transition-colors"
                >
                    Cancel
                </button>
                <button
                    onclick={onconfirm}
                    class="px-3 py-1.5 rounded-md text-xs font-medium {confirmClass} transition-colors"
                >
                    {confirmLabel}
                </button>
            </div>
        </div>
    </div>
{/if}
