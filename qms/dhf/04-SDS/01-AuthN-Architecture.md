# 01-AuthN-Architecture

---
title: Authentication (AuthN) Service Architecture
document_id: DHF-04-01
version: 2.0
status: DRAFT
owner: Lead Architect
created_date: 2026-01-26
trace_source: DHF-04, SRS-001
---

> Detailed architecture for Okapi Authentication services. This document covers **identity federation**, **session management**, and **device trust**. For authorization (RBAC, permissions, access grants), see [02-AuthZ-Architecture.md](02-AuthZ-Architecture.md).

## 1. Overview

Authentication (AuthN) answers the question: **"Who are you?"**

Okapi delegates primary authentication to external Identity Providers (IdPs) and does not store user passwords. The AuthN module is responsible for:

- **Protocol handling**: OIDC and SAML 2.0 integration with external IdPs
- **Identity normalization**: Mapping IdP claims to a stable internal identity record
- **Session management**: Establishing and maintaining authenticated sessions
- **Device trust**: Managing "remember this device" functionality

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATION FLOW                                  │
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
              │   AuthN Module      │              │   Identity Store    │   │
              │                     │              │                     │   │
              │  • OIDC Client      │──────────────│  • iam.identity     │   │
              │  • SAML 2.0 SP      │              │  • Normalized       │   │
              │  • Session Mgmt     │              │    claims           │   │
              │  • Device Trust     │              │                     │   │
              └──────────┬──────────┘              └─────────────────────┘   │
                         │                                                    │
                         ▼                                                    │
              ┌─────────────────────┐              ┌─────────────────────┐   │
              │   External IdPs     │              │  Device Trust Store │   │
              │                     │              │                     │   │
              │  • Okta             │              │  • iam.session_     │   │
              │  • Entra ID         │              │    device           │   │
              │  • Auth0            │              │                     │   │
              │  • Hospital SAML    │              └─────────────────────┘   │
              └─────────────────────┘                                        │
                                                                             │
                         ┌───────────────────────────────────────────────────┘
                         ▼
              ┌─────────────────────┐
              │   Audit Service     │
              │                     │
              │  • Login/Logout     │
              │  • Session Events   │
              │  • Device Trust     │
              └─────────────────────┘
```

## 3. Protocol Handling

### 3.1 Supported Protocols

| Protocol | Use Case | IdP Examples |
|----------|----------|--------------|
| **OIDC (OpenID Connect)** | Modern cloud IdPs | Okta, Entra ID (Azure AD), Auth0, Keycloak |
| **SAML 2.0** | Enterprise hospital SSO | Hospital AD FS, Ping Identity |

### 3.2 OIDC Flow

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
    │               │                 │ 9. Normalize + Persist Identity  │
    │               │                 ├──────────────────┼──────────────▶│
    │               │                 │                  │               │
    │               │                 │ 10. Identity Record              │
    │               │                 │◀─────────────────┼───────────────┤
    │               │                 │                  │               │
    │               │ 11. Session Cookie                 │               │
    │               │◀────────────────┤                  │               │
    │ 12. Logged In │                 │                  │               │
    │◀──────────────┤                 │                  │               │
```

### 3.3 SAML 2.0 Flow

Okapi acts as a SAML Service Provider (SP). The flow is similar to OIDC but uses SAML assertions instead of ID tokens:

1. User initiates login
2. Okapi generates SAML AuthnRequest and redirects to IdP
3. IdP authenticates user and returns SAML Response
4. Okapi validates assertion signature and extracts attributes
5. Identity is normalized and persisted
6. Session is established

## 4. Identity Normalization

### 4.1 Claim Mapping

The AuthN module maps diverse IdP claims to a stable internal identity structure:

