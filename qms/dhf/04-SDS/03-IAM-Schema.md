# 03-IAM-Schema

---
title: Software Design Specification - IAM Module Database Schema
document_id: DHF-04-03
version: 2.0
status: DRAFT
owner: Lead Architect
created_date: 2026-01-26
trace_source: SRS-001, DHF-04-01, DHF-04-02
---

> Database schema specification for the Identity and Access Management (IAM) module. This document defines the PostgreSQL schema that implements the authentication and authorization designs in [01-AuthN-Architecture.md](01-AuthN-Architecture.md) and [02-AuthZ-Architecture.md](02-AuthZ-Architecture.md).

## 1. Introduction

This document provides the detailed database schema specification for the IAM module. The schema supports:

- **Identity normalization**: Persisting authenticated user identities from external IdPs
- **Role-Based Access Control (RBAC)**: Managing roles, permissions, and assignments
- **IdP federation**: Mapping external IdP groups to internal roles
- **Device trust**: Managing trusted devices for session management
- **Access grants**: Break-glass and research access grants
- **Audit**: Recording authentication, authorization, and profile change events

## 2. Schema Architecture

### 2.1 Dedicated Schema

The IAM module uses a dedicated PostgreSQL schema (`iam`) to:
- Isolate IAM tables from clinical/domain tables
- Enable independent evolution and versioning
- Support clear ownership and access control at the database level
- Facilitate module-specific backup/restore if needed

```sql
CREATE SCHEMA IF NOT EXISTS iam;
```

### 2.2 Primary Key Strategy

All IAM tables use UUID primary keys (`gen_random_uuid()`) to:
- Avoid sequential ID enumeration attacks
- Support distributed ID generation
- Enable safe exposure in URLs and logs

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### 2.3 Timestamp Policy

- All timestamps use `TIMESTAMPTZ` (UTC instants)
- No actor/session time zone is stored; UI/API renders in user or institution preference
- Standard audit columns on all tables:

| Column | Type | Description |
|--------|------|-------------|
| `created_at` | TIMESTAMPTZ | Row creation timestamp, DEFAULT now() |
| `created_by_identity_id` | UUID | Identity that created the row (NULL for system) |
| `updated_at` | TIMESTAMPTZ | Last update timestamp, DEFAULT now() |
| `updated_by_identity_id` | UUID | Identity that last updated the row |

### 2.4 Design Policies

1. **No triggers**: Business logic is in application code for testability
2. **No passwords stored**: Authentication delegated to external IdPs
3. **No cross-schema foreign keys**: Domain tables reference IAM by ID, not FK
4. **Constraints + indexes only**: Schema enforces integrity, app enforces business rules

## 3. Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              IDENTITY CORE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐                           ┌─────────────────┐          │
│  │  iam.identity   │                           │ iam.session_    │          │
│  │                 │◀──────────────────────────│ device          │          │
│  │ identity_id(PK) │                           │                 │          │
│  │ provider_id     │                           │ device_id (PK)  │          │
│  │ external_subject│                           │ identity_id(FK) │          │
│  │ email, username │                           │ fingerprint_hash│          │
│  │ display_name    │                           │ trusted_until   │          │
│  │ attributes      │                           └─────────────────┘          │
│  └────────┬────────┘                                                        │
│           │                                                                  │
└───────────┼──────────────────────────────────────────────────────────────────┘
            │
