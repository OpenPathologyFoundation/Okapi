# Okapi Auth System

A robust, enterprise-grade authentication and authorization backend for Okapi, built with **Spring Boot 3.5.6** and **Java 25**.

## 🚀 Features

-   **Java 25 & Spring Boot 3.5.6**: Leveraging the latest Java features with a stable, compatible Spring Boot release.
-   **OIDC Authentication**: Fully integrated with Keycloak (or any OIDC provider like Okta, Auth0) for secure login.
-   **Identity Normalization**: Automatic mapping of external identities (from IdP) to internal `Identity` objects.
-   **Role-Based Access Control (RBAC)**: Maps IdP groups (e.g., `Okapi_Pathologists`) to internal roles (`PATHOLOGIST`, `ADMIN`).
-   **Comprehensive Testing**: Includes unit tests and Docker-based integration tests using a local Keycloak instance.

---

## 🛠 Project Structure

-   `src/main/java/com/okapi/auth`
    -   `config`: Security configuration (`SecurityFilterChain`).
    -   `model`: `Identity` and `Role` definitions.
    -   `service`: `CustomOidcUserService` (Auto-User Creation) and `UserRoleMapper`.
    -   `controller`: Endpoints like `/auth/me` (User Profile) and `/login`.
-   `src/test/java`: Extensive test suite.
    -   `integration/KeycloakIntegrationTest`: Verifies OIDC flow against a real (Dockerized) Keycloak.

---

## 🧪 Running Tests

This project includes a Docker Compose environment for testing against a real Keycloak instance.

### Prerequisites

-   Docker & Docker Compose running.
-   Java 25 installed.

### Steps

1.  **Start Keycloak**:
    ```bash
    docker compose up -d
    ```
    *(This starts Keycloak on port 8180 and automatically imports the `okapi` realm)*

2.  **Run Tests**:
    ```bash
    ./gradlew test
    ```
    *(You will see detailed DEBUG logs showing the connection to Keycloak:8180)*

---

## ▶️ Running locally (Keycloak + Postgres + Flyway)

This module expects **PostgreSQL** and an **OIDC IdP** (Keycloak for local dev). The database schema is managed by **Flyway** migrations on application startup.

### Prerequisites

- Docker + Docker Compose
- Java 25
- A local `.env` for dev values (see `auth-system/.env`)

### Start infrastructure

From the repository root:

```bash
docker compose -f auth-system/docker-compose.yml up -d
```

This starts:
- Keycloak at `http://localhost:8180` (imports realm from `auth-system/keycloak-data/realm.json`)
- Postgres (host port from `POSTGRES_PORT`, e.g. `5433`)

### Run the app

From `auth-system/`:

```bash
./gradlew bootRun
```

On startup you should see Flyway apply migrations (e.g. `v1`, `v2`, ...). If you change migrations and want a clean dev DB, reset the Docker volume:

```bash
docker compose -f auth-system/docker-compose.yml down -v
docker compose -f auth-system/docker-compose.yml up -d
```

### Environment variables

The app reads configuration from environment variables (for local dev these typically come from `auth-system/.env`). Key variables:

- OIDC:
  - `OIDC_ISSUER_URI` (example: `http://localhost:8180/realms/okapi`)
  - `OIDC_CLIENT_ID`
  - `OIDC_CLIENT_SECRET`
- Postgres:
  - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`

Important: **Gradle does not auto-load `.env`**. If you run `bootRun` from a shell that does not export these vars, Spring will fall back to defaults (e.g. port `5432`).

To check loaded .env variables you can run:

```bash
auth-system % set -a           
source ./.env
set +a

