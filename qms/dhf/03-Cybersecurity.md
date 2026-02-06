# 03-Cybersecurity
---
title: Cybersecurity Threat Model and Security Requirements (STRIDE)
document_id: DHF-03
version: 1.0
status: DRAFT
owner: Security Officer
created_date: 2026-01-11
trace_source: SRS-001
trace_destination: DHF-06-VVP
---

# 1. Purpose
Define cybersecurity assumptions, threat model (STRIDE), and security requirements for Okapi modules in scope, including:
- IAM (Authentication/Authorization)
- HAT (Histology Asset Tracking)

# 2. System Context (security-relevant)
## 2.1 Trust boundaries
- **External IdP (OIDC/SAML):** authenticates users; Okapi trusts issuer metadata and signed tokens.
- **Okapi Auth System:** enforces internal RBAC (authorization) based on normalized identity + group→role mappings.
- **Database (PostgreSQL):** stores normalized identities, RBAC mappings, and audit-event records.

For HAT:
- **Source systems (LIS/LIMS):** authoritative or semi-authoritative assertions about asset existence/state (e.g., CoPath, Beaker).
- **HAT users & devices:** scanners, workstations, and humans performing physical actions on assets.
- **HAT data store:** stores current state, requests, and append-only event history for traceability.

## 2.3 Identity provisioning and access management assumptions
- **Identity source of truth:** user accounts and group membership are managed in the external IdP (e.g., Keycloak). Okapi does not create primary credentials.
- **Local authorization source of truth:** Okapi stores issuer-scoped group→role mappings and local permission assignments (e.g., permission groups).
- **Timeliness:** timely provisioning reduces operational pressure to bypass controls during urgent clinical workflows (availability and safety concern).
- **Access tokens are short-lived:** access tokens default to 10 minutes and are not per-token revocable; emergency response relies on role changes, incident controls, and short TTL.

## 2.2 Key security assets
- Authentication assertions (OIDC ID token / access token, SAML assertion)
- Authorization data (roles, group mappings)
- Audit evidence (audit events)
- Secrets (OIDC client secret, DB credentials)

HAT-specific assets:
- Chain-of-custody evidence (who handled which physical asset, when, where)
- Asset identifiers and crosswalk mappings (barcodes, LIS IDs, external consult IDs)
- Request intent (what should be done to which assets, by whom, by when)
- Reconciliation decisions (conflicts between sources and their resolutions)

# 3. Threat Model (STRIDE summary)
| STRIDE | Threat (example) | Impact | Primary mitigations |
|--------|-------------------|--------|---------------------|
| **S** Spoofing | Attacker uses forged token or wrong issuer | Unauthorized access | Validate signatures and issuer/audience; fail closed (`SYS-AUTHN-006`) |
| **T** Tampering | DB mapping changed to elevate privileges | Privilege escalation | Least privilege DB access; change control; audit changes (`SYS-AUD-001`) |
| **R** Repudiation | User denies having performed an admin action | Non-repudiation gap | Emit audit events with correlation and actor/target (`SYS-AUD-001`, `SYS-AUD-002`) |
| **I** Information disclosure | Secrets committed or leaked via logs | PHI/security exposure | No committed secrets (`SYS-SEC-010`); log hygiene |
| **D** Denial of service | IdP outage prevents login | Loss of availability | Clear outage behavior; health checks; degraded mode policies (site-dependent) |
| **E** Elevation of privilege | Group mapping collision across issuers | Unauthorized role grants | Issuer-scoped mappings + identity uniqueness (`SYS-AUTHZ-002`, `SYS-AUTHN-005`) |

## 3.1 HAT threat highlights (STRIDE, abbreviated)

| STRIDE | HAT threat (example) | Impact | Primary mitigations |
|--------|-----------------------|--------|---------------------|
| **S** Spoofing | User acts under wrong identity or uses a shared workstation session | Incorrect custody/actions; audit gaps | Strong session handling; RBAC for HAT actions (`SYS-HAT-013`); audit attribution (`SYS-HAT-005`) |
| **T** Tampering | Asset history edited/deleted or custody changed without trace | Loss of defensibility; wrong operational decisions | Append-only history (`SYS-HAT-005`, `SYS-HAT-006`); authorization controls (`SYS-HAT-013`); change control |
| **R** Repudiation | User disputes having marked an asset missing or shipped | Investigation difficulty | Immutable event history with actor/comment (`SYS-HAT-005`) |
| **I** Information disclosure | Search or displays expose patient-identifying data beyond policy | Privacy breach | Privacy-limited search modes (`SYS-HAT-003`); RBAC and least-privilege UI/API (`SYS-HAT-013`) |
| **D** Denial of service | HAT unavailable; users bypass scan confirmation or recordkeeping | Increased wrong-asset risk | Resilience patterns; offline/degraded procedures; audit of deviations (deployment policy) |
| **E** Elevation of privilege | Low-privilege user performs distribution/release actions | Unauthorized release | Fine-grained privileges + governance for high-risk actions (`SYS-HAT-013`); approvals as policy |

## 3.2 Work List Module (WL) threat highlights

