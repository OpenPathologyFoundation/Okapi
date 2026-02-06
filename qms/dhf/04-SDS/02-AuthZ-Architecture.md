# 02-AuthZ-Architecture

---
title: Authorization (AuthZ) Service Architecture
document_id: DHF-04-02
version: 1.0
status: DRAFT
owner: Lead Architect
created_date: 2026-01-26
trace_source: DHF-04, SRS-001
---

> Detailed architecture for Okapi Authorization services. This document covers **RBAC**, **permissions**, **IdP group mapping**, **break-glass access**, and **research access grants**. For authentication (identity federation, sessions), see [01-AuthN-Architecture.md](01-AuthN-Architecture.md).

## 1. Overview

Authorization (AuthZ) answers the question: **"What can you do?"**

Given an authenticated identity, the AuthZ module determines what actions that identity may perform. Okapi implements:

- **Role-Based Access Control (RBAC)**: Roles aggregate permissions
- **Fine-grained permissions**: Individual capabilities that can be checked
- **IdP group federation**: External groups mapped to internal roles
- **Time-bounded access**: Role assignments with effective dates
- **Break-glass access**: Emergency, audited access to specific resources
- **Research access grants**: Governed, protocol-specific access with PHI controls

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AUTHORIZATION FLOW                                   │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────┐                    ┌─────────────────┐
    │  Authenticated  │                    │   Resource      │
    │    Identity     │────────────────────│   Request       │
    │  (from AuthN)   │                    │                 │
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             │         ┌────────────────────────────┘
             │         │
             ▼         ▼
    ┌─────────────────────────────────────────────────────────┐
    │                   AuthZ Engine                           │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
    │  │ Role        │  │ Permission  │  │ Break-Glass     │  │
    │  │ Resolver    │  │ Checker     │  │ Evaluator       │  │
    │  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘  │
    │         │                │                  │           │
    └─────────┼────────────────┼──────────────────┼───────────┘
              │                │                  │
              ▼                ▼                  ▼
    ┌─────────────────────────────────────────────────────────┐
    │                   IAM Data Store                         │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
    │  │ iam.role    │  │ iam.        │  │ iam.break_      │  │
    │  │ iam.identity│  │ permission  │  │ glass_grant     │  │
    │  │ _role       │  │ iam.role_   │  │                 │  │
    │  │             │  │ permission  │  │                 │  │
    │  └─────────────┘  └─────────────┘  └─────────────────┘  │
    │                                                          │
    │  ┌─────────────┐  ┌─────────────────────────────────┐   │
    │  │ iam.idp_    │  │ iam.research_access_grant       │   │
    │  │ group*      │  │                                 │   │
    │  └─────────────┘  └─────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────┘
```

## 3. RBAC Model

### 3.1 Core Concepts

| Concept | Definition | Example |
|---------|------------|---------|
| **Identity** | An authenticated user from any IdP | Dr. Jane Smith (from Okta) |
| **Role** | A named collection of permissions | `PATHOLOGIST`, `ADMIN` |
| **Permission** | A single, atomic capability | `CASE_VIEW`, `CASE_SIGN_OUT` |
| **Assignment** | An identity-to-role binding with metadata | Jane → PATHOLOGIST (from IdP group, effective indefinitely) |

### 3.2 Role Hierarchy

Roles are flat (no inheritance) to maintain simplicity and auditability. An identity may have multiple roles, and the effective permissions are the union of all role permissions.

```
Identity: Dr. Jane Smith
├── Role: PATHOLOGIST
│   ├── CASE_VIEW
│   ├── CASE_EDIT
│   ├── CASE_SIGN_OUT
│   └── HISTO_VIEW
└── Role: RESEARCHER
    ├── RESEARCH_VIEW
    └── RESEARCH_REQUEST

