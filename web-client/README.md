# Starling Web Client

**Orchestrator UI for Clinical Pathology** (project formerly known as Okapi)

The web-client is a SvelteKit application that serves as the clinician-facing orchestrator for the Starling open pathology platform. It provides login, worklist, case detail, and viewer window management. It does **not** render whole slide images directly — instead it launches a separate [Digital Viewer](https://github.com/your-org/large_image/tree/main/digital-viewer) window and communicates with it over a typed `postMessage` bridge.

## Tech Stack

- **Svelte 5** with runes (`$state`, `$derived`, `$effect`)
- **SvelteKit** with server-side rendering disabled (SPA mode)
- **TypeScript** (strict)
- **Tailwind CSS** for styling
- **Vite** as the build tool

## Repository Layout

```
web-client/
├── src/
│   ├── lib/
│   │   ├── csrf.ts                  # CSRF token cookie reader + header helper
│   │   ├── viewer-bridge.ts         # ViewerBridge class (postMessage bridge)
│   │   ├── stores/                  # Reactive stores (Svelte 5 class pattern)
│   │   │   ├── auth.svelte.ts       # Authentication state
│   │   │   ├── admin.svelte.ts      # Admin panel data + fetchJson helper
│   │   │   ├── worklist.svelte.ts   # Worklist data and filters
│   │   │   └── viewer.svelte.ts     # Viewer bridge lifecycle
│   │   ├── types/
│   │   │   └── viewer-bridge.ts     # postMessage protocol types
│   │   └── components/
│   ├── routes/
│   │   └── app/
│   │       ├── +layout.svelte       # App shell with sidebar
│   │       ├── worklist/            # Worklist page
│   │       └── case/[accession]/    # Case detail page
│   └── app.html
├── static/
├── svelte.config.js
├── vite.config.ts
└── tailwind.config.ts
```

## Getting Started

### Prerequisites

- Node.js 20+
- The auth-system must be running for login and token minting

### Development

```bash
# Install dependencies
npm install

# Start the dev server (default port 5173)
npm run dev

# When running behind the nginx proxy, use --host to bind to 0.0.0.0
# so the Docker-based nginx container can reach the dev server
npm run dev -- --host
```

When running behind the nginx reverse proxy (recommended), access the app at `http://localhost:8443` instead of `http://localhost:5173` directly.

### Build

```bash
npm run build
```

### Type Checking

```bash
npx svelte-check --threshold error
```

## Viewer Window Integration

The web-client acts as the **orchestrator** in a two-window architecture. The viewer is a separate Svelte 5 application in the `large_image/digital-viewer` repository.

### How It Works

1. The pathologist navigates to a case detail page (`/app/case/[accession]`)
2. They click **Launch Viewer** which opens a new browser window at `/viewer/orchestrated.html`
3. The `viewerStore` creates a `ViewerBridge` instance that manages the `postMessage` connection
4. The bridge sends the viewer a JWT token, case details, and tile server configuration
5. Ongoing communication handles: token refresh, case switching, heartbeat, and audit events

### Key Files

| File | Purpose |
|------|---------|
| `src/lib/stores/viewer.svelte.ts` | Reactive store owning the `ViewerBridge` singleton. Tracks bridge state, current case, slide count, and errors. Provides `launchViewer()`, `sendCaseChange()`, `closeViewer()`. |
| `src/lib/viewer-bridge.ts` | `ViewerBridge` class managing the `postMessage` channel, heartbeat, token refresh, and audit event batching. |
| `src/lib/csrf.ts` | Reads the `XSRF-TOKEN` cookie and provides a `csrfHeaders()` helper for all state-changing fetch calls. |
| `src/lib/types/viewer-bridge.ts` | Typed postMessage protocol shared between orchestrator and viewer. Defines `OrchestratorMessage`, `ViewerMessage`, `InitPayload`, `ViewerAuditEvent`, and `ViewerLaunchConfig`. |
| `src/routes/app/case/[accession]/+page.svelte` | Case detail page with the Launch Viewer button. Shows viewer-active badge and handles case switching. |

### Viewer Bridge States

The `viewerStore` tracks the viewer lifecycle:

| State | Meaning |
|-------|---------|
| `idle` | No viewer window open |
| `launching` | Window opened, waiting for `viewer:ready` |
| `connected` | Bridge active, heartbeat healthy |
| `disconnected` | Heartbeat missed (>15s), auto-recovery in progress |
| `lost` | No heartbeat for >60s, viewer presumed gone |
| `ended` | Orchestrator closed the connection intentionally |

### postMessage Protocol

**Orchestrator to Viewer:**
- `orchestrator:init` — JWT + case config (sent once after `viewer:ready`)
- `orchestrator:token-refresh` — fresh JWT when current token nears expiry
- `orchestrator:case-change` — switch to a different case
- `orchestrator:logout` — session ending, viewer should show overlay
- `orchestrator:heartbeat` — periodic keepalive (every 5s)

**Viewer to Orchestrator:**
- `viewer:ready` — viewer has mounted and is waiting for init
- `viewer:heartbeat-ack` — response to heartbeat
- `viewer:case-loaded` — case loaded successfully with slide count
- `viewer:error` — error occurred in viewer
- `viewer:audit-event` — batched audit events for backend persistence

### Audit Event Pipeline

Viewer audit events flow: **viewer window** -> `postMessage` -> **orchestrator (ViewerBridge)** -> batched `POST /api/viewer-events` -> **auth-system** -> `audit_event` table.

The `ViewerBridge` batches events for 5 seconds before flushing to the backend.

### CSRF Protection

The auth-system uses Spring Security's `CookieCsrfTokenRepository`, which sets an `XSRF-TOKEN` cookie (readable by JavaScript) on every response. All state-changing requests (POST, PUT, DELETE, PATCH) must include the token value as the `X-XSRF-TOKEN` header.

The `src/lib/csrf.ts` module provides two helpers:
- `getCsrfToken()` — reads the `XSRF-TOKEN` cookie value
- `csrfHeaders()` — returns a headers object with `X-XSRF-TOKEN` set (or empty if no cookie)

These are used in:
- `viewer.svelte.ts` → `mintToken()` (POST `/auth/token`)
- `viewer-bridge.ts` → `flushAuditEvents()` (POST `/api/viewer-events`)
- `admin.svelte.ts` → `fetchJson()` (all admin POST/PUT/DELETE/PATCH calls)

## Environment Variables

The web-client uses SvelteKit's built-in environment handling. Key variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PUBLIC_TILE_SERVER_URL` | Tile server URL (behind proxy) | `/tiles` |
| `PUBLIC_SESSION_SERVICE_URL` | WebSocket session service | `/ws` |
| `PUBLIC_VIEWER_URL` | Viewer entry point | `/viewer/orchestrated.html` |

## Running Behind the Proxy

For the two-window architecture to work, all services must share a single origin. The nginx reverse proxy (`proxy/nginx.dev.conf`) provides this:

```bash
# From the Okapi repository root
docker run --rm -p 8443:8443 \
  --add-host=host.docker.internal:host-gateway \
  -v $(pwd)/proxy/nginx.dev.conf:/etc/nginx/conf.d/default.conf:ro \
  nginx:alpine
```

This maps:
- `/` -> web-client (`:5173`)
- `/viewer/` -> digital-viewer (`:5174`)
- `/tiles/` -> tile server (`:8000`)
- `/ws/` -> session service (`:8765`)
- `/auth/`, `/api/`, `/admin/` -> auth-system (`:8080`)
