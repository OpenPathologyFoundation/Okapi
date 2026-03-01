-- WSI (Whole Slide Image) storage schema
-- Implements the data model from SDS-STR-001 Section 4:
--   cases -> parts -> blocks -> slides
--   cases -> case_icd_codes

CREATE SCHEMA IF NOT EXISTS wsi;

-- ---------------------------------------------------------------------------
-- Cases: accession-level container for clinical or educational slides
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi.cases (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id              varchar(64)  NOT NULL,
    collection           varchar(16)  NOT NULL,
    specimen_type        varchar(255) NULL,
    clinical_history     text         NULL,
    accession_date       date         NULL,
    ingested_at          timestamptz  NOT NULL DEFAULT now(),
    status               varchar(32)  NOT NULL DEFAULT 'pending_review',
    priority             varchar(16)  NULL,
    metadata             jsonb        NULL     DEFAULT '{}'::jsonb,

    CONSTRAINT uq_wsi_cases_collection_case_id
        UNIQUE (collection, case_id),

    CONSTRAINT ck_wsi_cases_collection
        CHECK (collection IN ('clinical', 'educational'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_cases_metadata
    ON wsi.cases USING gin (metadata);

CREATE INDEX IF NOT EXISTS ix_wsi_cases_collection_status
    ON wsi.cases (collection, status);

CREATE INDEX IF NOT EXISTS ix_wsi_cases_accession_date
    ON wsi.cases (accession_date);

CREATE INDEX IF NOT EXISTS ix_wsi_cases_ingested_at
    ON wsi.cases (ingested_at);

-- ---------------------------------------------------------------------------
-- Parts: specimen parts within a case (A, B, C ...)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi.parts (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id              uuid         NOT NULL,
    part_label           varchar(16)  NOT NULL,
    part_designator      varchar(255) NULL,
    anatomic_site        varchar(128) NULL,
    final_diagnosis      text         NULL,
    gross_description    text         NULL,
    metadata             jsonb        NULL     DEFAULT '{}'::jsonb,

    CONSTRAINT fk_wsi_parts_case
        FOREIGN KEY (case_id) REFERENCES wsi.cases(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wsi_parts_case_id
    ON wsi.parts (case_id);

CREATE INDEX IF NOT EXISTS ix_wsi_parts_anatomic_site
    ON wsi.parts (anatomic_site);

CREATE INDEX IF NOT EXISTS ix_wsi_parts_diagnosis_fts
    ON wsi.parts USING gin (to_tsvector('english', coalesce(final_diagnosis, '')));

-- ---------------------------------------------------------------------------
-- ICD codes: case-level diagnostic coding
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi.case_icd_codes (
    case_id              uuid        NOT NULL,
    icd_code             varchar(16) NOT NULL,
    code_system          varchar(16) NOT NULL,
    code_description     varchar(255) NULL,

    PRIMARY KEY (case_id, icd_code, code_system),

    CONSTRAINT fk_wsi_case_icd_codes_case
        FOREIGN KEY (case_id) REFERENCES wsi.cases(id) ON DELETE CASCADE,

    CONSTRAINT ck_wsi_case_icd_codes_system
        CHECK (code_system IN ('ICD-10', 'ICD-O-3', 'SNOMED'))
);

CREATE INDEX IF NOT EXISTS ix_wsi_case_icd_codes_code
    ON wsi.case_icd_codes (icd_code);

-- ---------------------------------------------------------------------------
-- Blocks: tissue blocks within a part (1, 2, 3 ...)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi.blocks (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    part_id              uuid        NOT NULL,
    block_label          varchar(16) NOT NULL,
    block_description    text        NULL,

    CONSTRAINT fk_wsi_blocks_part
        FOREIGN KEY (part_id) REFERENCES wsi.parts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wsi_blocks_part_id
    ON wsi.blocks (part_id);

-- ---------------------------------------------------------------------------
-- Slides: physical WSI files on disk
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wsi.slides (
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

    CONSTRAINT uq_wsi_slides_slide_id
        UNIQUE (slide_id),

    CONSTRAINT fk_wsi_slides_block
        FOREIGN KEY (block_id) REFERENCES wsi.blocks(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wsi_slides_block_id
    ON wsi.slides (block_id);

CREATE INDEX IF NOT EXISTS ix_wsi_slides_relative_path
    ON wsi.slides (relative_path);

CREATE INDEX IF NOT EXISTS ix_wsi_slides_stain
    ON wsi.slides (stain);

CREATE INDEX IF NOT EXISTS ix_wsi_slides_verified_at
    ON wsi.slides (verified_at);
