-- Educational WSI Collection schema (SDS-EDU-001 §4)
-- Implements the data model for educational teaching cases.
-- Mirrors wsi.* structure but adapted for pathology education:
-- no patient_id, source_lineage for provenance, curator assignments,
-- named collections for curriculum organization.

CREATE SCHEMA IF NOT EXISTS wsi_edu;

-- ---------------------------------------------------------------------------
-- Cases: accession-level container for educational teaching slides
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.cases (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id              varchar(64)  NOT NULL,
    collection           varchar(16)  NOT NULL DEFAULT 'educational',
    specimen_type        varchar(255) NULL,
    clinical_history     text         NULL,
    accession_date       date         NULL,
    ingested_at          timestamptz  NOT NULL DEFAULT now(),
    status               varchar(32)  NOT NULL DEFAULT 'active',
    priority             varchar(16)  NULL,
    source_lineage       jsonb        NULL     DEFAULT '{}'::jsonb,
    metadata             jsonb        NULL     DEFAULT '{}'::jsonb,

    CONSTRAINT uq_wsi_edu_cases_case_id
        UNIQUE (case_id),

    CONSTRAINT ck_wsi_edu_cases_collection
        CHECK (collection = 'educational'),

    CONSTRAINT ck_wsi_edu_cases_status
        CHECK (status IN ('active', 'archived', 'draft'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_cases_metadata
    ON wsi_edu.cases USING gin (metadata);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_cases_source_lineage
    ON wsi_edu.cases USING gin (source_lineage);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_cases_status
    ON wsi_edu.cases (status);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_cases_ingested_at
    ON wsi_edu.cases (ingested_at);

-- ---------------------------------------------------------------------------
-- Parts: specimen parts within an educational case
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.parts (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id              uuid         NOT NULL,
    part_label           varchar(16)  NOT NULL,
    part_designator      varchar(255) NULL,
    anatomic_site        varchar(128) NULL,
    final_diagnosis      text         NULL,
    gross_description    text         NULL,
    provenance           varchar(16)  NOT NULL DEFAULT 'IMPLIED',
    metadata             jsonb        NULL     DEFAULT '{}'::jsonb,

    CONSTRAINT fk_wsi_edu_parts_case
        FOREIGN KEY (case_id) REFERENCES wsi_edu.cases(id) ON DELETE CASCADE,

    CONSTRAINT ck_wsi_edu_parts_provenance
        CHECK (provenance IN ('ACCESSIONED', 'IMPLIED', 'CURATED'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_parts_case_id
    ON wsi_edu.parts (case_id);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_parts_anatomic_site
    ON wsi_edu.parts (anatomic_site);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_parts_diagnosis_fts
    ON wsi_edu.parts USING gin (to_tsvector('english', coalesce(final_diagnosis, '')));

-- ---------------------------------------------------------------------------
-- ICD codes: case-level diagnostic coding
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.case_icd_codes (
    case_id              uuid        NOT NULL,
    icd_code             varchar(16) NOT NULL,
    code_system          varchar(16) NOT NULL,
    code_description     varchar(255) NULL,

    PRIMARY KEY (case_id, icd_code, code_system),

    CONSTRAINT fk_wsi_edu_case_icd_codes_case
        FOREIGN KEY (case_id) REFERENCES wsi_edu.cases(id) ON DELETE CASCADE,

    CONSTRAINT ck_wsi_edu_case_icd_codes_system
        CHECK (code_system IN ('ICD-10', 'ICD-O-3', 'SNOMED'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_case_icd_codes_code
    ON wsi_edu.case_icd_codes (icd_code);

-- ---------------------------------------------------------------------------
-- Blocks: tissue blocks within a part
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.blocks (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    part_id              uuid        NOT NULL,
    block_label          varchar(16) NOT NULL,
    block_description    text        NULL,
    provenance           varchar(16) NOT NULL DEFAULT 'IMPLIED',

    CONSTRAINT fk_wsi_edu_blocks_part
        FOREIGN KEY (part_id) REFERENCES wsi_edu.parts(id) ON DELETE CASCADE,

    CONSTRAINT ck_wsi_edu_blocks_provenance
        CHECK (provenance IN ('ACCESSIONED', 'IMPLIED', 'CURATED'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_blocks_part_id
    ON wsi_edu.blocks (part_id);

-- ---------------------------------------------------------------------------
-- Slides: physical WSI files on disk
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.slides (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    block_id             uuid         NOT NULL,
    slide_id             varchar(64)  NOT NULL,
    relative_path        varchar(512) NOT NULL,
    hmac                 varchar(64)  NULL,
    size_bytes           bigint       NULL,
    format               varchar(16)  NOT NULL,
    stain                varchar(64)  NULL,
    level_label          varchar(16)  NULL,
    scanner              varchar(128) NULL,
    magnification        decimal(5,1) NULL,
    width_px             integer      NULL,
    height_px            integer      NULL,
    mpp_x                decimal(10,6) NULL,
    mpp_y                decimal(10,6) NULL,
    ingested_at          timestamptz  NOT NULL DEFAULT now(),
    verified_at          timestamptz  NULL,
    scan_metadata        jsonb        NULL,

    CONSTRAINT uq_wsi_edu_slides_slide_id
        UNIQUE (slide_id),

    CONSTRAINT fk_wsi_edu_slides_block
        FOREIGN KEY (block_id) REFERENCES wsi_edu.blocks(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_slides_block_id
    ON wsi_edu.slides (block_id);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_slides_relative_path
    ON wsi_edu.slides (relative_path);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_slides_stain
    ON wsi_edu.slides (stain);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_slides_verified_at
    ON wsi_edu.slides (verified_at);

-- ---------------------------------------------------------------------------
-- Case curators: educator/curator assignments (like case_pathologists)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.case_curators (
    id              uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id         uuid            NOT NULL,
    identity_id     uuid            NOT NULL,
    role            varchar(32)     NOT NULL,
    assigned_at     timestamptz     NOT NULL DEFAULT now(),
    assigned_by     uuid            NULL,

    CONSTRAINT fk_wsi_edu_curators_case
        FOREIGN KEY (case_id)      REFERENCES wsi_edu.cases(id)             ON DELETE CASCADE,
    CONSTRAINT fk_wsi_edu_curators_identity
        FOREIGN KEY (identity_id)  REFERENCES iam.identity(identity_id)     ON DELETE RESTRICT,
    CONSTRAINT fk_wsi_edu_curators_by
        FOREIGN KEY (assigned_by)  REFERENCES iam.identity(identity_id),
    CONSTRAINT ck_wsi_edu_curator_role
        CHECK (role IN ('PRIMARY_CURATOR', 'CURATOR', 'CONTRIBUTOR')),
    CONSTRAINT uq_wsi_edu_case_curator_identity
        UNIQUE (case_id, identity_id)
);

-- At most one PRIMARY_CURATOR per case
CREATE UNIQUE INDEX uq_wsi_edu_case_primary_curator
    ON wsi_edu.case_curators (case_id) WHERE role = 'PRIMARY_CURATOR';

CREATE INDEX ix_wsi_edu_curators_identity
    ON wsi_edu.case_curators (identity_id);

CREATE INDEX ix_wsi_edu_curators_role
    ON wsi_edu.case_curators (role);

-- ---------------------------------------------------------------------------
-- Annotations: teaching annotations (schema only — API deferred per SDS §1.1)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.annotations (
    id              uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    slide_id        uuid            NOT NULL,
    author_id       uuid            NOT NULL,
    annotation_type varchar(32)     NOT NULL DEFAULT 'region',
    geometry        jsonb           NOT NULL,
    label           text            NULL,
    description     text            NULL,
    visibility      varchar(16)     NOT NULL DEFAULT 'PERSONAL',
    created_at      timestamptz     NOT NULL DEFAULT now(),
    updated_at      timestamptz     NOT NULL DEFAULT now(),
    metadata        jsonb           NULL     DEFAULT '{}'::jsonb,

    CONSTRAINT fk_wsi_edu_annotations_slide
        FOREIGN KEY (slide_id)   REFERENCES wsi_edu.slides(id)           ON DELETE CASCADE,
    CONSTRAINT fk_wsi_edu_annotations_author
        FOREIGN KEY (author_id)  REFERENCES iam.identity(identity_id)    ON DELETE RESTRICT,
    CONSTRAINT ck_wsi_edu_annotation_visibility
        CHECK (visibility IN ('PERSONAL', 'SHARED', 'TEACHING', 'PUBLIC'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_annotations_slide_id
    ON wsi_edu.annotations (slide_id);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_annotations_author_id
    ON wsi_edu.annotations (author_id);

-- ---------------------------------------------------------------------------
-- Named collections: teaching sets for curriculum organization
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.named_collections (
    id              uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            varchar(255)    NOT NULL,
    description     text            NULL,
    owner_id        uuid            NOT NULL,
    visibility      varchar(16)     NOT NULL DEFAULT 'PRIVATE',
    created_at      timestamptz     NOT NULL DEFAULT now(),
    updated_at      timestamptz     NOT NULL DEFAULT now(),
    metadata        jsonb           NULL     DEFAULT '{}'::jsonb,

    CONSTRAINT fk_wsi_edu_collections_owner
        FOREIGN KEY (owner_id) REFERENCES iam.identity(identity_id) ON DELETE RESTRICT,
    CONSTRAINT ck_wsi_edu_collection_visibility
        CHECK (visibility IN ('PRIVATE', 'DEPARTMENT', 'INSTITUTION'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_collections_owner
    ON wsi_edu.named_collections (owner_id);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_collections_visibility
    ON wsi_edu.named_collections (visibility);

-- ---------------------------------------------------------------------------
-- Collection cases: M2M linking collections to cases with ordering
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi_edu.collection_cases (
    collection_id   uuid    NOT NULL,
    case_id         uuid    NOT NULL,
    sequence        integer NOT NULL DEFAULT 0,

    PRIMARY KEY (collection_id, case_id),

    CONSTRAINT fk_wsi_edu_cc_collection
        FOREIGN KEY (collection_id) REFERENCES wsi_edu.named_collections(id) ON DELETE CASCADE,
    CONSTRAINT fk_wsi_edu_cc_case
        FOREIGN KEY (case_id)       REFERENCES wsi_edu.cases(id)             ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wsi_edu_cc_case_id
    ON wsi_edu.collection_cases (case_id);

-- ---------------------------------------------------------------------------
-- Permissions for educational module
-- ---------------------------------------------------------------------------
INSERT INTO iam.permission (permission_id, name, description)
VALUES
    (gen_random_uuid(), 'VIEW_EDU_COLLECTION', 'View educational case collections'),
    (gen_random_uuid(), 'CURATE_EDU_COLLECTION', 'Curate educational case collections'),
    (gen_random_uuid(), 'MANAGE_EDU_COLLECTIONS', 'Manage named teaching collections'),
    (gen_random_uuid(), 'TRANSFER_TO_EDU', 'Transfer clinical cases to educational collection')
ON CONFLICT DO NOTHING;

-- Grant VIEW_EDU_COLLECTION to all authenticated roles
INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
CROSS JOIN iam.permission p
WHERE p.name = 'VIEW_EDU_COLLECTION'
  AND r.name IN ('ADMIN', 'PATHOLOGIST', 'TECHNICIAN', 'RESIDENT', 'FELLOW', 'HISTO_TECH', 'CYTO_TECH', 'RESEARCHER', 'RESEARCH_ADMIN')
ON CONFLICT DO NOTHING;

-- Grant CURATE_EDU_COLLECTION to ADMIN, PATHOLOGIST
INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
CROSS JOIN iam.permission p
WHERE p.name = 'CURATE_EDU_COLLECTION'
  AND r.name IN ('ADMIN', 'PATHOLOGIST')
ON CONFLICT DO NOTHING;

-- Grant MANAGE_EDU_COLLECTIONS to ADMIN, PATHOLOGIST
INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
CROSS JOIN iam.permission p
WHERE p.name = 'MANAGE_EDU_COLLECTIONS'
  AND r.name IN ('ADMIN', 'PATHOLOGIST')
ON CONFLICT DO NOTHING;

-- Grant TRANSFER_TO_EDU to ADMIN, PATHOLOGIST
INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
CROSS JOIN iam.permission p
WHERE p.name = 'TRANSFER_TO_EDU'
  AND r.name IN ('ADMIN', 'PATHOLOGIST')
ON CONFLICT DO NOTHING;