Effective permissions: CASE_VIEW, CASE_EDIT, CASE_SIGN_OUT, HISTO_VIEW, RESEARCH_VIEW, RESEARCH_REQUEST
```

### 3.3 System Roles

| Role | Description | System | Typical Permissions |
|------|-------------|--------|---------------------|
| `ADMIN` | System administrator | Yes | ADMIN_USERS, ADMIN_SYSTEM, ADMIN_AUDIT |
| `PATHOLOGIST` | Attending pathologist | Yes | CASE_VIEW, CASE_EDIT, CASE_SIGN_OUT, CASE_REASSIGN |
| `FELLOW` | Pathology fellow | Yes | CASE_VIEW, CASE_EDIT |
| `RESIDENT` | Pathology resident | Yes | CASE_VIEW, CASE_EDIT (limited) |
| `TECHNICIAN` | Lab technician | Yes | CASE_VIEW, HISTO_VIEW, HISTO_EDIT |
| `HISTO_TECH` | Histology technician | Yes | HISTO_VIEW, HISTO_EDIT, HISTO_BATCH, HISTO_QA |
| `CYTO_TECH` | Cytology technician | Yes | CASE_VIEW, HISTO_VIEW |
| `RESEARCHER` | Research user | Yes | RESEARCH_VIEW, RESEARCH_REQUEST |
| `RESEARCH_ADMIN` | Research administrator | Yes | RESEARCH_VIEW, RESEARCH_REQUEST, RESEARCH_APPROVE |

System roles (`is_system = true`) cannot be deleted but can have their permission assignments modified.

### 3.4 System Permissions

| Permission | Description | Category |
|------------|-------------|----------|
| `CASE_VIEW` | View case details and worklist | Clinical |
| `CASE_EDIT` | Edit case data and reports | Clinical |
| `CASE_SIGN_OUT` | Sign out / finalize cases | Clinical |
| `CASE_REASSIGN` | Reassign cases to other users | Clinical |
| `HISTO_VIEW` | View histology data | Histology |
| `HISTO_EDIT` | Edit histology data and status | Histology |
| `HISTO_BATCH` | Create and manage stain batches | Histology |
| `HISTO_QA` | Perform QA on stain batches | Histology |
| `RESEARCH_VIEW` | View research suitability metadata | Research |
| `RESEARCH_REQUEST` | Request research access grants | Research |
| `RESEARCH_APPROVE` | Approve research access grants | Research |
| `ADMIN_USERS` | Manage user identities and roles | Admin |
| `ADMIN_SYSTEM` | System configuration and settings | Admin |
| `ADMIN_AUDIT` | View audit logs | Admin |
| `BREAK_GLASS_INVOKE` | Invoke break-glass access | Emergency |
| `PROFILE_VIEW_OWN` | View own profile | Profile |
| `PROFILE_EDIT_OWN` | Edit own profile preferences | Profile |
| `PROFILE_VIEW_ANY` | View any user profile (admin) | Profile |
| `PROFILE_EDIT_ANY` | Edit any user profile (admin) | Profile |

## 4. Role Assignment

### 4.1 Assignment Sources

Role assignments track their provenance via `assignment_source`:

| Source | Description | Example |
|--------|-------------|---------|
| `IDP_GROUP` | Derived from IdP group membership | Okta group "Okapi_Pathologists" |
| `LOCAL_ADMIN` | Assigned by Okapi administrator | Manual role grant via admin UI |
| `BREAK_GLASS` | Temporary emergency assignment | Coverage during colleague absence |
| `SYSTEM` | System-generated assignment | Default roles for service accounts |

### 4.2 Time-Bounded Assignments

All role assignments support effective date ranges:

```sql
effective_from  TIMESTAMPTZ NOT NULL DEFAULT now()
effective_to    TIMESTAMPTZ NULL  -- NULL = indefinite
```

Use cases:
- **Training rotations**: Resident assigned to surgical pathology for 3 months
- **Locum tenens**: Temporary physician coverage
- **Research projects**: Time-limited access aligned with IRB approval
- **Break-glass**: Short-duration emergency access

### 4.3 Assignment Lifecycle

```
                    ┌─────────────────┐
   Admin creates    │                 │    effective_from reached
  ────────────────▶ │     PENDING     │ ─────────────────────────────┐
                    │  (future-dated) │                              │
                    └─────────────────┘                              ▼
                                                          ┌─────────────────┐
                                                          │                 │
                                                          │     ACTIVE      │
                                                          │                 │
                                                          └────────┬────────┘
                                                                   │
                    ┌──────────────────────────────────────────────┼──────────────────┐
                    │                                              │                  │
                    ▼                                              ▼                  ▼
         ┌─────────────────┐                           ┌─────────────────┐   ┌─────────────────┐
         │    EXPIRED      │                           │    REVOKED      │   │    SUPERSEDED   │
         │ (effective_to   │                           │  (admin action) │   │ (new assignment │
         │   reached)      │                           │                 │   │   replaces)     │
         └─────────────────┘                           └─────────────────┘   └─────────────────┘