┌───────────┼──────────────────────────────────────────────────────────────────┐
│           │                         RBAC                                     │
├───────────┼──────────────────────────────────────────────────────────────────┤
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐    │
│  │iam.identity_role│       │    iam.role     │       │ iam.permission  │    │
│  │                 │       │                 │       │                 │    │
│  │ identity_id(FK) │──────▶│ role_id (PK)    │       │ permission_id   │    │
│  │ role_id (FK)    │       │ name            │       │ name            │    │
│  │ assignment_src  │       │ is_system       │       │ description     │    │
│  │ effective_from  │       └────────┬────────┘       └────────┬────────┘    │
│  │ effective_to    │                │                         │             │
│  └─────────────────┘                │                         │             │
│                                     ▼                         ▼             │
│                            ┌─────────────────────────────────────┐          │
│                            │       iam.role_permission           │          │
│                            │                                     │          │
│                            │  role_id (FK, PK)                   │          │
│                            │  permission_id (FK, PK)             │          │
│                            └─────────────────────────────────────┘          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                           IDP GROUP FEDERATION                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐                           ┌─────────────────┐          │
│  │  iam.idp_group  │                           │iam.idp_group_   │          │
│  │                 │◀──────────────────────────│ role            │          │
│  │ idp_group_id(PK)│                           │                 │          │
│  │ provider_id     │                           │ idp_group_id(FK)│──────────┼──▶ iam.role
│  │ group_name      │                           │ role_id (FK)    │          │
│  └─────────────────┘                           └─────────────────┘          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                             ACCESS GRANTS                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────┐                   ┌─────────────────────┐          │
│  │iam.break_glass_grant│                   │iam.research_access_ │          │
│  │                     │                   │ grant               │          │
│  │ grant_id (PK)       │                   │                     │          │
│  │ identity_id (FK)    │                   │ grant_id (PK)       │          │
│  │ scope_entity_type   │                   │ identity_id (FK)    │          │
│  │ scope_entity_id     │                   │ scope_type          │          │
│  │ reason_code         │                   │ phi_access_level    │          │
│  │ expires_at          │                   │ protocol_id         │          │
│  └─────────────────────┘                   │ expires_at          │          │
│                                            └─────────────────────┘          │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                              AUDIT                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │                      iam.audit_event                            │        │
│  │                                                                 │        │
│  │  event_id (PK)          │  actor_identity_id (FK)               │        │
│  │  occurred_at            │  target_entity_type, target_entity_id │        │
│  │  event_type             │  outcome, outcome_reason              │        │
│  │  request_id, session_id │  ip_address, user_agent               │        │
│  │  details, metadata      │                                       │        │
│  └─────────────────────────────────────────────────────────────────┘        │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## 4. Table Specifications

### 4.1 `iam.identity`

The canonical representation of an authenticated user from any IdP.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `identity_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `provider_id` | TEXT | NOT NULL | Issuer/realm URL (e.g., Keycloak realm, OIDC issuer) |
| `external_subject` | TEXT | NOT NULL | Subject identifier within provider (OIDC `sub`, SAML NameID) |
| `username` | TEXT | NULL | Normalized username |
| `email` | TEXT | NULL | Email address |
| `display_name` | TEXT | NULL | Full display name |
| `display_short` | TEXT | NULL | Short display name (e.g., "Smith, J.") |
| `given_name` | TEXT | NULL | First name |
| `family_name` | TEXT | NULL | Last name |
| `is_active` | BOOLEAN | NOT NULL DEFAULT TRUE | Active flag |
| `last_seen_at` | TIMESTAMPTZ | NULL | Last activity timestamp |
| `attributes` | JSONB | NOT NULL DEFAULT '{}' | Extensible attributes (IdP claims, preferences) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT uq_iam_identity_provider_subject UNIQUE (provider_id, external_subject)
```

**Indexes:**
```sql
CREATE INDEX ix_iam_identity_email ON iam.identity (email);
CREATE INDEX ix_iam_identity_username ON iam.identity (username);
```

**Traceability:** UN-AUTHN-001, UN-AUTHN-007, UN-PROF-001, SYS-AUTHN-004, SYS-AUTHN-005

---

### 4.2 `iam.role`

System and custom roles for authorization decisions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `role_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `name` | TEXT | NOT NULL, UNIQUE | Role name (e.g., ADMIN, PATHOLOGIST) |
| `description` | TEXT | NULL | Human-readable description |
| `is_system` | BOOLEAN | NOT NULL DEFAULT FALSE | System-defined (not deletable) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT uq_iam_role_name UNIQUE (name)
```

