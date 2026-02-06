-- db/modules/iam/migration/V1__init.sql
-- IAM (Identity & Access Management) module schema
--
-- Time policy:
--   * All timestamps are stored as UTC instants using timestamptz.
--   * We do NOT store actor/session time zone or location.
--   * UI/API may render in institution time zone or user preference.
--
-- Design policy:
--   * No triggers.
--   * No passwords stored; authentication is handled by external IdPs.
--   * Constraints + indexes only.
--   * No cross-schema foreign keys; IAM is a source of identity_id/roles/permissions.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS iam;

-- ---------------------------------------------------------------------------
-- Identity: issuer-scoped subject, normalized claims
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.identity (
                                            identity_id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Issuer / provider namespace (e.g., Keycloak realm URL, SAML entityID, OIDC issuer)
                                            provider_id              text NOT NULL,
    -- Subject identifier within that provider namespace (OIDC "sub", SAML NameID, etc.)
                                            external_subject         text NOT NULL,

    -- Common normalized attributes (not authoritative; derived from IdP claims)
                                            username                 text NULL,
                                            email                    text NULL,
                                            display_name             text NULL,
                                            display_short            text NULL,
                                            given_name               text NULL,
                                            family_name              text NULL,

                                            is_active                boolean NOT NULL DEFAULT true,
                                            last_seen_at             timestamptz NULL,

    -- Additional IdP/derived attributes (claims subset, flags, etc.)
                                            attributes               jsonb NOT NULL DEFAULT '{}'::jsonb,

                                            created_at               timestamptz NOT NULL DEFAULT now(),
                                            created_by_identity_id   uuid NULL,
                                            updated_at               timestamptz NOT NULL DEFAULT now(),
                                            updated_by_identity_id   uuid NULL,

                                            CONSTRAINT uq_iam_identity_provider_subject
                                                UNIQUE (provider_id, external_subject)
);

CREATE INDEX IF NOT EXISTS ix_iam_identity_email
    ON iam.identity (email);

CREATE INDEX IF NOT EXISTS ix_iam_identity_username
    ON iam.identity (username);