```

### 4.4 Effective Roles Query

To determine an identity's current effective roles:

```sql
SELECT r.name, ir.assignment_source
FROM iam.identity_role ir
JOIN iam.role r ON ir.role_id = r.role_id
WHERE ir.identity_id = :identity_id
  AND ir.effective_from <= now()
  AND (ir.effective_to IS NULL OR ir.effective_to > now());
```

## 5. IdP Group Federation

### 5.1 Purpose

IdP group federation maps external group memberships to internal Okapi roles, enabling:
- Centralized access management in enterprise IdP
- Automatic provisioning/deprovisioning aligned with HR changes
- Reduced manual configuration in Okapi

### 5.2 Group Mapping Model

```
┌───────────────────────────────────────────────────────────────────┐
│                        IdP (e.g., Okta)                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │ Okapi_Admins    │  │ Okapi_Pathology │  │ Okapi_Research  │   │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘   │
└───────────┼─────────────────────┼─────────────────────┼───────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌───────────────────────────────────────────────────────────────────┐
│                    Okapi IdP Group Mappings                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │ provider_id:    │  │ provider_id:    │  │ provider_id:    │   │
│  │   okta.com/xxx  │  │   okta.com/xxx  │  │   okta.com/xxx  │   │
│  │ group_name:     │  │ group_name:     │  │ group_name:     │   │
│  │   Okapi_Admins  │  │   Okapi_Pathol  │  │   Okapi_Research│   │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘   │
└───────────┼─────────────────────┼─────────────────────┼───────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌───────────────────────────────────────────────────────────────────┐
│                       Okapi Roles                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │     ADMIN       │  │   PATHOLOGIST   │  │   RESEARCHER    │   │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘   │
└───────────────────────────────────────────────────────────────────┘
```

### 5.3 Issuer Scoping

Group mappings are scoped by `provider_id` to prevent collisions when multiple IdPs use the same group name:

```sql
CONSTRAINT uq_iam_idp_group_provider_name
    UNIQUE (provider_id, group_name)
```

### 5.4 Group Synchronization

During authentication:

1. AuthN module extracts group claims from IdP token/assertion
2. For each group, lookup mapping in `iam.idp_group` + `iam.idp_group_role`
3. Create/update `iam.identity_role` entries with `assignment_source = 'IDP_GROUP'`
4. Optionally: Remove IDP_GROUP assignments for groups no longer claimed

## 6. Break-Glass Access

### 6.1 Purpose

Break-glass provides emergency access to specific resources when normal permission models would block clinically necessary actions:

- Covering for an absent colleague
- Emergency consultation
- Unexpected patient transfer

### 6.2 Design Principles

1. **Explicit invocation**: User must consciously invoke break-glass
2. **Justification required**: Reason code and optional free-text explanation
3. **Time-bounded**: Automatic expiration (default: 24 hours)
4. **Scoped**: Access to specific entities, not system-wide elevation
5. **Fully audited**: All invocations logged with context

### 6.3 Data Model

```sql
CREATE TABLE iam.break_glass_grant (
    grant_id                 UUID PRIMARY KEY,
    identity_id              UUID NOT NULL REFERENCES iam.identity,

    -- Scope: what entity is being accessed
    scope_entity_type        TEXT NOT NULL,  -- 'CASE', 'SPECIMEN', etc.
    scope_entity_id          UUID NOT NULL,

    -- Justification
    reason_code              TEXT NOT NULL,  -- 'COVERAGE', 'EMERGENCY', 'CONSULT'
    justification            TEXT NULL,      -- Free-text explanation

    -- Validity
    granted_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at               TIMESTAMPTZ NOT NULL,

    -- Revocation
    revoked_at               TIMESTAMPTZ NULL,
    revoked_by_identity_id   UUID NULL,

    -- Audit metadata
    metadata                 JSONB NOT NULL DEFAULT '{}'
);
```

### 6.4 Break-Glass Flow

```
┌──────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌──────────────┐
│   User   │     │   Okapi UI      │     │   AuthZ Engine  │     │  Audit Log   │
└────┬─────┘     └────────┬────────┘     └────────┬────────┘     └──────┬───────┘
     │                    │                       │                     │
     │ 1. Request case    │                       │                     │
     │    (no permission) │                       │                     │
     ├───────────────────▶│                       │                     │
     │                    │ 2. Check permission   │                     │
     │                    ├──────────────────────▶│                     │
     │                    │ 3. ACCESS_DENIED      │                     │
     │                    │◀──────────────────────┤                     │
     │                    │                       │                     │
     │ 4. "Break-Glass    │                       │                     │
     │    Available"      │                       │                     │
     │◀───────────────────┤                       │                     │
     │                    │                       │                     │
     │ 5. Invoke break-   │                       │                     │
     │    glass with      │                       │                     │
     │    reason          │                       │                     │
     ├───────────────────▶│                       │                     │
     │                    │ 6. Create grant       │                     │
     │                    ├──────────────────────▶│                     │
     │                    │                       │ 7. Log invocation   │
     │                    │                       ├────────────────────▶│
     │                    │ 8. Grant created      │                     │
     │                    │◀──────────────────────┤                     │
     │ 9. Access granted  │                       │                     │
     │◀───────────────────┤                       │                     │
