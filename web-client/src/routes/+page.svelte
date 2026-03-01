<script lang="ts">
  // src/routes/+page.svelte
  // Landing Page / Login Portal
</script>

<div
  class="flex h-screen w-screen overflow-hidden text-clinical-text bg-clinical-bg font-sans"
>
  <!-- Left Half: The Brand Canvas (Always Dark) -->
  <div
    class="hidden lg:flex w-1/2 relative bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 border-r border-slate-700/50"
  >
    <!-- Abstract Digital H&E Background Layer -->
    <div class="absolute inset-0">
      <!-- Subtle gradient overlay -->
      <div
        class="w-full h-full bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-clinical-primary/10 via-transparent to-transparent"
      ></div>
      <!-- Decorative grid pattern (CSS-based) -->
      <div class="absolute inset-0 opacity-5" style="background-image: linear-gradient(to right, rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.1) 1px, transparent 1px); background-size: 40px 40px;"></div>
      <!-- Subtle noise texture feel -->
      <div
        class="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent"
      ></div>
    </div>

    <!-- Content Overlay -->
    <div class="relative z-10 p-12 flex flex-col justify-between h-full">
      <!-- Logo Area -->
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

      <!-- Hero Typography -->
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
          Unified workflow for Pathology, AI, <br />and Clinical Search.
        </p>
      </div>

      <!-- Footer/Copyright -->
      <div class="text-sm text-slate-500">
        &copy; 2026 Open Pathology Foundation. FDA Clearance Pending.
      </div>
    </div>
  </div>

  <!-- Right Half: The Secure Gate -->
  <div
    class="w-full lg:w-1/2 flex items-center justify-center p-8 lg:p-24 bg-clinical-surface"
  >
    <div class="w-full max-w-md space-y-8">
      <!-- Welcome Header -->
      <div class="text-center">
        <h2 class="text-3xl font-bold text-clinical-text tracking-tight">
          Welcome Back
        </h2>
        <p class="mt-2 text-sm text-clinical-muted">
          Please identify yourself to access the clinical environment.
        </p>
      </div>

      <!-- SSO Button (Primary) -->
      <a
        href="/oauth2/authorization/okta"
        class="group relative flex w-full justify-center items-center gap-3 rounded-md bg-clinical-bg px-4 py-3 text-sm font-semibold text-clinical-text hover:bg-clinical-hover focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-clinical-primary transition-all shadow-md border border-clinical-border no-underline"
      >
        <!-- Provider Icon -->
        <svg class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
          <path
            d="M10.5 2h-8c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h8V2zm10.5 0h-8v20h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"
            class="text-clinical-muted group-hover:text-clinical-primary transition-colors"
          />
        </svg>
        <span>Continue with Institutional ID</span>
      </a>

      <!-- Divider -->
      <div class="relative">
        <div class="absolute inset-0 flex items-center" aria-hidden="true">
          <div class="w-full border-t border-clinical-border"></div>
        </div>
        <div class="relative flex justify-center text-sm font-medium leading-6">
          <span
            class="bg-clinical-surface px-6 text-clinical-muted hover:text-clinical-text cursor-pointer select-none"
            >Or sign in with email</span
          >
        </div>
      </div>

      <!-- Email Forms (Secondary - Hidden by default in concept, visible here for dev) -->
      <form class="space-y-6 mt-6" action="/app" method="GET">
        <div>
          <label
            for="email"
            class="block text-sm font-medium leading-6 text-clinical-text"
            >Email address</label
          >
          <div class="mt-2">
            <input
              id="email"
              name="email"
              type="email"
              autocomplete="email"
              required
              class="block w-full rounded-md border-0 bg-clinical-bg py-1.5 text-clinical-text shadow-sm ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 pl-3"
            />
          </div>
        </div>

        <div>
          <div class="flex items-center justify-between">
            <label
              for="password"
              class="block text-sm font-medium leading-6 text-clinical-text"
              >Password</label
            >
            <div class="text-sm">
              <button
                type="button"
                class="font-semibold text-clinical-primary hover:text-clinical-primary/80"
                >Forgot password?</button
              >
            </div>
          </div>
          <div class="mt-2">
            <input
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
              required
              class="block w-full rounded-md border-0 bg-clinical-bg py-1.5 text-clinical-text shadow-sm ring-1 ring-inset ring-clinical-border placeholder:text-clinical-muted focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 pl-3"
            />
          </div>
        </div>

        <div>
          <a
            href="/app"
            class="flex w-full justify-center rounded-md bg-clinical-primary/10 px-3 py-1.5 text-sm font-semibold leading-6 text-clinical-primary shadow-sm hover:bg-clinical-primary/20 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-clinical-primary border border-clinical-primary/50 transition-all"
          >
            Sign in (Dev Override)
          </a>
        </div>
      </form>
    </div>
  </div>
</div>