**Traceability:** UN-AUTHZ-001, SYS-AUTHZ-001

---

### 4.3 `iam.permission`

Fine-grained permissions that can be assigned to roles.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `permission_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `name` | TEXT | NOT NULL, UNIQUE | Permission name (e.g., CASE_VIEW) |
| `description` | TEXT | NULL | Human-readable description |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT uq_iam_permission_name UNIQUE (name)
```

**Traceability:** UN-AUTHZ-003, SYS-AUTHZ-003

---

### 4.4 `iam.role_permission`

Many-to-many join between roles and permissions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `role_id` | UUID | PK, FK → iam.role | Role reference |
| `permission_id` | UUID | PK, FK → iam.permission | Permission reference |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |

**Constraints:**
```sql
PRIMARY KEY (role_id, permission_id),
CONSTRAINT fk_iam_role_permission_role
    FOREIGN KEY (role_id) REFERENCES iam.role(role_id) ON DELETE CASCADE,
CONSTRAINT fk_iam_role_permission_permission
    FOREIGN KEY (permission_id) REFERENCES iam.permission(permission_id) ON DELETE CASCADE
```

**Traceability:** UN-AUTHZ-003, SYS-AUTHZ-003

---

### 4.5 `iam.identity_role`

Identity ↔ Role assignment with provenance and time-bounds.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `identity_role_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `identity_id` | UUID | NOT NULL, FK → iam.identity | Identity reference |
| `role_id` | UUID | NOT NULL, FK → iam.role | Role reference |
| `assignment_source` | TEXT | NOT NULL DEFAULT 'LOCAL_ADMIN', CHECK | Source of assignment |
| `source_ref` | TEXT | NULL | Reference (IdP group name, ticket ID, etc.) |
| `effective_from` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Start of effectiveness |
| `effective_to` | TIMESTAMPTZ | NULL | End of effectiveness (NULL = indefinite) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT fk_iam_identity_role_identity
    FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE,
CONSTRAINT fk_iam_identity_role_role
    FOREIGN KEY (role_id) REFERENCES iam.role(role_id) ON DELETE CASCADE,
CONSTRAINT ck_iam_identity_role_assignment_source
    CHECK (assignment_source IN ('IDP_GROUP', 'LOCAL_ADMIN', 'BREAK_GLASS', 'SYSTEM')),
CONSTRAINT ck_iam_identity_role_effective_window
    CHECK (effective_to IS NULL OR effective_to > effective_from)
```

**Indexes:**
```sql
-- Enforce at most one active (open-ended) assignment per identity+role
CREATE UNIQUE INDEX uq_iam_identity_role_active
    ON iam.identity_role (identity_id, role_id)
    WHERE effective_to IS NULL;

CREATE INDEX ix_iam_identity_role_identity ON iam.identity_role (identity_id);
CREATE INDEX ix_iam_identity_role_role ON iam.identity_role (role_id);
```

**Traceability:** UN-AUTHZ-001, UN-AUTHZ-004, SYS-AUTHZ-001, SYS-AUTHZ-002, SYS-AUTHZ-004

---

### 4.6 `iam.idp_group`

Registry of IdP groups (issuer-scoped) for federation mapping.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `idp_group_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `provider_id` | TEXT | NOT NULL | IdP issuer identifier |
| `group_name` | TEXT | NOT NULL | Group name as claimed by IdP |
| `description` | TEXT | NULL | Human-readable description |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT uq_iam_idp_group_provider_name UNIQUE (provider_id, group_name)
```

**Traceability:** UN-AUTHZ-002, SYS-AUTHZ-002

---

### 4.7 `iam.idp_group_role`

Many-to-many mapping from IdP groups to internal roles.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `idp_group_id` | UUID | PK, FK → iam.idp_group | IdP group reference |
| `role_id` | UUID | PK, FK → iam.role | Role reference |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |

