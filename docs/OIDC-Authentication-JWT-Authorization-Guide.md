# OIDC Authentication & JWT Authorization Guide

This guide explains how detached applications (web UI, backend APIs, optional admin UI) integrate with the Okapi authentication module and Keycloak (OIDC). It covers setup, local testing, token handling, backend verification, and deployment practices.

## Purpose and guarantees (plain language)

What problem this module solves:
- Centralized login (SSO) across multiple apps.
- Consistent identity normalization and role mapping.
- Shared authorization model for downstream services.

Security guarantees and why they hold:
- OIDC/OAuth2 standard flows: well-understood, audited patterns.
- TLS is required: protects tokens and credentials in transit.
- Signed JWTs: integrity and authenticity of tokens.
  - Keycloak tokens use asymmetric signing (JWKS), enabling key rotation without redeploys.
  - Okapi-issued JWTs (via `POST /auth/token`) are signed with a shared secret (HS256) and rotated by updating the secret.
- Short-lived access tokens + refresh strategy: reduces risk if a token leaks.
- Server-side authorization: APIs enforce access with roles/permissions (never trust the UI).

## Architecture and terminology

Use these terms consistently:
- Identity Provider (IdP): Keycloak
- Authorization Server: Keycloak (issues tokens)
- Clients: web app(s), admin app, service-to-service clients
- Resource Server: backend APIs that validate tokens
- OIDC (authentication) and OAuth 2.0 (authorization)
- JWT access token, refresh token, ID token (see definitions below)
- Authorization Code Flow + PKCE for browser apps

Token definitions:
- ID token: who the user is (OIDC identity claims). Do not use for API authorization.
- Access token: what the user can access (roles/scopes/claims). This is sent to APIs.
- Refresh token: used to get a new access token when the old one expires.

## End-to-end flow

### A. First-time setup

1. Start Keycloak locally (see `auth-system/docker-compose.yml`).
2. Create a realm (or use the provided `okapi` realm import).
3. Create clients:
   - Web client (public client, Authorization Code + PKCE)
   - Backend APIs (confidential clients if they call other services)
   - Optional admin app (confidential client)
4. Configure redirect URIs and CORS:
   - Web app redirect: `http://localhost:<web-port>/auth/callback`
   - Admin app redirect: `http://localhost:<admin-port>/auth/callback`
5. Define roles and groups:
   - Example roles: `ADMIN`, `PATHOLOGIST`, `RESEARCHER`
   - Example groups: `Okapi_Admins`, `Okapi_Pathologists`

Configure the auth module (`auth-system`):
- `OIDC_ISSUER_URI` (example: `http://localhost:8180/realms/okapi`)
- `OIDC_CLIENT_ID`
- `OIDC_CLIENT_SECRET`
- `OKAPI_JWT_SECRET` (for Okapi-issued JWTs)
- `OKAPI_JWT_ISSUER`

JWKS endpoint (Keycloak):
- `http://localhost:8180/realms/okapi/protocol/openid-connect/certs`

Frontend environment variables (example):
- `OIDC_ISSUER_URI`
- `OIDC_CLIENT_ID`
- `OIDC_REDIRECT_URI`
- `OIDC_POST_LOGOUT_REDIRECT_URI`

Backend environment variables (example):
- `OIDC_ISSUER_URI`
- `OIDC_AUDIENCE` (if your JWTs include audience)
- `OKAPI_JWT_ISSUER`
- `OKAPI_JWT_SECRET`

Admin app environment variables (example):
- `OIDC_ISSUER_URI`
- `OIDC_CLIENT_ID`
- `OIDC_REDIRECT_URI`

### B. Runtime login flow (redirect-based SSO)

Happy path (browser app):
1. User visits web app and clicks Login.
2. Web app redirects to Keycloak (Authorization Code + PKCE).
3. Keycloak authenticates user and redirects back with a `code`.
4. Web app exchanges `code` for tokens.
   - Recommended: use a backend-for-frontend (BFF) to do this exchange server-side.
5. Web app calls backend APIs with `Authorization: Bearer <access_token>`.

ASCII flow diagram:

```
User -> Web App -> Keycloak -> Web App callback -> (token exchange) -> Web App -> Backend API
  1        2           3                4                  5               6
```

