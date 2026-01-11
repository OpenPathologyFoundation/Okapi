-- Okapi AuthN/AuthZ schema (managed by Flyway)
-- Based on the proposed `console_2.sql` schema.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------
-- Roles
-- -----------------------------
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),

    is_system   BOOLEAN      NOT NULL DEFAULT TRUE,
    metadata    JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_roles_name ON roles (name);

-- -----------------------------
-- Identities
-- -----------------------------
-- external_subject should be unique per provider/issuer, not globally.
CREATE TABLE IF NOT EXISTS identities (
    id               BIGSERIAL PRIMARY KEY,

    -- stable internal identifier safe to expose in URLs/logs
    euid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,

    -- external identity reference (e.g., Keycloak token "sub")
    external_subject VARCHAR(255) NOT NULL,

    -- issuer / realm / provider identifier (e.g., Keycloak realm URL)
    provider_id      VARCHAR(255) NOT NULL,

    -- human-facing fields
    display_name     VARCHAR(100),
    given_name       VARCHAR(60),
    family_name      VARCHAR(60),

    -- email is often missing for service accounts; keep unique when present
    email            VARCHAR(100) UNIQUE,

    -- account lifecycle / environment
    account_type     VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION'
        CHECK (account_type IN ('PRODUCTION', 'TEST', 'DEMO', 'SERVICE')),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED', 'PENDING')),

    -- operational flags
    is_test_user        BOOLEAN     NOT NULL DEFAULT FALSE,
    is_demo_user        BOOLEAN     NOT NULL DEFAULT FALSE,
    break_glass_enabled BOOLEAN     NOT NULL DEFAULT FALSE,

    -- future-proof attributes without schema churn
    attributes       JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ,
    last_login_at    TIMESTAMPTZ,
    last_seen_at     TIMESTAMPTZ,
    disabled_at      TIMESTAMPTZ,

    CONSTRAINT uq_identities_provider_subject UNIQUE (provider_id, external_subject)
);

CREATE INDEX IF NOT EXISTS idx_identities_provider_subject ON identities (provider_id, external_subject);
CREATE INDEX IF NOT EXISTS idx_identities_email ON identities (email);
CREATE INDEX IF NOT EXISTS idx_identities_account_type ON identities (account_type);
CREATE INDEX IF NOT EXISTS idx_identities_status ON identities (status);

-- -----------------------------
-- Identity ↔ Roles (many-to-many)
-- -----------------------------
CREATE TABLE IF NOT EXISTS identity_roles (
    identity_id BIGINT NOT NULL REFERENCES identities ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES roles ON DELETE CASCADE,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    assignment_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
        CHECK (assignment_source IN ('MANUAL', 'IDP_GROUP', 'IMPORT', 'SYSTEM')),
    assigned_by_identity_id BIGINT REFERENCES identities ON DELETE SET NULL,

    effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    effective_to   TIMESTAMPTZ,

    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (identity_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_identity_roles_role_id ON identity_roles (role_id);
CREATE INDEX IF NOT EXISTS idx_identity_roles_identity_id ON identity_roles (identity_id);
CREATE INDEX IF NOT EXISTS idx_identity_roles_effective ON identity_roles (effective_from, effective_to);

-- -----------------------------
-- Permission groups
-- -----------------------------
CREATE TABLE IF NOT EXISTS permission_groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),

    group_type  VARCHAR(30) NOT NULL DEFAULT 'SERVICE_LINE'
        CHECK (group_type IN ('SERVICE_LINE', 'SITE', 'TEAM', 'CUSTOM')),

    metadata    JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS identity_permission_groups (
    identity_id BIGINT NOT NULL REFERENCES identities ON DELETE CASCADE,
    group_id    BIGINT NOT NULL REFERENCES permission_groups ON DELETE CASCADE,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    assignment_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
        CHECK (assignment_source IN ('MANUAL', 'IDP_GROUP', 'IMPORT', 'SYSTEM')),
    assigned_by_identity_id BIGINT REFERENCES identities ON DELETE SET NULL,

    PRIMARY KEY (identity_id, group_id)
);

CREATE INDEX IF NOT EXISTS idx_identity_permission_groups_group_id ON identity_permission_groups (group_id);

-- -----------------------------
-- IdP group mappings
-- -----------------------------
CREATE TABLE IF NOT EXISTS idp_group_mappings (
    id             BIGSERIAL PRIMARY KEY,
    provider_id    VARCHAR(255) NOT NULL,
    idp_group_name VARCHAR(100) NOT NULL,

    metadata       JSONB        NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT uq_idp_group_provider_name UNIQUE (provider_id, idp_group_name)
);

CREATE TABLE IF NOT EXISTS idp_group_role_mappings (
    idp_group_mapping_id BIGINT NOT NULL REFERENCES idp_group_mappings ON DELETE CASCADE,
    role_id              BIGINT NOT NULL REFERENCES roles ON DELETE CASCADE,
    PRIMARY KEY (idp_group_mapping_id, role_id)
);

CREATE TABLE IF NOT EXISTS idp_group_permission_group_mappings (
    idp_group_mapping_id BIGINT NOT NULL REFERENCES idp_group_mappings ON DELETE CASCADE,
    group_id             BIGINT NOT NULL REFERENCES permission_groups ON DELETE CASCADE,
    PRIMARY KEY (idp_group_mapping_id, group_id)
);

-- -----------------------------
-- Audit events
-- -----------------------------
CREATE TABLE IF NOT EXISTS audit_events (
    id          BIGSERIAL PRIMARY KEY,
    event_id    UUID        NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_type  VARCHAR(50) NOT NULL,

    actor_identity_id BIGINT REFERENCES identities ON DELETE SET NULL,
    target_identity_id BIGINT REFERENCES identities ON DELETE SET NULL,

    outcome     VARCHAR(50),
    request_id  UUID,
    ip          INET,
    user_agent  TEXT,
    details     TEXT,
    metadata    JSONB       NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_at ON audit_events (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_event_type ON audit_events (event_type);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events (actor_identity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_target ON audit_events (target_identity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_request_id ON audit_events (request_id);
