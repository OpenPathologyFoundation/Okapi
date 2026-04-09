# Seed Data

Versioned seed datasets for populating Starling (formerly Okapi) dev/demo/test environments. Inputs are tool-agnostic JSON files consumed by the Spring Boot Admin API, `psql`, or the Keycloak CLI.

## Directory Structure

```
seed/
  identities/
    xenonym-identities.v1.json            # IAM identities (consumed by /admin/seed/identities AND seed-keycloak-users.sh)
    xenonym-identities.v1.schema.json
  patients/
    xenonym-azure-vale-9728.json          # 50 synthetic patients (consumed by /admin/seed/patients)
    xenonym-patients.v1.schema.json
    seed-patients.sql                     # psql alternative (no /admin API call required)
  wsi/
    wsi-test-cases.v1.json                # 12 clinical cases, 27 slides (consumed by /admin/seed/cases)
    wsi-test-cases.v1.schema.json
    case-assignments.v1.json              # Pathologist assignments (consumed by /admin/seed/case-assignments)
    case-assignments.v1.schema.json
    manifest.json                         # Slide file manifest (32 entries; consumed by the tile server)
    seed-wsi-test-cases.sql               # psql alternative
  wsi-edu/
    wsi-edu-cases.v1.json                 # 10 educational teaching cases (/admin/seed/edu-cases)
    edu-curator-assignments.v1.json       # Curator assignments (/admin/seed/edu-curators)
    seed-wsi-edu-cases.sql                # psql alternative
    manifest.json                         # Edu slide file manifest
  keycloak/
    seed-keycloak-users.sh                # Keycloak user provisioning
    out/keycloak-user-map.tsv             # Generated: written by seed-keycloak-users.sh
  team/
    team-professionals.v1.json            # UNUSED — historical; no seed endpoint wired up
    team-professionals.v1.schema.json
  permission-groups/
    demo-permission-groups.v1.json        # UNUSED — historical; no seed endpoint wired up
```

## Conventions

- Files are versioned: `*.v1.json`, `*.v2.json`, etc.
- `*.schema.json` validates the corresponding dataset (JSON Schema draft 2020-12).
- Seed files are **not** applied by Flyway migrations — they are triggered manually.

## Seeding Sequence

Run these steps in order. Two paths are provided: **browser console** (easiest if the app is already running) and **CLI** (for scripted/headless environments).

### Prerequisites

Start all the services needed for the full system. The commands below assume your
current directory is the **workspace root** (the directory that contains `starling/`,
`pelican/`, `willet/`). Note: the repository directory is lowercase `starling/`.

```bash
# 1. Start infrastructure (Keycloak + Postgres)
docker compose -f starling/auth-system/docker-compose.yml up -d

# 2. Start the Auth System / Spring Boot backend (applies Flyway migrations
#    V1–V13 against the fresh starling_auth database)
cd starling/auth-system
set -a && source .env && set +a
./gradlew bootRun
# Leave running in this terminal

# 3. Provision Keycloak users (creates IdP accounts + groups).
#    Run this AFTER step 1 so the Keycloak container exists, and AFTER step 2 is
#    initialized so Flyway has seeded the idp_group_role mapping table. The script
#    is cwd-agnostic — it resolves its own starling/ root, so you can run it from
#    any directory.
./starling/seed/keycloak/seed-keycloak-users.sh

# 4. Start the Starling web client (new terminal)
cd starling/web-client
npm run dev -- --host
# Runs on http://localhost:5173 (direct) or http://localhost:8443 (behind nginx)
```

> **Note:** The seed endpoints live on the Spring Boot backend (`:8080`), but they
> require an authenticated session. The easiest way to get one is to log into the
> **Starling web app** (the SvelteKit orchestrator) — not the Keycloak admin console.
> Logging in through the web app creates a Spring Security session with the correct
> roles and sets the `XSRF-TOKEN` cookie needed for POST requests.
>
> The Vite dev server at `:5173` proxies `/admin/*`, `/auth/*`, `/oauth2/*`, and
> `/login/*` to the Spring backend at `:8080`, so the browser-console snippet below
> works whether you access the app via `:5173` or `:8443`.

### Step-by-step

All `POST` endpoints require an authenticated session with `ROLE_ADMIN` (mapped from the `Starling_Admins` Keycloak group) and a CSRF token. **The order matters** — later steps SELECT from tables populated by earlier ones.

| # | Endpoint | Writes to | Depends on |
|---|----------|-----------|------------|
| 1 | `POST /admin/seed/identities` | `iam.identity` | — |
| 2 | `POST /admin/seed/patients` | `core.patients` | — |
| 3 | `POST /admin/seed/cases` | `wsi.cases`, `wsi.parts`, `wsi.blocks`, `wsi.slides` | #2 (cases lookup `core.patients` by MRN) |
| 4 | `POST /admin/seed/case-assignments` | `wsi.case_pathologists` | #1 and #3 (joins `iam.identity` ↔ `wsi.cases`) |
| 5 | `POST /admin/seed/worklist-sync` | `public.pathology_worklist` | #3 (syncs from `wsi.cases`) |
| 6 | `POST /admin/seed/edu-cases` | `wsi_edu.cases`, `wsi_edu.parts`, etc. | — |
| 7 | `POST /admin/seed/edu-curators` | `wsi_edu.case_curators` | #6 and #1 |

Steps 1 and 2 are independent of each other, so they could be swapped or run in parallel. Every other step has a hard dependency on an earlier one.

### Browser Console Path (recommended)

