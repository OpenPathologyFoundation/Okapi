# 01-Auth-Architecture

---
title: Authentication Service Architecture
document_id: DHF-04-01
version: 1.0
status: DRAFT
owner: Lead Architect
created_date: 2026-01-10
trace_source: DHF-04
---

> Detailed architecture for the Okapi Authentication Service. See [00-SDS-Overview.md](00-SDS-Overview.md) for high-level design.

## Quick Summary

Okapi Auth separates **who you are** (Authentication) from **what you can do** (Authorization):

| Concern | Handled By | Where |
|---------|-----------|-------|
| **Authentication (AuthN)** | External IdPs (Okta, Keycloak, SAML) | Outside Okapi |
| **Authorization (AuthZ)** | RBAC with Roles & Permissions | Inside Okapi |

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌──────────┐         ┌──────────────────┐         ┌──────────────────┐
    │          │         │                  │         │                  │
    │   User   │────────▶│   Okapi Web UI   │────────▶│  Okapi Auth API  │
    │          │         │                  │         │                  │
    └──────────┘         └──────────────────┘         └────────┬─────────┘
                                                               │
                         ┌─────────────────────────────────────┼─────────────┐
                         │                                     │             │
                         ▼                                     ▼             │
              ┌─────────────────────┐              ┌─────────────────────┐   │
              │   AuthN Module      │              │   AuthZ Module      │   │
              │                     │              │                     │   │
              │  • OIDC Client      │──Identity───▶│  • RBAC Engine      │   │
              │  • SAML 2.0 SP      │              │  • Permission Check │   │
              │  • Session Mgmt     │              │  • Role Mapping     │   │
              └──────────┬──────────┘              └──────────┬──────────┘   │
                         │                                    │              │
                         ▼                                    ▼              │
              ┌─────────────────────┐              ┌─────────────────────┐   │
              │   External IdPs     │              │   PostgreSQL        │   │
              │                     │              │                     │   │
              │  • Okta             │              │  • Identities       │   │
              │  • Entra ID         │              │  • Roles            │   │
              │  • Auth0            │              │  • UserRoles        │   │
              │  • Hospital SAML    │              │  • AuditEvents      │   │
              └─────────────────────┘              └─────────────────────┘   │
                                                                             │
                         ┌───────────────────────────────────────────────────┘
                         ▼
              ┌─────────────────────┐
              │   Audit Service     │
              │                     │
              │  • Login/Logout     │
              │  • Access Denied    │
              │  • Role Changes     │
              └─────────────────────┘
```

---

## Authentication Flow

```
┌────────┐     ┌─────────┐     ┌──────────────┐     ┌─────────┐     ┌──────────┐
│  User  │     │ Browser │     │  Okapi Auth  │     │   IdP   │     │ Database │
└───┬────┘     └────┬────┘     └──────┬───────┘     └────┬────┘     └────┬─────┘
    │               │                 │                  │               │
    │ 1. Login      │                 │                  │               │
    ├──────────────▶│                 │                  │               │
    │               │ 2. /oauth2/auth │                  │               │
    │               ├────────────────▶│                  │               │
    │               │                 │ 3. Redirect      │               │
    │               │◀────────────────┤                  │               │
    │               │ 4. IdP Login    │                  │               │
    │               ├─────────────────┼─────────────────▶│               │
    │               │                 │                  │               │
    │               │ 5. Auth Code    │                  │               │
    │               │◀────────────────┼──────────────────┤               │
    │               │                 │                  │               │
    │               │ 6. Callback     │                  │               │
    │               ├────────────────▶│                  │               │
    │               │                 │ 7. Exchange Code │               │
    │               │                 ├─────────────────▶│               │
    │               │                 │ 8. ID Token      │               │
    │               │                 │◀─────────────────┤               │
    │               │                 │                  │               │
    │               │                 │ 9. Normalize Identity            │
    │               │                 ├──────────────────┼──────────────▶│
    │               │                 │                  │               │
    │               │                 │ 10. Create/Update Identity       │
    │               │                 │◀─────────────────┼───────────────┤
    │               │                 │                  │               │
    │               │ 11. Set Cookie  │                  │               │
    │               │◀────────────────┤                  │               │
    │               │ (HTTP-only JWT) │                  │               │
    │ 12. Logged In │                 │                  │               │
    │◀──────────────┤                 │                  │               │
    │               │                 │                  │               │