-- ---------------------------------------------------------------------------
-- RBAC primitives
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.role (
                                        role_id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                        name                     text NOT NULL,
                                        description              text NULL,
                                        is_system                boolean NOT NULL DEFAULT false,

                                        created_at               timestamptz NOT NULL DEFAULT now(),
                                        created_by_identity_id   uuid NULL,
                                        updated_at               timestamptz NOT NULL DEFAULT now(),
                                        updated_by_identity_id   uuid NULL,

                                        CONSTRAINT uq_iam_role_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS iam.permission (
                                              permission_id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                              name                     text NOT NULL,
                                              description              text NULL,

                                              created_at               timestamptz NOT NULL DEFAULT now(),
                                              created_by_identity_id   uuid NULL,
                                              updated_at               timestamptz NOT NULL DEFAULT now(),
                                              updated_by_identity_id   uuid NULL,

                                              CONSTRAINT uq_iam_permission_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS iam.role_permission (
                                                   role_id                  uuid NOT NULL,
                                                   permission_id            uuid NOT NULL,

                                                   created_at               timestamptz NOT NULL DEFAULT now(),
                                                   created_by_identity_id   uuid NULL,

                                                   PRIMARY KEY (role_id, permission_id),

                                                   CONSTRAINT fk_iam_role_permission_role
                                                       FOREIGN KEY (role_id) REFERENCES iam.role(role_id) ON DELETE CASCADE,
                                                   CONSTRAINT fk_iam_role_permission_permission
                                                       FOREIGN KEY (permission_id) REFERENCES iam.permission(permission_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Identity ↔ Role assignment (local + IdP-derived + time-bounded)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.identity_role (
                                                 identity_role_id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                                                 identity_id              uuid NOT NULL,
                                                 role_id                  uuid NOT NULL,

    -- Provenance of the assignment (kept as text with CHECK to avoid enum friction)
                                                 assignment_source        text NOT NULL DEFAULT 'LOCAL_ADMIN',
                                                 source_ref               text NULL,  -- IdP group name, ticket ID, policy ID, etc.

                                                 effective_from           timestamptz NOT NULL DEFAULT now(),
                                                 effective_to             timestamptz NULL,

                                                 created_at               timestamptz NOT NULL DEFAULT now(),
                                                 created_by_identity_id   uuid NULL,
                                                 updated_at               timestamptz NOT NULL DEFAULT now(),
                                                 updated_by_identity_id   uuid NULL,

                                                 CONSTRAINT fk_iam_identity_role_identity
                                                     FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE,
                                                 CONSTRAINT fk_iam_identity_role_role
                                                     FOREIGN KEY (role_id) REFERENCES iam.role(role_id) ON DELETE CASCADE,

                                                 CONSTRAINT ck_iam_identity_role_assignment_source
                                                     CHECK (assignment_source IN ('IDP_GROUP','LOCAL_ADMIN','BREAK_GLASS','SYSTEM')),

                                                 CONSTRAINT ck_iam_identity_role_effective_window
                                                     CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- Enforce at most one active (open-ended) assignment per identity+role
CREATE UNIQUE INDEX IF NOT EXISTS uq_iam_identity_role_active
    ON iam.identity_role (identity_id, role_id)
    WHERE effective_to IS NULL;

CREATE INDEX IF NOT EXISTS ix_iam_identity_role_identity
    ON iam.identity_role (identity_id);

CREATE INDEX IF NOT EXISTS ix_iam_identity_role_role
    ON iam.identity_role (role_id);

-- ---------------------------------------------------------------------------
-- IdP group mappings (issuer-scoped)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.idp_group (
                                             idp_group_id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                             provider_id              text NOT NULL,
                                             group_name               text NOT NULL,
                                             description              text NULL,

                                             created_at               timestamptz NOT NULL DEFAULT now(),
                                             created_by_identity_id   uuid NULL,
                                             updated_at               timestamptz NOT NULL DEFAULT now(),
                                             updated_by_identity_id   uuid NULL,

                                             CONSTRAINT uq_iam_idp_group_provider_name
                                                 UNIQUE (provider_id, group_name)
);

CREATE TABLE IF NOT EXISTS iam.idp_group_role (
                                                  idp_group_id             uuid NOT NULL,
                                                  role_id                  uuid NOT NULL,

                                                  created_at               timestamptz NOT NULL DEFAULT now(),
                                                  created_by_identity_id   uuid NULL,

                                                  PRIMARY KEY (idp_group_id, role_id),

                                                  CONSTRAINT fk_iam_idp_group_role_group
                                                      FOREIGN KEY (idp_group_id) REFERENCES iam.idp_group(idp_group_id) ON DELETE CASCADE,
                                                  CONSTRAINT fk_iam_idp_group_role_role
                                                      FOREIGN KEY (role_id) REFERENCES iam.role(role_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Trusted devices ("remember this device")
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.session_device (
                                                  device_id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  identity_id              uuid NOT NULL,

    -- Store a stable, non-reversible fingerprint hash (no raw fingerprint material)
                                                  device_fingerprint_hash  text NOT NULL,

                                                  first_seen_at            timestamptz NOT NULL DEFAULT now(),
                                                  last_seen_at             timestamptz NULL,

                                                  trusted_until            timestamptz NULL,
                                                  revoked_at               timestamptz NULL,
                                                  revoked_by_identity_id   uuid NULL,

                                                  metadata                 jsonb NOT NULL DEFAULT '{}'::jsonb,

                                                  created_at               timestamptz NOT NULL DEFAULT now(),
                                                  created_by_identity_id   uuid NULL,
                                                  updated_at               timestamptz NOT NULL DEFAULT now(),
                                                  updated_by_identity_id   uuid NULL,

                                                  CONSTRAINT fk_iam_session_device_identity
                                                      FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_iam_session_device_identity_fingerprint
    ON iam.session_device (identity_id, device_fingerprint_hash);

CREATE INDEX IF NOT EXISTS ix_iam_session_device_identity
    ON iam.session_device (identity_id);

-- ---------------------------------------------------------------------------
-- Break-glass grants (time-bounded access for urgent/coverage situations)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.break_glass_grant (
                                                     grant_id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     identity_id              uuid NOT NULL,

    -- Scope is generic: identity is granted temporary access to a single entity
                                                     scope_entity_type        text NOT NULL,  -- e.g., 'CASE'
                                                     scope_entity_id          uuid NOT NULL,

                                                     reason_code              text NOT NULL,  -- e.g., 'COVERAGE','EMERGENCY'
                                                     justification            text NULL,

                                                     granted_at               timestamptz NOT NULL DEFAULT now(),
                                                     expires_at               timestamptz NOT NULL,

                                                     revoked_at               timestamptz NULL,
                                                     revoked_by_identity_id   uuid NULL,

                                                     metadata                 jsonb NOT NULL DEFAULT '{}'::jsonb,

                                                     created_at               timestamptz NOT NULL DEFAULT now(),
                                                     created_by_identity_id   uuid NULL,
                                                     updated_at               timestamptz NOT NULL DEFAULT now(),
                                                     updated_by_identity_id   uuid NULL,

                                                     CONSTRAINT fk_iam_break_glass_grant_identity
                                                         FOREIGN KEY (identity_id) REFERENCES iam.identity(identity_id) ON DELETE CASCADE,

                                                     CONSTRAINT ck_iam_break_glass_expiry
                                                         CHECK (expires_at > granted_at)
);

CREATE INDEX IF NOT EXISTS ix_iam_break_glass_scope
    ON iam.break_glass_grant (scope_entity_type, scope_entity_id, expires_at);

CREATE INDEX IF NOT EXISTS ix_iam_break_glass_identity
    ON iam.break_glass_grant (identity_id, expires_at);

COMMIT;