# Starling Auth System (formerly Okapi Auth System)

An authentication and authorization service for the Starling open pathology platform, built with Spring Boot 4.0.2 and Java 25.

> **Note:** Internal code identifiers — Java packages (`com.starling.auth.*`), Spring configuration namespace (`starling.*`), Keycloak realm, database name (`starling_auth`), and JWT issuer strings — retain the legacy `starling` name for regulatory traceability and build stability. The service name, documentation, and user-facing surfaces use **Starling**.

## What this service does

- OIDC login with Keycloak (or any OIDC provider).
- Normalizes external identities into the Postgres IAM schema.
- Role-based access control via IdP group mapping.
- Device trust, break-glass, and research grants.
- Issues short-lived authorization JWTs for downstream services (`POST /auth/token`).

## Repo structure (auth-system/)

- `src/main/java/com/starling/auth`: application code and API endpoints (legacy package name retained).
- `src/main/resources/db/migration`: Flyway migrations.
- `docker-compose.yml`: local Keycloak + Postgres.
- `keycloak-data/realm.json`: local realm import.
- `.env.example`: local environment variables template.

## Prerequisites

- Java 25.
- Docker + Docker Compose.

## Local development (Keycloak + Postgres + Flyway)

1. Create a local env file:
   ```bash
   cp auth-system/.env.example auth-system/.env
   ```
2. Start infrastructure from the top level directory:
   ```bash
   docker compose -f auth-system/docker-compose.yml up -d
   
   #if you want to clear the database, run:
   docker compose -f auth-system/docker-compose.yml down -v
   ```
3. Export variables into your shell (Gradle does not auto-load `.env`):
   ```bash
   set -a
   source auth-system/.env
   set +a
   ```
4. Run the app:
   ```bash
   cd auth-system
   ./gradlew bootRun
   ```
5. Validate login:
   - Open `http://localhost:8080/auth/me` and complete OIDC login.

### Port conflicts (Postgres)

If you already have Postgres on `5432`, set `POSTGRES_PORT` in `auth-system/.env` (for example `5433`) and restart the compose stack.

## Running tests

This module has Gradle test tasks wired, but there are no committed test classes yet.

- Unit tests: `./gradlew test`
- Integration tests (Docker): `./gradlew integrationTest`

### Coverage reporting (JaCoCo)

Coverage reports are generated automatically after `./gradlew test`.

- HTML report: `auth-system/build/reports/jacoco/test/html/index.html`
- XML report: `auth-system/build/reports/jacoco/test/jacocoTestReport.xml`

## Demo provisioning (Keycloak -> Starling DB)

Starling follows the pattern:
- Keycloak is the source of truth for accounts and group membership.
- Starling/Postgres stores normalized identities and local flags/metadata.

### 1) Seed Keycloak demo users and groups

Seed input: `seed/identities/xenonym-identities.v1.json`

```bash
docker compose -f auth-system/docker-compose.yml up -d keycloak
./seed/keycloak/seed-keycloak-users.sh
```

Notes:
- The script runs `kcadm.sh` inside the Keycloak container (`starling-keycloak`).
- The script parses the JSON on the host using `python3`.
- Re-running is intended to be safe and overwrites `seed/keycloak/out/keycloak-user-map.tsv`.

### 2) Seed Starling/Postgres identities (Admin-only)

Endpoint:
- `POST /admin/seed/identities`

What it does:
- Reads `seed/identities/xenonym-identities.v1.json`
- Validates each identity exists in Keycloak by `subject` (OIDC `sub`)
- Upserts into `identities` by `(provider_id, external_subject)`
- Applies display/name fields and local flags (for example `account_type`, `break_glass_enabled`)
- Writes a summary `audit_events` record

#### Pre-req: you must be logged in as an Admin
The endpoint is protected by `ROLE_ADMIN` (`/admin/**` requires Admin). Ensure your Keycloak user is in `Starling_Admins`.

#### Easiest way to call it (browser + session cookie)
1. Start the app (`./gradlew bootRun`).
2. Log in via the browser (OIDC redirect flow).
3. Trigger the seed request using an HTTP client that can reuse your browser session cookies (for example, Postman) or by copying the request as cURL from DevTools.

#### cURL example (requires session + CSRF token)
Spring Security uses cookie-based CSRF protection (`CookieCsrfTokenRepository`).
On every response, Spring sets an `XSRF-TOKEN` cookie (readable by JavaScript).
To call any POST/PUT/DELETE endpoint from CLI, you must supply:
- the authenticated session cookie, and
- the CSRF token from the `XSRF-TOKEN` cookie as the `X-XSRF-TOKEN` header

```bash
# 1. First, make a GET request to obtain the XSRF-TOKEN cookie
curl -c cookies.txt http://localhost:8080/auth/me

# 2. Extract the XSRF-TOKEN value from cookies.txt, then:
curl -X POST \
  -H 'X-XSRF-TOKEN: <token-from-cookie>' \
  -b cookies.txt \
  http://localhost:8080/admin/seed/identities
```