1. Open the Starling web app at `http://localhost:5173` (or `http://localhost:8443` if using nginx).
2. Log in as a user in the `Starling_Admins` group. Two options, depending on whether you have already run `seed-keycloak-users.sh`:
   - **Always available** (from the imported `realm.json`): username `admin.bombalurina`, password `password`.
   - **After running `seed-keycloak-users.sh`**: username `tsnausfo`, password `StarlingDev!2026` (the default value of `DEMO_PASSWORD_DEFAULT` in the seed script; override with an env var if you need a different one).

   Non-admin seeded users (`hlemsesor`, `ltsindi`, `gmuklus`, etc.) cannot run the seed endpoints — they're in `Starling_Pathologists` / `Starling_Residents` / `Starling_Fellows` and the endpoints require `ROLE_ADMIN`.
3. Open the browser developer console (F12 → Console) and run:

```javascript
// Paste this entire block into the browser console and press Enter.
// It defines a helper, then calls each seed endpoint in order.
// Each step waits for the previous one to finish before proceeding.
(async () => {
  const token = document.cookie
    .split('; ')
    .find(c => c.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];

  async function seed(path) {
    const res = await fetch(path, {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': decodeURIComponent(token) },
      credentials: 'same-origin'
    });
    const body = await res.json();
    console.log(`${path}: ${res.status}`, body);
    return body;
  }

  await seed('/admin/seed/identities');
  await seed('/admin/seed/patients');
  await seed('/admin/seed/cases');
  await seed('/admin/seed/case-assignments');
  await seed('/admin/seed/worklist-sync');
  await seed('/admin/seed/edu-cases');
  await seed('/admin/seed/edu-curators');
  console.log('All seed endpoints complete.');
})();
```

To run individual endpoints one at a time instead, paste just the `seed` function first, then call `await seed('/admin/seed/identities')` etc. separately.

### CLI Path (curl)

Requires a valid session cookie and CSRF token. Log in via the Starling web app first, then copy the `JSESSIONID` and `XSRF-TOKEN` cookies from your browser's developer tools (Application → Cookies):

```bash
# Export cookies from browser (or use curl to authenticate first)
COOKIE="JSESSIONID=...; XSRF-TOKEN=..."
CSRF_TOKEN="..."  # URL-decoded value of XSRF-TOKEN cookie

for endpoint in identities patients cases case-assignments worklist-sync edu-cases edu-curators; do
  curl -s -X POST "http://localhost:8080/admin/seed/$endpoint" \
    -H "Cookie: $COOKIE" \
    -H "X-XSRF-TOKEN: $CSRF_TOKEN" | python3 -m json.tool
done
```

### Alternative: Direct psql (no auth required)

For clinical and educational WSI cases, you can bypass the API and load directly via SQL. Run from the workspace root:

```bash
set -a && source starling/auth-system/.env && set +a

PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f starling/seed/wsi/seed-wsi-test-cases.sql

PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f starling/seed/wsi-edu/seed-wsi-edu-cases.sql
```

Note: the psql path loads `wsi.cases` and `wsi.slides` directly, but does **not** populate `public.pathology_worklist` or `wsi.case_pathologists`. If you go this route, you still need to call `/admin/seed/case-assignments` and `/admin/seed/worklist-sync` via the API (or write equivalent SQL) to get a working worklist.

## Verification

Table locations (in case you wonder why the queries span several schemas):
- **`iam.*`** — identities, roles, idp group mappings, audit events (V1, V2)
- **`core.*`** — patients (V7)
- **`wsi.*`** — clinical cases, parts, blocks, slides, case_pathologists (V6, V8)
- **`wsi_edu.*`** — educational cases (V12)
- **`public.pathology_worklist`** — the worklist read model (V4, V11)

```sql
-- IAM identities (seeded via POST /admin/seed/identities)
SELECT username, display_name, account_type FROM iam.identity ORDER BY username;

-- Patients (POST /admin/seed/patients)
SELECT mrn, display_name, sex FROM core.patients ORDER BY mrn;

-- Clinical cases (POST /admin/seed/cases — expect 12)
SELECT case_id, collection, status FROM wsi.cases WHERE collection = 'clinical' ORDER BY case_id;

-- Slides (also inserted by /admin/seed/cases — expect 27)
SELECT slide_id, format FROM wsi.slides ORDER BY slide_id;

-- Case assignments (POST /admin/seed/case-assignments).
-- Note the table is wsi.case_pathologists (V8; V10 renamed role → designation).
SELECT cp.designation, i.username, c.case_id
  FROM wsi.case_pathologists cp
  JOIN wsi.cases c ON c.id = cp.case_id
  JOIN iam.identity i ON i.id = cp.identity_id
  ORDER BY c.case_id, cp.designation;

-- Worklist (POST /admin/seed/worklist-sync)
SELECT accession_number, status, assigned_to_display
  FROM public.pathology_worklist
  ORDER BY accession_number;

-- Educational cases (POST /admin/seed/edu-cases — expect 10)
SELECT case_id, metadata->>'teaching_category' AS category
  FROM wsi_edu.cases
  ORDER BY case_id;
```

## Keycloak Seeding Details

```bash
./seed/keycloak/seed-keycloak-users.sh
```

- Runs `kcadm.sh` **inside** the Keycloak container, parses `xenonym-identities.v1.json` on the **host** via `python3`.
- Auto-loads `auth-system/.env` (override with `DOTENV_FILE=...`, disable with `SKIP_DOTENV=1`).
- Idempotent: creates missing users/groups, overwrites `seed/keycloak/out/keycloak-user-map.tsv`.