| STRIDE | WL Threat (example) | Impact | Primary mitigations |
|--------|---------------------|--------|---------------------|
| **S** Spoofing | User impersonates another pathologist to view their work list | Unauthorized PHI access | `SYS-WL-003`: Service-layer capability gating; `SYS-WL-007`: Break-glass audit trail. |
| **I** Information Disclosure | Work list viewed in public area exposes PHI | Accidental Privacy Breach | `SYS-WL-004`: "Teaching View" (Masked Mode); default to masked in public terminals. |
| **R** Repudiation | User denies using "Break-Glass" to view VIP case | Abuse of emergency access | `SYS-WL-007`: Mandatory audit event with reason code for all break-glass actions. |
| **D** Denial of Service | "Grim Reaper" attack: User assigns all cases to a black hole queue | Clinical workflow stoppage | `SYS-WL-003`: Reassignment capability strictly limited by RBAC; Rate limiting on assignment APIs. |

## 3.3 IAM Administration (Admin UI) threat highlights

| STRIDE | Admin Threat (example) | Impact | Primary mitigations |
|--------|-----------------------|--------|---------------------|
| **S** Spoofing | Non-admin user accesses admin API endpoints via direct URL or API call | Unauthorized admin operations; unauthorized access to identity data | `SYS-AUTHZ-011`: Server-side RBAC enforcement on all admin APIs; `SYS-ADMIN-009`: UI checks are advisory only; `SYS-ADMIN-001`: All admin endpoints gated by `ROLE_ADMIN`. |
| **T** Tampering | Admin modifies IdP group→role mappings to escalate privileges for a user group | Privilege escalation for multiple users | `SYS-ADMIN-003`: Audit all admin actions with actor attribution; change control review per SOP-ChangeControl; issuer-scoped mappings (`SYS-AUTHZ-002`). |
| **R** Repudiation | Admin denies having modified group mappings or revoked a user's access | Investigation gap; inability to attribute responsibility | `SYS-ADMIN-003`: Audit events with actor identity, timestamp, target entity, and metadata; non-repudiation via IdP-authenticated session. |
| **E** Elevation of privilege | Admin assigns themselves additional roles beyond their legitimate authority | Self-privilege escalation | Operational policy: dual-admin review for self-role-assignment; `SYS-ADMIN-003`: audit visibility; short-lived tokens (`SYS-AUTHZ-012`) limit blast radius. |

Additional IAM-specific threats to track:
- **Mis-provisioning / delayed access:** incorrect or late group membership updates can block appropriate users from timely access, creating workflow disruption and “break-glass” pressure.
- **Privilege escalation by mapping drift:** unauthorized changes to group→role mappings can elevate access; mitigated by change control + audit evidence.
- **Token leakage within TTL:** a leaked access token remains valid until expiry; mitigated by short TTL, audit logging, and incident response controls (disable login, suspend services, revoke roles).

# 4. Security Requirements (traceable)
The following security requirements are defined in `qms/dhf/02-SRS.md` and verified in `qms/dhf/06-VVP.md`:
- `SYS-AUTHN-001`/`SYS-AUTHN-002`: External IdP federation (OIDC/SAML)
- `SYS-AUTHZ-001`: Internal RBAC enforcement
- `SYS-AUTHZ-002` + `SYS-AUTHN-005`: Issuer-scoped group mapping and identity uniqueness
- `SYS-AUTHN-006`: Fail-closed behavior on invalid auth context
- `SYS-AUTHN-008`: Authenticated identity introspection (“who am I”) endpoint
- `SYS-AUTHZ-005`/`SYS-AUTHZ-006`: Admin access management and timely provisioning model (IdP-managed identities + Okapi-local permissions)
- `SYS-AUTHZ-010`/`SYS-AUTHZ-011`/`SYS-AUTHZ-012`: Default-deny authorization, server-side enforcement, and token augmentation
- `SYS-AUD-001`/`SYS-AUD-002`: Audit event recording + schema support
- `SYS-DATA-001`: Flyway-managed schema as a controlled baseline
- `SYS-SEC-010`: Secrets supplied via env/secret store, not committed

Admin UI security-relevant requirements:
- `SYS-ADMIN-001`: All admin API endpoints gated by `ROLE_ADMIN`
- `SYS-ADMIN-003`: All admin actions audited with actor attribution
- `SYS-ADMIN-009`: UI authorization checks are advisory; server-side enforcement is authoritative
- `SYS-AUTHZ-011`: Server-side authorization on every protected API (applies to admin APIs)

HAT security-relevant requirements:
- `SYS-HAT-005`/`SYS-HAT-006`: Append-only history and non-destructive corrections
- `SYS-HAT-003`: Privacy-limited search modes (site policy)
- `SYS-HAT-013`: RBAC + governance controls for high-risk actions
- `SYS-HAT-014`: Traceability from request → events → current state

# 5. Verification Approach (summary)
- **Unit tests:** authorization mapping logic and config wiring (fast, in-memory DB).
- **Integration tests:** OIDC discovery against a real IdP (Keycloak) and Flyway migrations against Postgres (Docker/Testcontainers).
- **Manual verification (dev):** `bootRun` against Docker Postgres to confirm Flyway migration and app startup.

For HAT (planned):
- **Unit tests:** identifier normalization, deterministic match outcomes, state transition rules.
- **Integration tests:** event append-only invariants; request lifecycle; RBAC constraints.
