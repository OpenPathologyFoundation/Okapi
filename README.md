<div align="center">
  <img src="assets/card.jpg" alt="Okapi Logo" width="600">
</div>

Okapi is a cloud-native platform designed to modernize Anatomic Pathology workflows by bridging the gap between hospital Laboratory Information Systems (LIS) and advanced AI decision support tools.

---

## 🎯 Purpose

Okapi serves as the "Orchestration Kernel" that enables:
-   **AI-Assisted Diagnostics**: Seamlessly integrating AI suggestions into the pathologist's workflow.
-   **Interoperability**: Connecting on-premise hospital networks (Traditional AP LIS, Cerner) with cloud-hosted utilities via HL7/FHIR.
-   **Clinical Decision Support (CDS)**: Automating routine tasks to reduce burnout and error rates.

## 🏗️ System Architecture

Okapi uses a **two-window orchestrator + viewer architecture** for clinical pathology:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         nginx reverse proxy (:8443)                          │
│   /              → Okapi web-client (SvelteKit)  ← orchestrator window      │
│   /viewer/       → Digital viewer (Svelte 5)     ← viewer window            │
│   /tiles/        → Large Image tile server (Python/FastAPI)                  │
│   /ws            → Session awareness service (Node.js WebSocket)             │
│   /oauth2/       → Okapi auth-system (Spring Security OIDC initiation)      │
│   /login/oauth2/ → Okapi auth-system (OIDC callback)                        │
│   /logout        → Okapi auth-system (Spring Security logout)               │
│   /auth/         → Okapi auth-system (Spring Boot API)                      │
│   /api/          → Okapi auth-system (viewer events, annotations)           │
│   /admin/        → Okapi auth-system (admin API)                            │
└───────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┘
        │              │              │              │              │
 ┌──────┴──────┐ ┌─────┴─────┐ ┌─────┴─────┐ ┌─────┴─────┐ ┌─────┴─────┐
 │ web-client  │ │  viewer   │ │tile server│ │  auth-sys │ │  session  │
 │ :5173       │ │  :5174    │ │  :8000    │ │  :8080    │ │  :8765    │
 └──────┬──────┘ └─────┬─────┘ └───────────┘ └───────────┘ └───────────┘
        │              │
        └──postMessage─┘
        (typed bridge)
```

The **orchestrator** (web-client) manages login, worklist, and case navigation. When a pathologist opens a case, the orchestrator launches a **viewer** window that renders whole slide images via OpenSeadragon. The two windows communicate through a typed `postMessage` bridge for:
- JWT token provisioning and refresh
- Case switching
- Heartbeat-based lifecycle management (orphan detection)
- Audit event forwarding

The **session awareness service** implements the Focus Declaration Protocol (FDP) — a WebSocket hub that tracks which cases each user has open across viewer windows. It raises clinically important safety warnings when a pathologist has the same case open in multiple contexts, helping prevent diagnostic errors.

The viewer window is a separate application (`large_image/digital-viewer`) with its own repository. See the Digital Viewer README for viewer-specific documentation.

## 🗺️ Repository Structure

| Directory | Description |
|-----------|-------------|
| **[`auth-system/`](auth-system/README.md)** | **Authentication & Audit Service**<br>Identity & Access Management backend. Handles OIDC/SAML login, Identity Normalization, RBAC, viewer event auditing, and annotation API stubs.<br>_Tech: Java 25, Spring Boot 3.5, Keycloak, Postgres_ |
| **[`web-client/`](web-client/README.md)** | **Orchestrator Web Client**<br>The clinician-facing SvelteKit application. Provides login, worklist, case detail, and viewer window management.<br>_Tech: Svelte 5, SvelteKit, TypeScript, Tailwind CSS_ |
| **[`proxy/`](proxy/)** | **Development Reverse Proxy**<br>nginx config that routes all services through a single origin (`:8443`) to avoid CORS issues during development.<br>_See `proxy/nginx.dev.conf`_ |
| **[`seed/`](seed/README.md)** | **Demo Data Seeding**<br>Scripts and datasets for provisioning Keycloak users/groups and Okapi identities. |
| **[`qms/`](qms/dhf/00-Index.md)** | **Quality Management System**<br>Design History File (DHF) and SOPs for regulated medical software.<br>_Includes: Requirements (SRS), Design (SDS), Risk Management_ |
| **[`docs/`](docs/)** | **Integration Guides**<br>End-to-end authentication and authorization documentation. |

## 🚀 Getting Started

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 25+ | auth-system backend |
| Docker + Compose | latest | Keycloak, Postgres, nginx proxy |
| Node.js | 20+ | web-client, digital-viewer, session service |
| Python | 3.10+ | Large Image tile server |

### Quick Start (full system)

The system has 7 services. Start them in order:

**1. Infrastructure (Keycloak + Postgres)**
```bash
docker compose -f auth-system/docker-compose.yml up -d
```

**2. Auth System (Spring Boot on :8080)**
```bash
set -a && source auth-system/.env && set +a
cd auth-system && ./gradlew bootRun
```

**3. Tile Server (FastAPI on :8000)**
```bash
# In the large_image repository
source .venv/bin/activate
large_image_server --image-dir /path/to/slides --port 8000
```

```bash
# or with database-backed case routing clinical only:
large_image_server \
    --db-url "postgresql://okapi_service:postgres_dev_password@localhost:5433/okapi_auth" \
    --clinical-root test-cases/clinical \
    --port 8000
