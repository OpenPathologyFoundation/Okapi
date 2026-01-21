<script lang="ts">
    // src/routes/logout/+page.svelte
    // The "Goodnight" Page
</script>

<div
    class="h-screen w-screen bg-clinical-bg flex items-center justify-center text-center p-8"
>
    <div class="max-w-md space-y-8 animate-in fade-in duration-700">
        <div class="mx-auto flex justify-center">
            <img
                src="/logo-nb.png"
                alt="Okapi"
                class="h-16 w-auto opacity-50 grayscale hover:grayscale-0 transition-all duration-1000"
            />
        </div>

        <div>
            <h1 class="text-3xl font-bold text-clinical-text tracking-tight mb-2">
                Session Securely Terminated.
            </h1>
            <p class="text-clinical-muted">
                Thank you for your contribution to precision diagnostics today.
            </p>
        </div>

        <!-- Divider -->
        <div class="w-16 h-px bg-clinical-border mx-auto"></div>

        <div class="text-sm text-clinical-muted">
            <p class="mb-4">
                You have been signed out of the secure environment.
            </p>
            <a
                href="/"
                class="text-clinical-primary hover:text-clinical-text transition-colors font-medium"
            >
                Return to Login Page &rarr;
            </a>
        </div>
    </div>
</div>