```

### 6.5 Eligibility

Not all users can invoke break-glass. Eligibility is controlled by:

1. **Permission**: User must have `BREAK_GLASS_INVOKE` permission
2. **Identity flag**: `iam.identity.break_glass_enabled` must be true
3. **Scope policy**: Deployment-specific rules about which entity types allow break-glass

## 7. Research Access Grants

### 7.1 Purpose

Research access grants provide governed, time-bounded access to specimens/cases for approved research studies, with fine-grained PHI exposure controls.

### 7.2 PHI Access Levels

| Level | Description | Use Case |
|-------|-------------|----------|
| `NONE` | No PHI visible; fully de-identified | Retrospective cohort counting |
| `MASKED` | PHI masked/redacted in display | Educational/training |
| `LIMITED` | Limited Identifiers (dates shifted, age ranges) | Research with waiver |
| `FULL` | Full PHI access | Clinical care, authorized research |

### 7.3 Data Model

```sql
CREATE TABLE iam.research_access_grant (
    grant_id                 UUID PRIMARY KEY,
    identity_id              UUID NOT NULL REFERENCES iam.identity,

    -- Scope
    scope_type               TEXT NOT NULL,  -- 'BLOCK', 'SPECIMEN', 'COHORT'
    scope_entity_id          UUID NULL,      -- NULL for cohort/protocol-wide
    scope_filter             JSONB NULL,     -- Filter criteria for cohort grants

    -- Protocol/governance
    protocol_id              TEXT NULL,      -- IRB protocol, clinical trial ID
    reason                   TEXT NOT NULL,  -- Justification
    approved_by_identity_id  UUID NULL REFERENCES iam.identity,

    -- PHI control
    phi_access_level         TEXT NOT NULL CHECK (phi_access_level IN
                             ('NONE', 'MASKED', 'LIMITED', 'FULL')),

    -- Validity
    granted_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at               TIMESTAMPTZ NOT NULL,

    -- Revocation
    revoked_at               TIMESTAMPTZ NULL,
    revoked_by_identity_id   UUID NULL,
    revocation_reason        TEXT NULL
);
```

### 7.4 Grant Workflow

1. **Request**: Researcher requests access via UI, specifying scope and protocol
2. **Review**: Research admin reviews request against IRB approval
3. **Approve/Deny**: Admin approves with PHI level and expiration, or denies with reason
4. **Grant active**: Researcher can access specified resources with PHI controls applied
5. **Expiration/Revocation**: Access automatically expires or can be manually revoked

## 8. Permission Evaluation

### 8.1 Evaluation Order

When checking if an identity can perform an action:

```
1. Check role-based permissions
   └─▶ Identity → identity_role → role → role_permission → permission

2. If denied, check break-glass grants
   └─▶ Identity → break_glass_grant (active, not expired, not revoked)

3. If denied, check research access grants
   └─▶ Identity → research_access_grant (active, not expired, not revoked)

4. Return final decision (ALLOW or DENY)
```

### 8.2 Permission Check API

```java
public interface AuthorizationService {

    // Simple permission check
    boolean hasPermission(UUID identityId, String permission);

    // Permission check with resource context
    boolean hasPermission(UUID identityId, String permission,
                          String entityType, UUID entityId);

    // Get all effective permissions
    Set<String> getEffectivePermissions(UUID identityId);

