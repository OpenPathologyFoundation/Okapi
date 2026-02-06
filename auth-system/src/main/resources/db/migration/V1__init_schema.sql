-- Okapi IAM schema (managed by Flyway)

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS iam;

-- ---------------------------------------------------------------------------
-- Identity: issuer-scoped subject, normalized claims
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.identity (
    identity_id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id              text NOT NULL,
    external_subject         text NOT NULL,

    username                 text NULL,
    email                    text NULL,
    display_name             text NULL,
    display_short            text NULL,
    given_name               text NULL,
    family_name              text NULL,
    middle_name              text NULL,
    prefix                   text NULL,
    suffix                   text NULL,

    is_active                boolean NOT NULL DEFAULT true,
    last_seen_at             timestamptz NULL,

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

    assignment_source        text NOT NULL DEFAULT 'LOCAL_ADMIN',
    source_ref               text NULL,

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
        CHECK (assignment_source IN ('IDP_GROUP', 'LOCAL_ADMIN', 'BREAK_GLASS', 'SYSTEM')),

    CONSTRAINT ck_iam_identity_role_effective_window
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

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
-- Trusted devices (remember this device)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.session_device (
    device_id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_id              uuid NOT NULL,

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
-- Break-glass grants
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.break_glass_grant (
    grant_id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_id              uuid NOT NULL REFERENCES iam.identity,

    scope_entity_type        text NOT NULL,
    scope_entity_id          uuid NOT NULL,

    reason_code              text NOT NULL,
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

    CONSTRAINT ck_iam_break_glass_expiry CHECK (expires_at > granted_at)
);

CREATE INDEX IF NOT EXISTS ix_iam_break_glass_identity
    ON iam.break_glass_grant (identity_id, expires_at);

-- ---------------------------------------------------------------------------
-- Research access grants
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.research_access_grant (
    grant_id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_id              uuid NOT NULL REFERENCES iam.identity,

    scope_type               text NOT NULL,
    scope_entity_id          uuid NULL,
    scope_filter             jsonb NULL,

    protocol_id              text NULL,
    reason                   text NOT NULL,
    approved_by_identity_id  uuid NULL REFERENCES iam.identity,

    phi_access_level         text NOT NULL CHECK (phi_access_level IN
                             ('NONE', 'MASKED', 'LIMITED', 'FULL')),

    granted_at               timestamptz NOT NULL DEFAULT now(),
    expires_at               timestamptz NOT NULL,

    revoked_at               timestamptz NULL,
    revoked_by_identity_id   uuid NULL,
    revocation_reason        text NULL,

    created_at               timestamptz NOT NULL DEFAULT now(),
    created_by_identity_id   uuid NULL,
    updated_at               timestamptz NOT NULL DEFAULT now(),
    updated_by_identity_id   uuid NULL
);

CREATE INDEX IF NOT EXISTS ix_iam_research_grant_identity
    ON iam.research_access_grant (identity_id, expires_at);

CREATE INDEX IF NOT EXISTS ix_iam_research_grant_protocol
    ON iam.research_access_grant (protocol_id);

-- ---------------------------------------------------------------------------
-- Audit events
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam.audit_event (
    event_id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at              timestamptz NOT NULL DEFAULT now(),
    event_type               text NOT NULL,

    actor_identity_id        uuid NULL REFERENCES iam.identity,
    actor_provider_id        text NULL,
    actor_external_subject   text NULL,

    target_entity_type       text NULL,
    target_entity_id         uuid NULL,
    target_identity_id       uuid NULL REFERENCES iam.identity,

    outcome                  text NULL,
    outcome_reason           text NULL,

    request_id               uuid NULL,
    session_id               uuid NULL,
    ip_address               inet NULL,
    user_agent               text NULL,
    details                  text NULL,
    metadata                 jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS ix_iam_audit_event_occurred
    ON iam.audit_event (occurred_at);

CREATE INDEX IF NOT EXISTS ix_iam_audit_event_type
    ON iam.audit_event (event_type);

CREATE INDEX IF NOT EXISTS ix_iam_audit_event_actor
    ON iam.audit_event (actor_identity_id);

CREATE INDEX IF NOT EXISTS ix_iam_audit_event_target
    ON iam.audit_event (target_entity_type, target_entity_id);

CREATE INDEX IF NOT EXISTS ix_iam_audit_event_request
    ON iam.audit_event (request_id);
