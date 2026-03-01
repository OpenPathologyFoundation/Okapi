-- Core schema: patient master index
-- Implements the normalized patient model discussed in SDS-STR-001 §4.9.
-- The patient is the clinical entity that ties cases together across the platform.
-- De-identification severs the link between case and patient (wsi.cases.patient_id = NULL).

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS core;

-- ---------------------------------------------------------------------------
-- Patients: master patient index
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS core.patients (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Primary clinical identifier
    mrn                  varchar(64)  NOT NULL,

    -- Structured name (matches HL7/FHIR patient name)
    given_name           varchar(128) NOT NULL,
    family_name          varchar(128) NOT NULL,
    display_name         varchar(255) NOT NULL,

    -- Core demographics
    dob                  date         NULL,
    sex                  varchar(8)   NULL,

    -- Contact
    phone                varchar(32)  NULL,
    email                varchar(255) NULL,

    -- Address (varies by system; JSONB avoids over-normalization)
    address              jsonb        NULL,

    -- Flags
    is_active            boolean      NOT NULL DEFAULT true,
    is_test_patient      boolean      NOT NULL DEFAULT false,

    -- Extensible metadata: race, ethnicity, language, deceased_date,
    -- additional identifiers, edge_case flags, etc.
    metadata             jsonb        NULL DEFAULT '{}'::jsonb,

    -- Audit
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_core_patients_mrn UNIQUE (mrn),

    CONSTRAINT ck_core_patients_sex
        CHECK (sex IS NULL OR sex IN ('M', 'F', 'X', 'U'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS ix_core_patients_name
    ON core.patients (family_name, given_name);

CREATE INDEX IF NOT EXISTS ix_core_patients_dob
    ON core.patients (dob);

CREATE INDEX IF NOT EXISTS ix_core_patients_metadata
    ON core.patients USING gin (metadata);

CREATE INDEX IF NOT EXISTS ix_core_patients_display_name_fts
    ON core.patients USING gin (to_tsvector('simple', coalesce(display_name, '')));

-- ---------------------------------------------------------------------------
-- Link cases to patient master index (was V8, combined here)
-- NULL patient_id = educational / de-identified case (severed link).
-- ON DELETE SET NULL: removing a patient orphans their cases.
-- ---------------------------------------------------------------------------
ALTER TABLE wsi.cases
    ADD COLUMN IF NOT EXISTS patient_id uuid NULL;

DO $$ BEGIN
    ALTER TABLE wsi.cases
        ADD CONSTRAINT fk_wsi_cases_patient
            FOREIGN KEY (patient_id) REFERENCES core.patients(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

CREATE INDEX IF NOT EXISTS ix_wsi_cases_patient_id
    ON wsi.cases (patient_id);
