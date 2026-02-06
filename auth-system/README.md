# Okapi Auth System

An authentication and authorization service for Okapi, built with Spring Boot 4.0.2 and Java 25.

## What this service does

- OIDC login with Keycloak (or any OIDC provider).
- Normalizes external identities into the Okapi Postgres schema.
- Role-based access control via IdP group mapping.
- Device trust, break-glass, and research grants.
- Issues short-lived Okapi authorization JWTs for downstream services (`POST /auth/token`).

## Repo structure (auth-system/)

- `src/main/java/com/okapi/auth`: application code and API endpoints.
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

## Demo provisioning (Keycloak -> Okapi DB)

Okapi follows the pattern:
- Keycloak is the source of truth for accounts and group membership.
- Okapi/Postgres stores normalized identities and local flags/metadata.

### 1) Seed Keycloak demo users and groups

Seed input: `seed/identities/demo-identities.v1.json`

```bash
docker compose -f auth-system/docker-compose.yml up -d keycloak
./seed/keycloak/seed-keycloak-users.sh
```

Notes:
- The script runs `kcadm.sh` inside the Keycloak container (`okapi-keycloak`).
- The script parses the JSON on the host using `python3`.
- Re-running is intended to be safe and overwrites `seed/keycloak/out/keycloak-user-map.tsv`.

### 2) Seed Okapi/Postgres identities (Admin-only)

Endpoint:
- `POST /admin/seed/identities`

What it does:
- Reads `seed/identities/demo-identities.v1.json`
- Validates each identity exists in Keycloak by `subject` (OIDC `sub`)
- Upserts into `identities` by `(provider_id, external_subject)`
- Applies display/name fields and local flags (for example `account_type`, `break_glass_enabled`)
- Writes a summary `audit_events` record

#### Pre-req: you must be logged in as an Admin
The endpoint is protected by `ROLE_ADMIN` (`/admin/**` requires Admin). Ensure your Keycloak user is in `Okapi_Admins`.

#### Easiest way to call it (browser + session cookie)
1. Start the app (`./gradlew bootRun`).
2. Log in via the browser (OIDC redirect flow).
3. Trigger the seed request using an HTTP client that can reuse your browser session cookies (for example, Postman) or by copying the request as cURL from DevTools.

#### cURL example (requires session + CSRF token)
Spring Security protects `POST` requests with CSRF by default.
If you want to call the seed endpoint from CLI, you must supply:
- the authenticated session cookie, and
- the CSRF token (as header or parameter)

```bash
curl -X POST \
  -H 'X-CSRF-TOKEN: <csrf-token>' \
  -b '<session-cookie-jar-or-cookie-header>' \
  http://localhost:8080/admin/seed/identities
```

#### Keycloak Admin API credentials (required by the seed endpoint)
The seed endpoint validates identities against Keycloak via the Keycloak Admin API.
By default it expects a Keycloak admin password via Spring config:

- Property: `okapi.keycloak.admin.password`
- Environment variable form: `OKAPI_KEYCLOAK_ADMIN_PASSWORD`

For local `docker-compose` the default Keycloak admin password is `admin` (see `auth-system/docker-compose.yml`).

## Using this module from other services

This service is a standalone runtime, not a shared library. Other modules should treat it as the identity and authorization source of truth.

### Option A: Web clients (browser session)

1. Users sign in through OIDC (Keycloak -> Okapi Auth).
2. The app can call `GET /auth/me` to read the normalized identity.
3. The app can call `POST /auth/token` to mint an Okapi authorization JWT for downstream services.

### Option B: Service-to-service authorization (Okapi JWT)

If a service needs authorization context, call `POST /auth/token` and pass the token as a Bearer token to downstream services.

Okapi JWT claims:
- `iss` = `OKAPI_JWT_ISSUER` (default `okapi`)
- `sub` = OIDC subject (`external_subject`)
- `roles` and `permissions`
- `okapi_authz_version`
- `exp` and `iat`

Validation details:
- Algorithm: HS256 (shared secret)
- Secret: `OKAPI_JWT_SECRET`
- TTL: `OKAPI_JWT_TTL_SECONDS` (default 600 seconds)

Downstream services should validate the signature, issuer, expiry, and required roles/permissions before handling sensitive routes.

### Example: verify Okapi JWT in a downstream service

The Okapi token uses HS256 and a shared secret. A downstream service can verify it and gate routes by roles/permissions.

```java
package com.okapi.example;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class OkapiJwtVerifier {
    private OkapiJwtVerifier() {
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
- `POST /auth/token` - mint Okapi JWT for downstream services
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

## SAML (optional)

SAML 2.0 support is present but disabled by default. To enable it, follow the steps in this file and uncomment the relevant lines in:
- `auth-system/build.gradle`
- `auth-system/src/main/java/com/okapi/auth/config/SecurityConfig.java`
- `auth-system/src/main/resources/application.yml`
