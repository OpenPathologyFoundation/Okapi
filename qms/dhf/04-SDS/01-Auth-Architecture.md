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
    │               │ 11. Establish Session/Cookie │                  │               │
    │               │◀────────────────┤                  │               │
    │               │ (HTTP-only cookie/session)    │                  │               │
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
| Spring Security OAuth2 Client | OIDC login redirect and callback handling |
| `CustomOidcUserService` | Loads user from IdP, normalizes identity, persists identity (including structured name components when available), derives authorities |
| `SecurityConfig` | Endpoint protection and OAuth2 login wiring |

**Supported Protocols:**
- **OIDC** → Okta, Entra ID (Azure AD), Auth0
- **SAML 2.0** → Hospital enterprise SSO 

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
| `PATHOLOGIST` | VIEW_CASES, CREATE_AI_SUGGESTIONS, CONFIRM_RESULTS, WRITE_BACK_EHR |
| `TECHNICIAN` | VIEW_CASES, MANAGE_DATA_INGESTION |
| `ADMIN` | MANAGE_CONFIG, VIEW_AUDIT_LOGS |

### Data Layer

**Schema (AuthZ core):**

| Table | Purpose | Key constraints/notes |
|------|---------|------------------------|
| `identities` | Normalized user record | Unique `(provider_id, external_subject)`; stable `euid` UUID; supports structured name fields (`given_name`, `family_name`, `display_short`, etc.) |
| `roles` | Internal RBAC roles | Unique `name` |
| `identity_roles` | Identity ↔ roles assignments | M:N join; `assignment_source` supports IdP-derived assignments |
| `idp_group_mappings` | Provider-scoped IdP group registry | Unique `(provider_id, idp_group_name)` |
| `idp_group_role_mappings` | Group → role mapping | M:N join between groups and roles |
| `audit_events` | Security/ops audit trail (foundation) | Structured metadata (`jsonb`) and correlation fields |

---

## Security Controls

| Control | Implementation | Requirement |
|---------|----------------|-------------|
| **External AuthN** | Authentication is delegated to external IdP (OIDC/SAML). Okapi does not store passwords. | SYS-AUTH-001, SYS-AUTH-002 |
| **Fail closed on invalid tokens** | Invalid/expired tokens or missing auth context results in `401/403`. | SYS-AUTH-007 |
| **Issuer-scoped identity** | Identity uniqueness is enforced per IdP issuer via `(provider_id, external_subject)`. | SYS-AUTH-006 |
| **Least privilege RBAC** | Roles are derived from IdP groups via DB mappings; unmapped groups yield no elevated access. | SYS-AUTH-004, SYS-AUTH-009 |
| **Audit foundation** | `audit_events` schema exists for security/ops auditing; events to be emitted by the app in later iterations. | SYS-AUD-002 |
| **No committed secrets** | IdP and DB secrets are supplied via env vars/secret stores (`.env` is dev-only and gitignored). | SYS-SEC-010 |

### Administrative access management (no local credential creation)
- **Identity provisioning** is managed centrally in the IdP (e.g., Keycloak). Okapi does not create primary credentials.
- Okapi **derives** administrative authority from IdP group membership (e.g., `Okapi_Admins`) via issuer-scoped group→role mappings.
- Okapi provides an authenticated "who am I" endpoint (`GET /auth/me`) to enable UI integration and rapid troubleshooting of incorrect or delayed access.

---

## Database Configuration

### Development / Local
- Postgres runs in Docker (see `auth-system/docker-compose.yml`).
- Schema and baseline reference data are created via Flyway migrations at application startup.

### Unit tests
- Use in-memory H2 with Hibernate `ddl-auto: create-drop` and Flyway disabled.

### Migration management
- Flyway migrations live in `auth-system/src/main/resources/db/migration/`.
- `V1__init_schema.sql` owns schema creation (including `pgcrypto`).
- `V2__seed_reference_data.sql` owns baseline reference data (roles + IdP group mappings). Sample identities are not seeded in baseline migrations.

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
| Session/cookie policy (duration, remember-device) | SYS-AUTH-008 | RISK-011 |
| Immutable Audit Logs | SYS-AUD-001 | RISK-006 |
| RBAC Enforcement | SYS-AUTH-004, SYS-AUTH-009 | RISK-010 |
| Identity introspection (`/auth/me`) | SYS-AUTH-010 | RISK-013 |
| Admin provisioning model (IdP groups + local mappings) | SYS-AUTH-011, SYS-AUTH-012 | RISK-013, RISK-014 |