**Constraints:**
```sql
PRIMARY KEY (idp_group_id, role_id),
CONSTRAINT fk_iam_idp_group_role_group
    FOREIGN KEY (idp_group_id) REFERENCES iam.idp_group(idp_group_id) ON DELETE CASCADE,
CONSTRAINT fk_iam_idp_group_role_role
    FOREIGN KEY (role_id) REFERENCES iam.role(role_id) ON DELETE CASCADE
```

**Traceability:** UN-AUTHZ-002, SYS-AUTHZ-002, SYS-AUTHZ-005

---

### 4.8 `iam.session_device`

Trusted devices for "remember this device" functionality.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `device_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `identity_id` | UUID | NOT NULL, FK → iam.identity | Identity reference |
| `device_fingerprint_hash` | TEXT | NOT NULL | Hashed device fingerprint (no raw material) |
| `first_seen_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Initial trust establishment |
| `last_seen_at` | TIMESTAMPTZ | NULL | Most recent use |
| `trusted_until` | TIMESTAMPTZ | NULL | Trust expiration |
| `revoked_at` | TIMESTAMPTZ | NULL | When trust was revoked |
| `revoked_by_identity_id` | UUID | NULL | Who revoked |
| `metadata` | JSONB | NOT NULL DEFAULT '{}' | Additional context (browser, OS, etc.) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT fk_iam_session_device_identity
    FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE
```

**Indexes:**
```sql
CREATE UNIQUE INDEX uq_iam_session_device_identity_fingerprint
    ON iam.session_device (identity_id, device_fingerprint_hash);
CREATE INDEX ix_iam_session_device_identity ON iam.session_device (identity_id);
```

**Traceability:** UN-AUTHN-004, UN-AUTHN-005, SYS-AUTHN-009, SYS-AUTHN-010

---

### 4.9 `iam.break_glass_grant`

Time-bounded emergency access grants.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `grant_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `identity_id` | UUID | NOT NULL, FK → iam.identity | Grantee |
| `scope_entity_type` | TEXT | NOT NULL | Entity type (e.g., CASE, SPECIMEN) |
| `scope_entity_id` | UUID | NOT NULL | Entity identifier |
| `reason_code` | TEXT | NOT NULL | COVERAGE, EMERGENCY, CONSULT |
| `justification` | TEXT | NULL | Free-text explanation |
| `granted_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Grant timestamp |
| `expires_at` | TIMESTAMPTZ | NOT NULL | Expiration timestamp |
| `revoked_at` | TIMESTAMPTZ | NULL | Revocation timestamp |
| `revoked_by_identity_id` | UUID | NULL | Who revoked |
| `metadata` | JSONB | NOT NULL DEFAULT '{}' | Additional context |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT fk_iam_break_glass_grant_identity
    FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE,
CONSTRAINT ck_iam_break_glass_expiry CHECK (expires_at > granted_at)
```

**Indexes:**
```sql
CREATE INDEX ix_iam_break_glass_scope
    ON iam.break_glass_grant (scope_entity_type, scope_entity_id, expires_at);
CREATE INDEX ix_iam_break_glass_identity
    ON iam.break_glass_grant (identity_id, expires_at);
```

**Traceability:** UN-AUTHZ-008, UN-AUTHZ-009, SYS-WL-007

---

### 4.10 `iam.research_access_grant`

Governed, time-bounded research access with PHI exposure control.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `grant_id` | UUID | PK, DEFAULT gen_random_uuid() | Internal primary key |
| `identity_id` | UUID | NOT NULL, FK → iam.identity | Grantee |
| `scope_type` | TEXT | NOT NULL | BLOCK, SPECIMEN, COHORT |
| `scope_entity_id` | UUID | NULL | Entity ID (NULL for cohort/protocol-wide) |
| `scope_filter` | JSONB | NULL | Filter criteria for cohort grants |
| `protocol_id` | TEXT | NULL | IRB protocol, clinical trial ID |
| `reason` | TEXT | NOT NULL | Justification |
| `approved_by_identity_id` | UUID | NULL, FK → iam.identity | Approver |
| `phi_access_level` | TEXT | NOT NULL, CHECK | NONE, MASKED, LIMITED, FULL |
| `granted_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Grant timestamp |
| `expires_at` | TIMESTAMPTZ | NOT NULL | Expiration timestamp |
| `revoked_at` | TIMESTAMPTZ | NULL | Revocation timestamp |
| `revoked_by_identity_id` | UUID | NULL | Who revoked |
| `revocation_reason` | TEXT | NULL | Why revoked |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Row creation |
| `created_by_identity_id` | UUID | NULL | Creator identity |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Last update |
| `updated_by_identity_id` | UUID | NULL | Last updater identity |

**Constraints:**
```sql
CONSTRAINT fk_iam_research_grant_identity
    FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE,
