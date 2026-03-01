-- Case-pathologist assignment table (SDS §4.10, SRS §1.12, PURS §3.7)
-- Links pathologists to cases with role-based assignments.
-- wsi.case_pathologists is the authoritative source of truth for case responsibility.

CREATE TABLE IF NOT EXISTS wsi.case_pathologists (
    id              uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id         uuid            NOT NULL,
    identity_id     uuid            NOT NULL,
    role            varchar(32)     NOT NULL,
    sequence        integer         NOT NULL DEFAULT 1,
    assigned_at     timestamptz     NOT NULL DEFAULT now(),
    assigned_by     uuid            NULL,

    CONSTRAINT fk_case   FOREIGN KEY (case_id)     REFERENCES wsi.cases(id)              ON DELETE CASCADE,
    CONSTRAINT fk_ident  FOREIGN KEY (identity_id)  REFERENCES iam.identity(identity_id)  ON DELETE RESTRICT,
    CONSTRAINT fk_by     FOREIGN KEY (assigned_by)  REFERENCES iam.identity(identity_id),
    CONSTRAINT ck_role   CHECK (role IN ('PRIMARY','SECONDARY','CONSULTING','RESIDENT','FELLOW')),
    CONSTRAINT uq_case_identity UNIQUE (case_id, identity_id)
);

-- SYS-CA-002: exactly one PRIMARY per case
CREATE UNIQUE INDEX uq_case_primary ON wsi.case_pathologists (case_id) WHERE role = 'PRIMARY';

-- Lookup indexes
CREATE INDEX ix_identity ON wsi.case_pathologists (identity_id);
CREATE INDEX ix_role     ON wsi.case_pathologists (role);