    // Check break-glass eligibility
    boolean canInvokeBreakGlass(UUID identityId,
                                 String entityType, UUID entityId);
}
```

## 9. JWT Augmentation (Authorization Claims)

Okapi augments access tokens after AuthN succeeds by injecting authoritative authorization claims derived from the IAM schema. These claims are the only source of truth for downstream service authorization decisions.

### 9.1 Claim Schema

Required claims:
- `roles`: array of role names (e.g., `["PATHOLOGIST","RESEARCHER"]`)
- `permissions`: array of permission names (e.g., `["CASE_VIEW","CASE_SIGN_OUT"]`)
- `okapi_authz_version`: string identifying the active authorization policy/mapping version

Standard claims (from IdP/issuer policy):
- `iss`, `sub`, `aud`, `exp`, `iat`, `jti`
- `auth_time`, `acr`, `amr` (if provided by IdP)

### 9.2 TTL and Refresh

- Access token TTL defaults to **10 minutes** and is configurable per deployment policy.
- Claims are recomputed on session renewal to reflect mapping or role changes.

### 9.3 Revocation Policy (Option 1: Short-Lived Tokens)

Okapi does **not** implement per-token revocation (no introspection, deny-list, or back-channel logout) for access tokens. Instead:

- Access tokens are short-lived (default **10 minutes**).
- Role/permission changes take effect on the **next token issuance** (session renewal).
- Emergency response is handled operationally (disable login, revoke roles, or pause sensitive services) rather than per-token invalidation.

This minimizes operational complexity and aligns with the clinical workflow assumption that sub-10-minute residual access is acceptable, while preserving auditability and incident response controls.

### 9.4 Token Trust Boundary (Diagram)

```
           ┌───────────────────────────┐
           │   Institutional IdP       │
           │   (OIDC/SAML)             │
           └──────────┬────────────────┘
                      │ AuthN assertion
                      ▼
           ┌───────────────────────────┐
           │   Okapi AuthN             │
           │   (validates, normalizes) │
           └──────────┬────────────────┘
                      │ Identity (issuer-scoped)
                      ▼
           ┌───────────────────────────┐
           │   Okapi AuthZ             │
           │   (IAM roles/permissions) │
           └──────────┬────────────────┘
                      │ Augmented token
                      ▼
           ┌───────────────────────────┐
           │   Downstream Services     │
           │   (enforce roles/perm)    │
           └───────────────────────────┘
```

## 10. API Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/roles` | GET | List all roles | Yes + ADMIN_USERS |
| `/api/roles` | POST | Create custom role | Yes + ADMIN_USERS |
| `/api/roles/{id}` | PUT | Update role | Yes + ADMIN_USERS |
| `/api/roles/{id}/permissions` | GET | List role permissions | Yes + ADMIN_USERS |
| `/api/permissions` | GET | List all permissions | Yes + ADMIN_USERS |
| `/api/identity/{id}/roles` | GET | Get identity's roles | Yes + ADMIN_USERS |
| `/api/identity/{id}/roles` | POST | Assign role to identity | Yes + ADMIN_USERS |
| `/api/break-glass` | POST | Invoke break-glass | Yes + BREAK_GLASS_INVOKE |
| `/api/break-glass/{id}` | DELETE | Revoke break-glass | Yes + ADMIN_USERS |
| `/api/research-grants` | GET | List research grants | Yes + RESEARCH_APPROVE |
| `/api/research-grants` | POST | Create research grant | Yes + RESEARCH_APPROVE |
| `/api/research-grants/{id}` | DELETE | Revoke research grant | Yes + RESEARCH_APPROVE |

## 11. Audit Events

The AuthZ module emits the following audit events:

| Event Type | Description | Logged Data |
|------------|-------------|-------------|
| `AUTHZ_ROLE_ASSIGNED` | Role assigned to identity | identity_id, role_id, source, effective dates |
| `AUTHZ_ROLE_REVOKED` | Role revoked from identity | identity_id, role_id, revoked_by |
| `AUTHZ_PERMISSION_DENIED` | Access denied | identity_id, permission, entity context |
| `AUTHZ_BREAK_GLASS_INVOKED` | Break-glass access invoked | identity_id, entity, reason |
| `AUTHZ_BREAK_GLASS_REVOKED` | Break-glass access revoked | grant_id, revoked_by |
| `AUTHZ_RESEARCH_GRANT_CREATED` | Research access granted | grant_id, identity_id, scope, phi_level |
| `AUTHZ_RESEARCH_GRANT_REVOKED` | Research access revoked | grant_id, revoked_by, reason |

## 12. Authorization Hardening Requirements

Authorization decisions are enforced centrally by Okapi, with IdP authentication delegated to institutional SSO (Keycloak used for demo OIDC/SAML). Okapi derives roles/permissions from its authoritative IAM store and uses them to augment access tokens used by downstream services.