```

---

## Component Details

### AuthN Module (Authentication)

**Purpose:** Prove the user's identity via external IdPs.

| Component | Description |
|-----------|-------------|
| `OidcAuthenticationSuccessHandler` | Handles successful OIDC login, normalizes identity |
| `SamlAuthenticationSuccessHandler` | Handles successful SAML login, normalizes identity |
| `AuthenticationFailureHandler` | Logs failed authentication attempts |
| `SessionAuthenticationFilter` | Validates JWT session cookie on each request |
| `SessionService` | Creates/validates JWT tokens (12-hour default) |

**Supported Protocols:**
- **OIDC** → Okta, Entra ID (Azure AD), Auth0
- **SAML 2.0** → Hospital enterprise SSO (e.g., Epic, Cerner integrations)

### AuthZ Module (Authorization)

**Purpose:** Determine what an authenticated user can do.

| Component | Description |
|-----------|-------------|
| `RoleService` | Manages role assignments and permission checks |
| `IdentityNormalizationService` | Maps IdP claims to internal Identity |
| `SecurityConfig` | Spring Security configuration with endpoint protection |

**RBAC Roles:**

| Role | Permissions |
|------|-------------|
| `PATHOLOGIST` | VIEW_CASES, CREATE_AI_SUGGESTIONS, CONFIRM_RESULTS, WRITE_BACK_EPIC |
| `TECHNICIAN` | VIEW_CASES, MANAGE_DATA_INGESTION |
| `ADMIN` | MANAGE_CONFIG, VIEW_AUDIT_LOGS |

### Data Layer

**Entities:**

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│    Identity     │       │      Role       │       │    UserRole     │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id              │       │ id              │       │ id              │
│ external_subject│       │ name            │       │ identity_id  ───┼──▶ Identity
│ issuer          │       │ description     │       │ role_id      ───┼──▶ Role
│ display_name    │       │ permissions[]   │       │ assigned_at     │
│ email           │       └─────────────────┘       │ assigned_by     │
│ auth_protocol   │                                 └─────────────────┘
│ mfa_verified    │       ┌─────────────────┐
│ status          │       │ IdpGroupMapping │
│ created_at      │       ├─────────────────┤
│ last_login_at   │       │ issuer          │
└─────────────────┘       │ idp_group_name  │
                          │ role_id      ───┼──▶ Role
┌─────────────────┐       │ active          │
│   AuditEvent    │       └─────────────────┘
├─────────────────┤
│ id              │
│ timestamp       │
│ event_type      │
│ identity_id     │
│ outcome         │
│ ip_address      │
│ details         │
└─────────────────┘
```

---

## Security Controls

| Control | Implementation | Requirement |
|---------|----------------|-------------|
| **Fail Closed** | Any token/IdP failure = access denied | SDS Section 6 |
| **HTTP-only Cookies** | JWT stored in secure, HTTP-only cookie | SDS Section 3.3 |
| **MFA Detection** | `amr` claim checked for MFA status | SYS-AUTH-003 |
| **Immutable Audit Logs** | All auth events logged to database | SYS-AUD-001 |
| **No Local Secrets** | Credentials via env vars / Secrets Manager | SDS Section 6 |
| **12-Hour Sessions** | Configurable session duration | SYS-AUTH-004 |

---

## Database Configuration

### Development (H2 In-Memory)
```properties
DATABASE_URL=jdbc:h2:mem:okapi
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
```

### Production (PostgreSQL)
```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/okapi
DATABASE_USERNAME=okapi_service
DATABASE_PASSWORD=${from_secrets_manager}
```

---

## API Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/auth/check` | GET | Check if authenticated | No |
| `/auth/me` | GET | Get current user info | Yes |
| `/auth/logout` | POST | Logout and clear session | Yes |
| `/auth/providers` | GET | List enabled IdP providers | No |
| `/api/roles` | GET/POST | Manage roles (Admin only) | Yes + ADMIN |
| `/api/audit` | GET | View audit logs (Admin only) | Yes + ADMIN |
| `/actuator/health` | GET | Health check | No |

---

## Traceability Matrix

| Design Element | System Requirement | Risk Control |
|----------------|-------------------|--------------|
| AuthN Gateway (OIDC/SAML) | SYS-AUTH-001, SYS-AUTH-002 | RISK-001 |
| Session Token (12hr) | SYS-AUTH-004 | N/A |
| Immutable Audit Logs | SYS-AUD-001 | RISK-006 |
| RBAC Enforcement | SYS-AUTH-003 | RISK-005 |