CONSTRAINT fk_iam_research_grant_approver
    FOREIGN KEY (approved_by_identity_id) REFERENCES iam.identity(identity_id),
CONSTRAINT ck_iam_research_grant_phi_level
    CHECK (phi_access_level IN ('NONE', 'MASKED', 'LIMITED', 'FULL')),
CONSTRAINT ck_iam_research_grant_expiry CHECK (expires_at > granted_at)
```

**Indexes:**
```sql
CREATE INDEX ix_iam_research_grant_identity ON iam.research_access_grant (identity_id, expires_at);
CREATE INDEX ix_iam_research_grant_protocol ON iam.research_access_grant (protocol_id);
```

**Traceability:** UN-RES-001, UN-RES-002, UN-RES-003, UN-RES-004

---

### 4.11 `iam.audit_event`

Authentication, authorization, and profile change event recording.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `event_id` | UUID | PK, DEFAULT gen_random_uuid() | Event identifier |
| `occurred_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Event timestamp |
| `event_type` | TEXT | NOT NULL | Event classification (e.g., AUTHN_LOGIN_SUCCESS) |
| `actor_identity_id` | UUID | NULL, FK → iam.identity | Who performed action |
| `actor_provider_id` | TEXT | NULL | Actor's provider (for pre-identity events) |
| `actor_external_subject` | TEXT | NULL | Actor's subject (for pre-identity events) |
| `target_entity_type` | TEXT | NULL | What type was affected |
| `target_entity_id` | UUID | NULL | What was affected |
| `target_identity_id` | UUID | NULL, FK → iam.identity | Target identity (if applicable) |
| `outcome` | TEXT | NULL | SUCCESS, FAILURE, DENIED |
| `outcome_reason` | TEXT | NULL | Reason for outcome |
| `request_id` | UUID | NULL | Request correlation ID |
| `session_id` | UUID | NULL | Session correlation ID |
| `ip_address` | INET | NULL | Client IP |
| `user_agent` | TEXT | NULL | Client user agent |
| `details` | TEXT | NULL | Human-readable details |
| `metadata` | JSONB | NOT NULL DEFAULT '{}' | Structured details |

**Indexes:**
```sql
CREATE INDEX ix_iam_audit_event_occurred ON iam.audit_event (occurred_at);
CREATE INDEX ix_iam_audit_event_type ON iam.audit_event (event_type);
CREATE INDEX ix_iam_audit_event_actor ON iam.audit_event (actor_identity_id);
CREATE INDEX ix_iam_audit_event_target ON iam.audit_event (target_entity_type, target_entity_id);
CREATE INDEX ix_iam_audit_event_request ON iam.audit_event (request_id);
```

**Traceability:** UN-AUTHN-006, UN-AUTHZ-007, UN-PROF-003, UN-RES-004, SYS-AUD-001, SYS-AUD-002

---

## 5. Seed Data

### 5.1 System Roles

| Role | Description | is_system |
|------|-------------|-----------|
| ADMIN | System Administrator with full access | true |
| PATHOLOGIST | Medical Pathologist - View Cases, Sign-out | true |
| TECHNICIAN | Lab Technician - Ingestion, Processing | true |
| RESIDENT | Pathology Resident - View Cases, Draft Reports | true |
| FELLOW | Pathology Fellow - View Cases, Edit Reports | true |
| HISTO_TECH | Histology Technician | true |
| CYTO_TECH | Cytology Technician | true |
| RESEARCHER | Research User - Limited read access | true |
| RESEARCH_ADMIN | Research Administrator - Manage research grants | true |

