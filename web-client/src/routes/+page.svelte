<script lang="ts">
  // src/routes/+page.svelte
  // Landing Page / Login Portal
</script>

<div
  class="flex h-screen w-screen overflow-hidden text-clinical-text bg-clinical-bg font-sans"
>
  <!-- Left Half: The Brand Canvas -->
  <div
    class="hidden lg:flex w-1/2 relative bg-gray-900 border-r border-gray-800"
  >
    <!-- Abstract Digital H&E Background Layer -->
    <div class="absolute inset-0 opacity-40">
      <!-- Placeholder for specialized graphic - using a CSS pattern for 'Digital Tissue' vibe -->
      <div
        class="w-full h-full bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-clinical-primary/20 via-gray-900 to-black"
      ></div>
      <div class="absolute inset-0 bg-[url('/grid.svg')] opacity-10"></div>
      <!-- Optional faint grid -->
    </div>

    <!-- Content Overlay -->
    <div class="relative z-10 p-12 flex flex-col justify-between h-full">
      <!-- Logo Area -->
      <div class="flex items-center gap-3">
        <img src="/logo-nb.png" alt="Okapi Logo" class="h-16 w-auto" />
        <span class="text-2xl font-bold tracking-tight text-white">Okapi</span>
      </div>

      <!-- Hero Typography -->
      <div class="mb-24">
        <h1
          class="text-5xl font-extrabold tracking-tight text-white leading-tight mb-6"
        >
          Orchestrating <br />
          <span class="text-clinical-primary">Precision Diagnostics.</span>
        </h1>
        <p
          class="text-xl text-clinical-text/80 font-light max-w-md border-l-2 border-clinical-primary pl-6"
        >
          Unified workflow for Pathology, AI, <br />and Clinical Search.
        </p>
      </div>

      <!-- Footer/Copyright -->
      <div class="text-sm text-gray-500">
        &copy; 2026 Okapi Health. FDA Clearance Pending.
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
        <h2 class="text-3xl font-bold text-white tracking-tight">
          Welcome Back
        </h2>
        <p class="mt-2 text-sm text-clinical-muted">
          Please identify yourself to access the clinical environment.
        </p>
      </div>

      <!-- SSO Button (Primary) -->
      <button
        type="button"
        class="group relative flex w-full justify-center items-center gap-3 rounded-md bg-white px-4 py-3 text-sm font-semibold text-gray-900 hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white transition-all shadow-md"
      >
        <!-- Mock Provider Icon (e.g. Microsoft/Okta) -->
        <svg class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
          <path
            d="M10.5 2h-8c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h8V2zm10.5 0h-8v20h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"
            class="text-gray-400 group-hover:text-clinical-primary transition-colors"
          />
          <!-- Simple Door Icon as placeholder for SSO -->
        </svg>
        <span>Continue with Institutional ID</span>
      </button>

      <!-- Divider -->
      <div class="relative">
        <div class="absolute inset-0 flex items-center" aria-hidden="true">
          <div class="w-full border-t border-gray-700"></div>
        </div>
        <div class="relative flex justify-center text-sm font-medium leading-6">
          <span
            class="bg-clinical-surface px-6 text-gray-400 hover:text-gray-300 cursor-pointer select-none"
            >Or sign in with email</span
          >
        </div>
      </div>

      <!-- Email Forms (Secondary - Hidden by default in concept, visible here for dev) -->
      <form class="space-y-6 mt-6" action="/app" method="GET">
        <div>
          <label
            for="email"
            class="block text-sm font-medium leading-6 text-white"
            >Email address</label
          >
          <div class="mt-2">
            <input
              id="email"
              name="email"
              type="email"
              autocomplete="email"
              required
              class="block w-full rounded-md border-0 bg-white/5 py-1.5 text-white shadow-sm ring-1 ring-inset ring-gray-700 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 pl-3"
            />
          </div>
        </div>

        <div>
          <div class="flex items-center justify-between">
            <label
              for="password"
              class="block text-sm font-medium leading-6 text-white"
              >Password</label
            >
            <div class="text-sm">
              <a
                href="#"
                class="font-semibold text-clinical-primary hover:text-clinical-primary/80"
                >Forgot password?</a
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
              class="block w-full rounded-md border-0 bg-white/5 py-1.5 text-white shadow-sm ring-1 ring-inset ring-gray-700 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-clinical-primary sm:text-sm sm:leading-6 pl-3"
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