env | grep -E '^(POSTGRES|OIDC)_'
```

---

## 🧬 Demo provisioning (Keycloak → Okapi DB)

Okapi follows the pattern:
- **Keycloak** is the source of truth for accounts and group membership.
- **Okapi/Postgres** stores normalized identities and local flags/metadata.

### 1) Seed Keycloak demo users and groups

Seed input: `seed/identities/demo-identities.v1.json`

```bash
docker compose -f auth-system/docker-compose.yml up -d keycloak
./seed/keycloak/seed-keycloak-users.sh
```

Notes:
- The script runs `kcadm.sh` inside the Keycloak container (`okapi-keycloak`).
- The script parses the JSON on the host using `python3`.
- Re-running is intended to be safe and overwrites `seed/keycloak/out/keycloak-user-map.tsv` each run.

### 2) Seed Okapi/Postgres identities (Admin-only)

Endpoint:
- `POST /admin/seed/identities`

What it does:
- Reads `seed/identities/demo-identities.v1.json`
- Validates each identity exists in Keycloak by `subject` (OIDC `sub`)
- Upserts into `identities` by `(provider_id, external_subject)`
- Applies structured display/name fields and local flags (e.g. `account_type`, `break_glass_enabled`)
- Writes a summary `audit_events` record

#### Pre-req: you must be logged in as an Admin
The endpoint is protected by `ROLE_ADMIN` (`/admin/**` requires Admin). Ensure your Keycloak user is in `Okapi_Admins`.

#### Easiest way to call it (browser + session cookie)
1. Start the app (`./gradlew bootRun`).
2. Log in via the browser (OIDC redirect flow).
3. Trigger the seed request using an HTTP client that can reuse your browser session cookies (e.g. Postman) or by copying the request as cURL from DevTools.

#### cURL example (requires session + CSRF token)
Spring Security protects `POST` requests with CSRF by default.
If you want to call the seed endpoint from CLI, you must supply:
- the authenticated session cookie, and
- the CSRF token (as header or parameter)

Example skeleton:

```bash
curl -X POST \
  -H 'X-CSRF-TOKEN: <csrf-token>' \
  -b '<session-cookie-jar-or-cookie-header>' \
  http://localhost:8080/admin/seed/identities
```

If you prefer a fully automated CLI workflow, we can add a dev-only CSRF helper (or explicitly exempt this endpoint in dev) — but the default behavior is intentionally fail-closed.

#### Keycloak Admin API credentials (required by the seed endpoint)
The seed endpoint validates identities against Keycloak via the Keycloak Admin API.
By default it expects a Keycloak admin password via Spring config:

- Property: `okapi.keycloak.admin.password`
- Environment variable form: `OKAPI_KEYCLOAK_ADMIN_PASSWORD`

For local `docker-compose` the default Keycloak admin password is `admin` (see `auth-system/docker-compose.yml`).

## 🐘 Connecting to the Database

The project runs a Dockerized **PostgreSQL 16** instance.

### Connection Details
-   **Host**: `localhost`
-   **Port**: `5432` (Default, see troubleshooting below)
-   **Database**: `okapi_auth`
-   **Username**: `okapi_service`
-   **Password**: `postgres_dev_password` (defined in `.env`)

### ⚠️ Troubleshooting: Port Conflicts
If you have a **local PostgreSQL** installation running on port `5432`, Docker may fail to bind or you might accidentally connect to your local DB instead of the container.

**Solution**: Change the Docker mapping in your `.env` file to use a different host port (e.g., `5433`):

```bash
# .env
POSTGRES_PORT=5433
```

Then restart Docker:
```bash
docker compose down
docker compose up -d
```

You can now connect via **`localhost:5433`** while the container internally uses 5432.

---

## 🔐 Enabling Enterprise SAML Integration

The system is pre-wired for SAML 2.0 but it is currently disabled to allow for OIDC-first development. Follow these strict steps to enable SAML for enterprise accounts.

### Step 1: Add Dependency
Open `build.gradle` and **uncomment** line 22:

```groovy
// FROM:
// implementation 'org.springframework.boot:spring-boot-starter-saml2-service-provider'

// TO:
implementation 'org.springframework.boot:spring-boot-starter-saml2-service-provider'
```

### Step 2: Enable Security Chain
Open `src/main/java/com/okapi/auth/config/SecurityConfig.java` and **uncomment** line 29, adding the import `org.springframework.security.config.Customizer`:

```text
// FROM:
// .saml2Login(Customizer.withDefaults()); // Enable when metadata is ready

// TO:
.saml2Login(org.springframework.security.config.Customizer.withDefaults());
```

### Step 3: Configure Metadata
Open `src/main/resources/application.yml` and **uncomment** lines 15-20. Ensure you provide the valid Metadata URL from your Identity Provider (IdP).

```yaml
# FROM:
#      saml2:
#        relying-party:
#          registration:
#            okapi-saml:
#              asserting-party:
#                metadata-uri: ${SAML_IDP_METADATA_URL}

# TO:
      saml2:
        relying-party:
          registration:
            okapi-saml:
              asserting-party:
                metadata-uri: ${SAML_IDP_METADATA_URL}
```

### Step 4: Environment Variables
Add the metadata URL to your `.env` file or environment variables:

```bash
SAML_IDP_METADATA_URL=https://your-idp.com/metadata.xml
```

### Step 5: Verification
Run the application. Spring Security will automatically generate the Service Provider (SP) metadata at:
`http://localhost:8080/saml2/service-provider-metadata/okapi-saml`

Share this URL (or the XML content) with your Enterprise Identity Provider to complete the trust establishment.