| Internal Field | OIDC Claim | SAML Attribute | Description |
|----------------|------------|----------------|-------------|
| `provider_id` | `iss` (issuer) | EntityID | IdP identifier; scopes all lookups |
| `external_subject` | `sub` | NameID | Subject identifier within provider |
| `email` | `email` | mail, emailAddress | User's email |
| `username` | `preferred_username` | sAMAccountName, uid | Username |
| `display_name` | `name` | displayName | Full display name |
| `display_short` | (derived) | (derived) | Short display (e.g., "Smith, J.") |
| `given_name` | `given_name` | givenName | First name |
| `family_name` | `family_name` | sn, surname | Last name |
| `middle_name` | `middle_name` | middleName | Middle name |
| `prefix` | (custom) | personalTitle | Name prefix (Dr., etc.) |
| `suffix` | (custom) | generationQualifier | Name suffix (MD, PhD, Jr.) |

### 4.2 Identity Uniqueness

Identity uniqueness is enforced at the database level via:

```sql
CONSTRAINT uq_iam_identity_provider_subject
    UNIQUE (provider_id, external_subject)
```

This ensures that the same subject from two different IdPs creates two distinct identity records, preventing cross-IdP collision.

### 4.3 Identity Persistence

On successful authentication:

1. **Lookup**: Query `iam.identity` by `(provider_id, external_subject)`
2. **Create or Update**:
   - If not found: Create new identity record
   - If found: Update normalized claims (email, name, etc.) and `last_login_at`
3. **Return**: Identity ID for session establishment

## 5. Session Management

### 5.1 Session Establishment

After identity normalization, Okapi establishes an authenticated session:

- **Session storage**: Server-side session with HTTP-only session cookie
- **Session ID**: Opaque, cryptographically random identifier
- **Session data**: Identity ID, authentication timestamp, IdP provider

### 5.2 Session Configuration

| Parameter | Description | Default | Configurable |
|-----------|-------------|---------|--------------|
| `session.timeout` | Idle session timeout | 30 minutes | Yes (deployment policy) |
| `session.max-age` | Maximum session lifetime | 8 hours | Yes (deployment policy) |
| `session.cookie.secure` | HTTPS-only cookie | true | No (always true in production) |
| `session.cookie.http-only` | JavaScript-inaccessible | true | No (always true) |
| `session.cookie.same-site` | CSRF protection | Lax | Yes |

### 5.3 Session Termination

Sessions are terminated by:

- **User logout**: Explicit `/auth/logout` request
- **Idle timeout**: No activity for `session.timeout` period
- **Max age expiry**: Session exceeds `session.max-age`
- **IdP logout**: OIDC back-channel logout or SAML SLO (if supported)

## 6. Device Trust

### 6.1 Purpose

Device trust ("remember this device") reduces authentication friction for users on trusted devices while maintaining security. When a device is trusted:

- Reduced re-authentication prompts
- Configurable trust duration per deployment policy

### 6.2 Data Model

Device trust is stored in `iam.session_device`:

| Column | Type | Description |
|--------|------|-------------|
| `device_id` | UUID | Primary key |
| `identity_id` | UUID | FK to iam.identity |
| `device_fingerprint_hash` | TEXT | Hashed device fingerprint (no raw material stored) |
| `first_seen_at` | TIMESTAMPTZ | Initial trust establishment |
| `last_seen_at` | TIMESTAMPTZ | Most recent use |
| `trusted_until` | TIMESTAMPTZ | Trust expiration |
| `revoked_at` | TIMESTAMPTZ | When trust was revoked |
| `revoked_by_identity_id` | UUID | Who revoked (user or admin) |
| `metadata` | JSONB | Additional context (browser, OS, etc.) |

### 6.3 Device Fingerprinting

Device fingerprinting uses a hash of stable device characteristics:

- Browser/User-Agent characteristics
- Screen resolution and color depth
- Installed fonts (subset)
- Timezone

**Security note**: Only the hash is stored; raw fingerprint material is never persisted.

### 6.4 Trust Lifecycle

```
┌─────────────┐    User consents     ┌─────────────┐
│   Unknown   │ ─────────────────────▶│   Trusted   │
│   Device    │                       │   Device    │
└─────────────┘                       └──────┬──────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
           ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
           │   Expired   │          │   Revoked   │          │   Revoked   │
           │  (timeout)  │          │  (by user)  │          │  (by admin) │
           └─────────────┘          └─────────────┘          └─────────────┘
```