Hardening requirements:
1. **Default-deny**: Unmapped IdP groups confer no access; permissions must be explicitly granted. (SYS-AUTHZ-010)
2. **Server-side enforcement**: All protected APIs perform authorization checks server-side; UI checks only inform UX. (SYS-AUTHZ-011)
3. **Token augmentation**: Okapi augments access tokens with derived roles/permissions and refreshes them on session renewal. (SYS-AUTHZ-012)

## 13. Security Considerations

1. **Least privilege**: Users receive minimum permissions necessary for their role
2. **Audit trail**: All authorization decisions and changes are logged
3. **Time-bounded access**: Temporary assignments automatically expire
4. **Break-glass accountability**: Emergency access requires justification and is fully audited
5. **PHI minimization**: Research grants enforce appropriate PHI exposure levels
6. **No credential storage**: Okapi does not create or store user credentials

## 14. Appendix: `okapi_authz_version` Format

`okapi_authz_version` identifies the active authorization mapping/policy used to compute roles and permissions. It is mandatory in augmented access tokens and must change when policy/mapping changes.

Recommended format:
- `YYYY.MM.DD+<short-hash>` (e.g., `2026.01.26+1a2b3c4`)
- `YYYY.MM.DD` reflects the policy baseline date
- `<short-hash>` is a truncated hash of the policy/config bundle

## 15. Policy Versioning Procedure (SOP Note)

When authorization mappings or policies change:
1. Update the policy/config bundle under change control.
2. Recompute the `okapi_authz_version` (`YYYY.MM.DD+<short-hash>`).
3. Deploy the updated policy and invalidate affected sessions if required by site policy.
4. Verify new tokens carry the updated `okapi_authz_version` and audit the change.

Change control reference: follow the project change-control SOP when present in the QMS (e.g., `SOP-CC`).

### 15.1 Example Token Payload (Claims Only)

```json
{
  "iss": "https://idp.example.org/realms/hospital",
  "sub": "f2c7b3b4-8c2a-4b89-9c49-1f0c0e1a2b3c",
  "aud": "okapi",
  "exp": 1767184200,
  "iat": 1767183600,
  "jti": "a3dcb4d2-3b0b-4a6d-bd46-2fd7f822f3d4",
  "roles": ["PATHOLOGIST", "RESEARCHER"],
  "permissions": ["CASE_VIEW", "CASE_SIGN_OUT", "RESEARCH_VIEW"],
  "okapi_authz_version": "2026.01.26+1a2b3c4",
  "auth_time": 1767180000,
  "acr": "urn:mace:incommon:iap:gold",
  "amr": ["pwd", "mfa"]
}
```

## 16. Traceability Matrix

| Design Element | User Need | System Requirement | Risk Control |
|----------------|-----------|-------------------|--------------|
| RBAC model | UN-AUTHZ-001 | SYS-AUTHZ-001 | RISK-010 |
| Fine-grained permissions | UN-AUTHZ-003 | SYS-AUTHZ-003 | RISK-010 |
| IdP group mapping | UN-AUTHZ-002 | SYS-AUTHZ-002 | RISK-010 |
| Time-bounded assignments | UN-AUTHZ-004 | SYS-AUTHZ-004 | RISK-011 |
| Admin access management | UN-AUTHZ-005 | SYS-AUTHZ-005, SYS-AUTHZ-006 | RISK-013, RISK-014 |
| Break-glass access | UN-AUTHZ-008, UN-AUTHZ-009 | SYS-AUTHZ-008, SYS-AUTHZ-009 | RISK-015 |
| Research access grants | UN-RES-001, UN-RES-002, UN-RES-003 | SYS-RES-001, SYS-RES-002, SYS-RES-003, SYS-RES-004 | RISK-016 |
| Authorization audit events | UN-AUTHZ-007 | SYS-AUTHZ-007, SYS-AUD-001, SYS-AUD-002 | RISK-006 |
| AuthZ hardening (default-deny, server-side enforcement, token augmentation) | UN-AUTHZ-001, UN-AUTHZ-003 | SYS-AUTHZ-010, SYS-AUTHZ-011, SYS-AUTHZ-012 | RISK-010 |

## 17. Component Implementation

| Component | Description |
|-----------|-------------|
| `RoleService` | Role CRUD, permission assignment |
| `IdentityRoleService` | Identity-role assignment with time bounds |
| `IdpGroupMappingService` | IdP group → role mapping management |
| `PermissionChecker` | Permission evaluation with caching |
| `BreakGlassService` | Break-glass grant lifecycle |
| `ResearchGrantService` | Research access grant lifecycle |
| `AuthzAuditService` | Authorization event emission |
