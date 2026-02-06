<script lang="ts">
    // src/routes/logged-out/+page.svelte
    // Post-logout landing page — shown after Keycloak ends the IdP session.
</script>

<div
    class="flex h-screen w-screen overflow-hidden text-clinical-text bg-clinical-bg font-sans"
>
    <!-- Left Half: The Brand Canvas (Always Dark) -->
    <div
        class="hidden lg:flex w-1/2 relative bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 border-r border-slate-700/50"
    >
        <div class="absolute inset-0">
            <div
                class="w-full h-full bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-clinical-primary/10 via-transparent to-transparent"
            ></div>
            <div
                class="absolute inset-0 opacity-5"
                style="background-image: linear-gradient(to right, rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.1) 1px, transparent 1px); background-size: 40px 40px;"
            ></div>
            <div
                class="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent"
            ></div>
        </div>

        <div class="relative z-10 p-12 flex flex-col justify-between h-full">
            <div class="flex items-center gap-3">
                <img
                    src="/logo-nb.png"
                    alt="Okapi Logo"
                    class="h-16 w-auto drop-shadow-lg"
                />
                <span
                    class="text-2xl font-bold tracking-tight text-white drop-shadow-sm"
                    >Okapi</span
                >
            </div>

            <div class="mb-24">
                <h1
                    class="text-5xl font-extrabold tracking-tight text-white leading-tight mb-6 drop-shadow-sm"
                >
                    Orchestrating <br />
                    <span
                        class="text-clinical-primary drop-shadow-[0_0_20px_rgba(59,130,246,0.3)]"
                        >Precision Diagnostics.</span
                    >
                </h1>
                <p
                    class="text-xl text-slate-300 font-light max-w-md border-l-2 border-clinical-primary/70 pl-6"
                >
                    Unified workflow for Pathology, AI, <br />and Clinical
                    Search.
                </p>
            </div>

            <div class="text-sm text-slate-500">
                &copy; 2026 Open Pathology Foundation. FDA Clearance Pending.
            </div>
        </div>
    </div>

    <!-- Right Half: Signed-Out Confirmation -->
    <div
        class="w-full lg:w-1/2 flex items-center justify-center p-8 lg:p-24 bg-clinical-surface"
    >
        <div class="w-full max-w-md space-y-8 text-center">
            <!-- Signed Out Icon -->
            <div class="flex justify-center">
                <div
                    class="h-16 w-16 rounded-full bg-green-500/10 flex items-center justify-center"
                >
                    <svg
                        class="w-8 h-8 text-green-500"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M5 13l4 4L19 7"
                        />
                    </svg>
                </div>
            </div>

            <!-- Heading -->
            <div>
                <h2
                    class="text-3xl font-bold text-clinical-text tracking-tight"
                >
                    Signed Out
                </h2>
                <p class="mt-3 text-sm text-clinical-muted leading-relaxed">
                    You have been securely signed out of Okapi.<br />
                    Your session and credentials have been cleared.
                </p>
            </div>

            <!-- Sign In Again Button -->
            <a
                href="/oauth2/authorization/okta"
                class="group relative flex w-full justify-center items-center gap-3 rounded-md bg-clinical-bg px-4 py-3 text-sm font-semibold text-clinical-text hover:bg-clinical-hover focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-clinical-primary transition-all shadow-md border border-clinical-border no-underline"
            >
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
                    <path
                        d="M10.5 2h-8c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h8V2zm10.5 0h-8v20h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"
                        class="text-clinical-muted group-hover:text-clinical-primary transition-colors"
                    />
                </svg>
                <span>Sign in with Institutional ID</span>
            </a>

            <!-- PHI Notice -->
            <p class="text-xs text-clinical-muted pt-4">
                Protected Health Information &mdash; Authorized Access Only
            </p>
        </div>
    </div>
</div>