### 6.5 User Self-Service

Users can manage their trusted devices via the profile interface:

- **View**: List all trusted devices with last-seen timestamp
- **Revoke**: Remove trust from specific devices
- **Revoke all**: Emergency "log out everywhere" functionality

## 7. API Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/auth/check` | GET | Check if authenticated | No |
| `/auth/me` | GET | Get current identity + roles | Yes |
| `/auth/logout` | POST | Logout and clear session | Yes |
| `/auth/providers` | GET | List enabled IdP providers | No |
| `/auth/devices` | GET | List user's trusted devices | Yes |
| `/auth/devices/{id}` | DELETE | Revoke device trust | Yes |
| `/auth/devices/revoke-all` | POST | Revoke all device trust | Yes |
| `/actuator/health` | GET | Health check | No |

## 8. Security Controls

| Control | Implementation | Requirement |
|---------|----------------|-------------|
| **External AuthN** | Authentication delegated to external IdP; no password storage | SYS-AUTHN-001, SYS-AUTHN-002 |
| **MFA delegation** | MFA enforcement is IdP responsibility; Okapi trusts IdP assertion | SYS-AUTHN-003 |
| **Fail closed** | Invalid/expired tokens or missing auth context yields 401/403 | SYS-AUTHN-006 |
| **Issuer-scoped identity** | Uniqueness enforced per IdP via `(provider_id, external_subject)` | SYS-AUTHN-005 |
| **Secure cookies** | HTTP-only, Secure, SameSite attributes on session cookies | SYS-AUTHN-007 |
| **Device fingerprint hashing** | Only hash stored; no raw fingerprint material | Privacy best practice |
| **No committed secrets** | IdP secrets via env vars/secret store | SYS-SEC-010 |

## 9. Audit Events

The AuthN module emits the following audit events to `iam.audit_event`:

| Event Type | Description | Logged Data |
|------------|-------------|-------------|
| `AUTHN_LOGIN_SUCCESS` | Successful authentication | identity_id, provider_id, device_id |
| `AUTHN_LOGIN_FAILURE` | Failed authentication attempt | provider_id, reason |
| `AUTHN_LOGOUT` | User-initiated logout | identity_id, session_id |
| `AUTHN_SESSION_EXPIRED` | Session timeout/expiry | identity_id, session_id |
| `AUTHN_DEVICE_TRUSTED` | Device trust established | identity_id, device_id |
| `AUTHN_DEVICE_REVOKED` | Device trust revoked | identity_id, device_id, revoked_by |

## 10. Traceability Matrix

| Design Element | User Need | System Requirement | Risk Control |
|----------------|-----------|-------------------|--------------|
| OIDC/SAML integration | UN-AUTHN-002 | SYS-AUTHN-001, SYS-AUTHN-002 | RISK-001 |
| Identity normalization | UN-AUTHN-001 | SYS-AUTHN-004, SYS-AUTHN-005 | RISK-009 |
| Session management | UN-AUTHN-003 | SYS-AUTHN-007 | RISK-011 |
| Device trust | UN-AUTHN-004, UN-AUTHN-005 | SYS-AUTHN-009, SYS-AUTHN-010 | RISK-011 |
| Identity introspection (`/auth/me`) | UN-AUTHN-007 | SYS-AUTHN-008 | RISK-013 |
| Authentication audit events | UN-AUTHN-006 | SYS-AUTHN-011, SYS-AUD-001, SYS-AUD-002 | RISK-006 |

## 11. Component Implementation

| Component | Description |
|-----------|-------------|
| `SecurityConfig` | Spring Security configuration with OAuth2 login wiring |
| `CustomOidcUserService` | Loads user from IdP, normalizes identity, persists to DB |
| `SamlUserDetailsService` | SAML assertion processing and identity normalization |
| `SessionService` | Session lifecycle management |
| `DeviceTrustService` | Device fingerprinting, trust establishment/revocation |
| `AuthAuditService` | Authentication event emission |