#### Keycloak Admin API credentials (required by the seed endpoint)
The seed endpoint validates identities against Keycloak via the Keycloak Admin API.
By default it expects a Keycloak admin password via Spring config:

- Property: `starling.keycloak.admin.password`
- Environment variable form: `STARLING_KEYCLOAK_ADMIN_PASSWORD`

For local `docker-compose` the default Keycloak admin password is `admin` (see `auth-system/docker-compose.yml`).

## Using this module from other services

This service is a standalone runtime, not a shared library. Other modules should treat it as the identity and authorization source of truth.

### Option A: Web clients (browser session)

1. Users sign in through OIDC (Keycloak -> Starling Auth).
2. The app can call `GET /auth/me` to read the normalized identity.
3. The app can call `POST /auth/token` to mint an Starling authorization JWT for downstream services.

### Option B: Service-to-service authorization (Starling JWT)

If a service needs authorization context, call `POST /auth/token` and pass the token as a Bearer token to downstream services.

Starling JWT claims:
- `iss` = `STARLING_JWT_ISSUER` (default `starling`)
- `sub` = OIDC subject (`external_subject`)
- `roles` and `permissions`
- `starling_authz_version`
- `exp` and `iat`

Validation details:
- Algorithm: HS256 (shared secret)
- Secret: `STARLING_JWT_SECRET`
- TTL: `STARLING_JWT_TTL_SECONDS` (default 600 seconds)

Downstream services should validate the signature, issuer, expiry, and required roles/permissions before handling sensitive routes.

### Example: verify Starling JWT in a downstream service

The Starling token uses HS256 and a shared secret. A downstream service can verify it and gate routes by roles/permissions.

```java
package com.starling.example;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class StarlingJwtVerifier {
    private StarlingJwtVerifier() {
    }

    public static Claims verify(String token, String sharedSecret, String expectedIssuer) {
        SecretKey key = Keys.hmacShaKeyFor(sharedSecret.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(expectedIssuer)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    public static boolean hasRole(Claims claims, String role) {
        List<String> roles = claims.get("roles", List.class);
        return roles != null && roles.contains(role);
    }

    public static boolean hasPermission(Claims claims, String permission) {
        List<String> permissions = claims.get("permissions", List.class);
        return permissions != null && permissions.contains(permission);
    }
}
```

## Auth endpoints (quick reference)

- `GET /auth/me` - current identity profile
- `POST /auth/token` - mint Starling JWT for downstream services
- Device trust:
  - `GET /auth/devices`
  - `POST /auth/devices`
  - `DELETE /auth/devices/{deviceId}`
  - `POST /auth/devices/revoke-all`
- Break-glass:
  - `POST /api/break-glass`
  - `GET /api/break-glass`
  - `DELETE /api/break-glass/{grantId}`
- Research grants:
  - `POST /api/research-grants`
  - `GET /api/research-grants`
  - `DELETE /api/research-grants/{grantId}`
- Viewer events:
  - `POST /api/viewer-events` - batch ingest viewer audit events (case opened, slide viewed, case closed, annotation created)
- Annotations (stub — returns 501):
  - `GET /api/cases/{accession}/annotations` - list annotations for a case
  - `POST /api/cases/{accession}/annotations` - create annotation
  - `PUT /api/cases/{accession}/annotations/{id}` - update annotation
  - `DELETE /api/cases/{accession}/annotations/{id}` - delete annotation

### Viewer event pipeline

The web-client orchestrator batches audit events from the viewer window and flushes them to `POST /api/viewer-events`. Each event is validated against an allowlist (`VIEWER_CASE_OPENED`, `VIEWER_SLIDE_VIEWED`, `VIEWER_CASE_CLOSED`, `VIEWER_ANNOTATION_CREATED`) and persisted to the `iam.audit_event` table with full actor context from the authenticated session.

## Running behind a reverse proxy

When deployed behind the nginx reverse proxy (at `localhost:8443`), the auth-system needs to trust forwarded headers so that Spring constructs correct OIDC redirect URIs:

```yaml
# application.yml
server:
  forward-headers-strategy: framework
```

This is already configured. The nginx proxy sends `X-Forwarded-Host` and `X-Forwarded-Port` headers so Spring uses the proxy's address (`localhost:8443`) instead of the backend's (`host.docker.internal:8080`).

The Keycloak client configuration must include redirect URIs for each access point:
- `http://localhost:8080/login/oauth2/code/okta` (direct backend)
- `http://localhost:5173/login/oauth2/code/okta` (Vite dev proxy)
- `http://localhost:8443/login/oauth2/code/okta` (nginx proxy)

See `keycloak-data/realm.json` for the full list.

## SAML (optional)

SAML 2.0 support is present but disabled by default. To enable it, follow the steps in this file and uncomment the relevant lines in:
- `auth-system/build.gradle`
- `auth-system/src/main/java/com/starling/auth/config/SecurityConfig.java`
- `auth-system/src/main/resources/application.yml`
