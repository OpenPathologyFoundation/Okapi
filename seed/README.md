# Seed data

This directory contains **versioned seed datasets** used to populate Okapi environments (dev/demo/test) with:

- Identities (users/service accounts)
- Permission groups
- Synthetic cases

The goal is to keep seed inputs **tool-agnostic**: the same JSON files can be consumed by different provisioning mechanisms (e.g., DB seed runner, Keycloak importer, future admin API).

## Structure

```
seed/
  identities/
    demo-identities.v1.json
    demo-identities.v1.schema.json
  permission-groups/
    demo-permission-groups.v1.json
  cases/
    synthetic-cases.v1.json
```

## Conventions

- Files are versioned: `*.v1.json`, `*.v2.json`, etc.
- `*.schema.json` documents/validates the corresponding dataset format.
- These files are **not** automatically applied by Flyway baseline migrations.

## Keycloak demo seeding (users + groups)

To provision Keycloak demo users/groups from `seed/identities/demo-identities.v1.json`, use:

```bash
docker compose -f auth-system/docker-compose.yml up -d keycloak
./seed/keycloak/seed-keycloak-users.sh
```

Notes:
- The script runs `kcadm.sh` **inside** the Keycloak container, but parses `demo-identities.v1.json` on the **host** using `python3`.
- The script will auto-load `auth-system/.env` if present (override with `DOTENV_FILE=...` or disable with `SKIP_DOTENV=1`).
- Re-running is intended to be safe: it creates missing users/groups and overwrites `seed/keycloak/out/keycloak-user-map.tsv` each run (no duplicate accumulation).

## Okapi DB seeding (normalized identities) via Admin API

Okapi stores a **local normalized identity record** in Postgres (for local flags/metadata and authorization state), but user accounts remain managed in the IdP (Keycloak).

### Prerequisites

1. Start Keycloak + Postgres:

```bash
docker compose -f auth-system/docker-compose.yml up -d
```

2. Provision demo users/groups in Keycloak (this makes sure subjects exist):

```bash
./seed/keycloak/seed-keycloak-users.sh
```

3. Run the Spring Boot app:

```bash
cd auth-system
./gradlew bootRun
```

4. Provide Keycloak Admin API credentials to the app (used for validation during seeding):

- `OKAPI_KEYCLOAK_ADMIN_PASSWORD=admin` (default for local `docker-compose`)

### Trigger the seed

Endpoint:
- `POST http://localhost:8080/admin/seed/identities`

Authorization:
- Requires Okapi authentication and `ROLE_ADMIN` (mapped from Keycloak group `Okapi_Admins`).

Notes:
- The endpoint reads `seed/identities/demo-identities.v1.json` and upserts by `(provider_id, external_subject)`.
- Spring Security CSRF protection applies to `POST` by default. If you call this from CLI, you must supply a valid session cookie and CSRF token.
- The simplest workflow is to log in via the browser first, then trigger the request using an HTTP client that can reuse your session cookies.

## Next step

Define the permission-group assignment model and extend the seeding to populate `identity_permission_groups` from `seed/permission-groups/demo-permission-groups.v1.json`.