### C. Backend request handling

Backend APIs must validate:
- Token signature:
  - Keycloak access tokens: verify using JWKS (`/protocol/openid-connect/certs`).
  - Okapi-issued JWTs: verify using `OKAPI_JWT_SECRET`.
- `iss` (issuer)
- `aud` (audience, if used)
- `exp` and `nbf` (expiry and not-before)
- Roles/permissions (claims)

Expected responses:
- `401 Unauthorized`: token missing/invalid/expired
- `403 Forbidden`: token valid but lacks required role/permission

### D. Refresh / session expiry

When access tokens expire:
- The client uses the refresh token to obtain a new access token.
- If refresh fails (revoked/expired), prompt the user to log in again.

## Token handling rules

Token sources used in this stack:
- Keycloak access token: used by clients to call APIs that accept IdP tokens directly.
- Okapi JWT (`POST /auth/token`): used when you want a normalized Okapi authorization token for downstream services.
- Okapi JWTs are access tokens only; there is no refresh token. Re-mint them using the authenticated session in the auth module.

Do:
- Prefer httpOnly, secure cookies (BFF pattern) to keep tokens out of JS.
- Use TLS everywhere.
- Validate tokens server-side on every request.
- Log only high-level auth failures (no tokens).

Do not:
- Store tokens in localStorage unless you accept XSS risk and mitigate it.
- Put tokens in URLs or logs.

If you use cookies:
- Set `Secure`, `HttpOnly`, and `SameSite` appropriately.
- Implement CSRF protection for state-changing requests.

## Local testing procedure (copy/paste)

Start infrastructure:
```bash
docker compose -f auth-system/docker-compose.yml up -d
```

Run the auth module:
```bash
set -a
source auth-system/.env
set +a
cd auth-system
./gradlew bootRun
```

Minimal end-to-end checklist:
1. Open Keycloak Admin Console: `http://localhost:8180/admin` (admin/admin).
2. Create a test user and add to `Okapi_Admins` or `Okapi_Pathologists`.
3. Log in via `http://localhost:8080/auth/me`.
4. Mint an Okapi JWT: `POST http://localhost:8080/auth/token`.
5. Call a protected endpoint with the token (a downstream API in your stack).
6. Verify role-based access by using a second user without the role and confirm a `403`.

End-to-end checklist (auth-system RBAC demo):
- Log in with a user in `Okapi_Admins`.
- Call `POST /admin/seed/identities` (requires session + CSRF token) and confirm 200.
- Log in with a user not in `Okapi_Admins`.
- Call the same endpoint and confirm 403.

Troubleshooting:
- Redirect URI mismatch: ensure Keycloak client redirect URIs match your app callback URL.
- Invalid issuer/audience: verify `iss` and `aud` match config.
- Clock skew: ensure system time is correct; adjust allowed skew if needed.
- CORS vs auth failures: CORS failures are browser console errors; auth failures are 401/403 from API.
- 401 vs 403: 401 means token invalid/missing; 403 means token valid but insufficient rights.

## User and role management

Recommended default: Keycloak Admin Console + Admin API.
- Fastest setup and minimal custom code.
- Works well until you need custom UX workflows.

Optional: build a lightweight admin app.
- Separate UI used only by admins.
- Uses Keycloak Admin API via a backend service account.
- Deployed independently (separate port/deploy) to avoid coupling.
- Security model:
  - Admins authenticate via OIDC.
  - Backend uses least-privilege Keycloak admin roles.
  - All changes audited (who changed what and when).

## Deployment and operations notes

Environment configuration:
- Use separate Keycloak realms or separate clients per environment.
- Rotate secrets regularly (client secrets, Okapi JWT secret).

Key rotation:
- Keycloak JWKS keys rotate without service redeploys; resource servers must fetch JWKS and cache it.
- For Okapi JWTs, rotate `OKAPI_JWT_SECRET` and deploy dependent services.

Observability:
- Log request IDs, auth failure reasons, and user IDs (no tokens).
- Track metrics: auth failures, token validation errors, refresh failures.

Production security checklist:
- TLS everywhere.
- Enforce short access token TTLs.
- Use refresh tokens with revocation and rotation.
- Validate issuer, audience, and signature in every API.
- Restrict admin access and audit admin actions.