### 5.2 System Permissions

| Permission | Description |
|------------|-------------|
| CASE_VIEW | View case details and worklist |
| CASE_EDIT | Edit case data and reports |
| CASE_SIGN_OUT | Sign out / finalize cases |
| CASE_REASSIGN | Reassign cases to other users |
| HISTO_VIEW | View histology data |
| HISTO_EDIT | Edit histology data and status |
| HISTO_BATCH | Create and manage stain batches |
| HISTO_QA | Perform QA on stain batches |
| RESEARCH_VIEW | View research suitability metadata |
| RESEARCH_REQUEST | Request research access grants |
| RESEARCH_APPROVE | Approve research access grants |
| ADMIN_USERS | Manage user identities and roles |
| ADMIN_SYSTEM | System configuration and settings |
| ADMIN_AUDIT | View audit logs |
| BREAK_GLASS_INVOKE | Invoke break-glass access |
| PROFILE_VIEW_OWN | View own profile |
| PROFILE_EDIT_OWN | Edit own profile preferences |
| PROFILE_VIEW_ANY | View any user profile (admin) |
| PROFILE_EDIT_ANY | Edit any user profile (admin) |

## 6. Migration Strategy

### 6.1 Flyway Migration Path

```
db/modules/iam/
├── V1__iam_init_schema.sql       # Create iam schema and all tables
├── V2__iam_seed_reference_data.sql   # Seed roles, permissions, IdP mappings
└── V3__iam_add_research_grants.sql   # Add research_access_grant table
```

### 6.2 Migration from Legacy Schema

If migrating from the existing `public` schema tables:

1. Create new `iam` schema and tables
2. Migrate data from `public.identities` → `iam.identity`
3. Migrate data from `public.roles` → `iam.role`
4. Migrate data from `public.identity_roles` → `iam.identity_role`
5. Migrate IdP group mappings
6. Update application code to use new schema
7. Deprecate and eventually drop legacy tables

## 7. Traceability Matrix

| Requirement | IAM Component | Verification |
|-------------|---------------|--------------|
| SYS-AUTHZ-001 | iam.role, iam.permission, iam.identity_role | Test |
| SYS-AUTHN-004 | iam.identity | Test |
| SYS-AUTHN-005 | iam.identity UNIQUE constraint | Inspection |
| SYS-AUTHN-009 | iam.session_device | Test |
| SYS-AUTHZ-002 | iam.idp_group, iam.idp_group_role | Test |
| SYS-AUTHZ-005 | iam.idp_group_role (ADMIN mapping) | Analysis/Test |
| SYS-AUD-001 | iam.audit_event | Test |
| SYS-AUD-002 | iam.audit_event schema | Inspection |
| SYS-WL-007 | iam.break_glass_grant | Test |
| UN-AUTHN-004 | iam.session_device | Test |
| UN-AUTHZ-004 | iam.identity_role.effective_from/to | Test |
| UN-RES-001 | iam.research_access_grant | Test |
| UN-RES-002 | iam.research_access_grant.phi_access_level | Test |

## 8. Security Considerations

1. **No cross-schema FKs**: IAM tables do not have foreign keys to clinical/domain tables; domain tables reference `iam.identity.identity_id` as needed.
2. **Least privilege**: Database roles for the application should have minimal required permissions on the `iam` schema.
3. **Audit immutability**: Audit events should be append-only; application should not UPDATE/DELETE audit records.
4. **PHI in audit**: Audit events should not contain PHI in plaintext; use entity references instead.
5. **Device fingerprint hashing**: Only hashed fingerprints are stored; raw fingerprint material is never persisted.
6. **Preference security**: User preferences in JSONB are validated to prevent injection of executable content.