```

```bash
# or with database-backed case routing with clinical and edu:
large_image_server --db-url "postgresql://okapi_service:postgres_dev_password@localhost:5433/okapi_auth" --clinical-root test-cases/clinical --edu-root test-cases/edu --port 8000
```



**4. Digital Viewer (Vite on :5174)**
```bash
# In the large_image/digital-viewer directory
# VITE_BASE=/viewer/ is required so Vite serves assets under /viewer/
# when accessed through the nginx proxy.
npm install && VITE_BASE=/viewer/ npm run dev -- --port 5174 --host
```

**5. Session Awareness Service (WebSocket on :8765)**
```bash
# In the large_image/digital-viewer directory
npm run dev:session
`

> The session service implements the Focus Declaration Protocol (FDP) Layer 2 —
> it tracks which cases each user has open across viewer windows and raises
> multi-case safety warnings. Without it the viewer still works, but
> cross-window awareness is disabled.

**6. Web Client (SvelteKit on :5173)**
```bash
cd web-client && npm install && npm run dev -- --host
```

> **Note:** The `--host` flag is required so that Vite binds to `0.0.0.0` instead of `localhost`. Without it, the nginx Docker container cannot reach the dev servers via `host.docker.internal`.

**7. Reverse Proxy (nginx on :8443)**
```bash
docker run --rm -p 8443:8443 \
  --add-host=host.docker.internal:host-gateway \
  -v $(pwd)/proxy/nginx.dev.conf:/etc/nginx/conf.d/default.conf:ro \
  nginx:alpine
```

Open `http://localhost:8443` to access the unified application.

### What you should see

1. Login via Keycloak OIDC
2. Worklist with cases (assigned and unassigned)
3. Click a case to open the case detail page
4. Click **Launch Viewer** to open the viewer in a second window
5. The viewer receives a JWT, loads the case slides, and renders tiles via OpenSeadragon
6. Session awareness connects (green indicator) — opening the same case in another window triggers a multi-case safety warning
7. Switching cases in the orchestrator sends a case-change message to the viewer
8. Closing the orchestrator triggers a session-ended overlay in the viewer

### CSRF Protection

The auth-system uses Spring Security's `CookieCsrfTokenRepository` so that the SPA can participate in CSRF protection. On every response, Spring sets an `XSRF-TOKEN` cookie (readable by JavaScript). The web-client reads this cookie and sends it back as the `X-XSRF-TOKEN` header on all state-changing requests (POST, PUT, DELETE). See `web-client/src/lib/csrf.ts` for the helper utilities.

### Reverse Proxy Notes

The nginx reverse proxy routes OIDC flows (`/oauth2/`, `/login/oauth2/`, `/logout`) directly to the Spring Boot backend — these must **not** fall through to SvelteKit. The backend requires `server.forward-headers-strategy: framework` in `application.yml` to trust the `X-Forwarded-Host` and `X-Forwarded-Port` headers from nginx, so that Spring constructs correct OIDC redirect URIs using the proxy's address (`localhost:8443`) instead of the backend's address (`host.docker.internal:8080`).

## 📘 Integration Guide

For an end-to-end walkthrough (setup, login flow, token handling, backend verification, role checks, and deployment notes), see:
- [OIDC Authentication & JWT Authorization Guide](docs/OIDC-Authentication-JWT-Authorization-Guide.md)

### Demo provisioning (dev)

If you want a fully working local demo with:
- demo users/groups in Keycloak, and
- normalized identities populated in Okapi/Postgres,

see:
- `auth-system/README.md` (end-to-end local run + Admin seeding endpoint)
- `seed/README.md` (seed datasets + Keycloak seeding script)

## 📜 Quality & Compliance

Okapi follows a strict Quality Management System (QMS) compliant with medical software standards.

-   **Design History File**: Start with the [DHF Index](qms/dhf/00-Index.md) to understand the system definition.
-   **Contributing**: All changes must follow the [Git Contribution Workflow (SOP-DocControl)](sops/SOP-DocControl.md).
