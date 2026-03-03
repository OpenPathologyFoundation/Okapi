# Seed Data

Versioned seed datasets for populating Okapi dev/demo/test environments. Inputs are tool-agnostic JSON files consumed by the Spring Boot Admin API, `psql`, or the Keycloak CLI.

## Directory Structure

```
seed/
  identities/
    xenonym-identities.v1.json            # IAM identities (users + service accounts)
    xenonym-identities.v1.schema.json
  patients/
    xenonym-azure-vale-9728.json          # 50 synthetic patients
    xenonym-patients.v1.schema.json
    seed-patients.sql                     # psql alternative
  team/
    team-professionals.v1.json            # 11 clinical professionals
    team-professionals.v1.schema.json
  permission-groups/
    demo-permission-groups.v1.json        # RBAC group definitions
  wsi/
    wsi-test-cases.v1.json               # 12 clinical cases, 27 slides
    wsi-test-cases.v1.schema.json
    case-assignments.v1.json             # Pathologist assignments
    case-assignments.v1.schema.json
    manifest.json                        # Slide file manifest (32 entries)
    seed-wsi-test-cases.sql              # psql alternative
  wsi-edu/
    wsi-edu-cases.v1.json               # 10 educational teaching cases
    edu-curator-assignments.v1.json      # Curator assignments
    seed-wsi-edu-cases.sql              # psql alternative
    manifest.json                       # Edu slide file manifest
  keycloak/
    seed-keycloak-users.sh              # Keycloak user provisioning
    out/keycloak-user-map.tsv           # Generated user map
```

## Conventions

- Files are versioned: `*.v1.json`, `*.v2.json`, etc.
- `*.schema.json` validates the corresponding dataset (JSON Schema draft 2020-12).
- Seed files are **not** applied by Flyway migrations — they are triggered manually.

## Seeding Sequence

Run these steps in order. Two paths are provided: **browser console** (easiest if the app is already running) and **CLI** (for scripted/headless environments).

### Prerequisites

Start all the services needed for the full system. From the workspace root:

```bash
# 1. Start infrastructure (Keycloak + Postgres)
docker compose -f Okapi/auth-system/docker-compose.yml up -d

# 2. Provision Keycloak users (creates IdP accounts + groups)
./Okapi/seed/keycloak/seed-keycloak-users.sh

# 3. Start the Auth System / Spring Boot backend (applies Flyway migrations)
cd Okapi/auth-system
set -a && source .env && set +a
./gradlew bootRun
# Leave running in this terminal

# 4. Start the Okapi web client (new terminal)
cd Okapi/web-client
npm run dev -- --host
# Runs on http://localhost:5173
```

> **Note:** The seed endpoints live on the Spring Boot backend (`:8080`), but they
> require an authenticated session. The easiest way to get one is to log into the
> **Okapi web app** (the SvelteKit orchestrator) — not the Keycloak admin console.
> Logging in through the web app creates a Spring Security session with the correct
> roles and sets the `XSRF-TOKEN` cookie needed for POST requests.

### Step-by-step

All `POST` endpoints require an authenticated session with `ROLE_ADMIN` (mapped from the `Okapi_Admins` Keycloak group) and a CSRF token.

| # | Endpoint | What it seeds |
|---|----------|---------------|
| 1 | `POST /admin/seed/identities` | IAM identity records |
| 2 | `POST /admin/seed/patients` | Xenonym patient demographics |
| 3 | `POST /admin/seed/cases` | 12 clinical WSI cases + 27 slides |
| 4 | `POST /admin/seed/case-assignments` | Pathologist-to-case assignments |
| 5 | `POST /admin/seed/worklist-sync` | Builds worklist from `wsi.cases` |
| 6 | `POST /admin/seed/edu-cases` | 10 educational teaching cases |
| 7 | `POST /admin/seed/edu-curators` | Curator assignments for edu cases |

### Browser Console Path (recommended)

1. Open the Okapi web app at `http://localhost:5173` (or `http://localhost:8443` if using nginx)
2. Log in as a user in the `Okapi_Admins` group (e.g., username `hlemsesor`, password `test`)
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

Requires a valid session cookie and CSRF token. Log in via the Okapi web app first, then copy the `JSESSIONID` and `XSRF-TOKEN` cookies from your browser's developer tools (Application → Cookies):

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

For clinical and educational WSI cases, you can bypass the API and load directly via SQL:

```bash
source Okapi/auth-system/.env
PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f Okapi/seed/wsi/seed-wsi-test-cases.sql

PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f Okapi/seed/wsi-edu/seed-wsi-edu-cases.sql
```

## Verification

```sql
-- IAM identities
SELECT username, display_name, account_type FROM core.identities ORDER BY username;

-- Patients
SELECT mrn, display_name, sex FROM core.patients ORDER BY mrn;

-- Clinical cases (expect 12)
SELECT case_id, collection, status FROM wsi.cases WHERE collection = 'clinical' ORDER BY case_id;

-- Slides (expect 27)
SELECT s.slide_id, s.format FROM wsi.slides s ORDER BY s.slide_id;

-- Case assignments
SELECT ca.accession_number, i.username, ca.designation
  FROM wsi.case_assignments ca
  JOIN core.identities i ON i.identity_id = ca.identity_id
  ORDER BY ca.accession_number, ca.sequence;

-- Worklist
SELECT accession_number, status, assigned_to_display FROM core.worklist_items ORDER BY accession_number;

-- Educational cases (expect 10)
SELECT case_id, metadata->>'teaching_category' AS category FROM wsi_edu.cases ORDER BY case_id;
```

## Keycloak Seeding Details

```bash
./seed/keycloak/seed-keycloak-users.sh
```

- Runs `kcadm.sh` **inside** the Keycloak container, parses `xenonym-identities.v1.json` on the **host** via `python3`.
- Auto-loads `auth-system/.env` (override with `DOTENV_FILE=...`, disable with `SKIP_DOTENV=1`).
- Idempotent: creates missing users/groups, overwrites `seed/keycloak/out/keycloak-user-map.tsv`.
