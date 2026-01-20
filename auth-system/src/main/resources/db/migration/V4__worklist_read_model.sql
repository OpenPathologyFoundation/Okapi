-- Pathology Worklist Read Model
-- Read-optimized table for worklist display and filtering
-- This is a denormalized projection of case data for fast queries

-- -----------------------------
-- Pathology Worklist
-- -----------------------------
CREATE TABLE IF NOT EXISTS pathology_worklist (
    id                   BIGSERIAL PRIMARY KEY,

    -- Core identification
    accession_number     VARCHAR(50)  NOT NULL UNIQUE,
    patient_mrn          VARCHAR(50),
    patient_display      VARCHAR(100),

    -- Service and specimen
    service              VARCHAR(30)  NOT NULL
        CHECK (service IN ('SURGICAL', 'CYTOLOGY', 'HEMATOLOGY', 'AUTOPSY')),
    specimen_type        VARCHAR(100),
    specimen_site        VARCHAR(100),

    -- Status tuple (composite workflow state)
    status               VARCHAR(30)  NOT NULL DEFAULT 'ACCESSIONED'
        CHECK (status IN (
            'ACCESSIONED',
            'GROSSING',
            'PROCESSING',
            'SLIDES_CUT',
            'PENDING_SIGNOUT',
            'UNDER_REVIEW',
            'SIGNED_OUT',
            'AMENDED'
        )),
    lis_status           VARCHAR(30),
    wsi_status           VARCHAR(30),
    authoring_status     VARCHAR(30),

    -- Priority
    priority             VARCHAR(20)  NOT NULL DEFAULT 'ROUTINE'
        CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE')),

    -- Assignment (denormalized for read performance)
    assigned_to_id       BIGINT REFERENCES identities ON DELETE SET NULL,
    assigned_to_display  VARCHAR(100),

    -- Slide counts
    slide_count          INT NOT NULL DEFAULT 0,
    slide_pending        INT NOT NULL DEFAULT 0,
    slide_scanned        INT NOT NULL DEFAULT 0,

    -- Timestamps
    case_date            DATE NOT NULL DEFAULT CURRENT_DATE,
    received_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    collected_at         TIMESTAMPTZ,

    -- Enrichment (flexible JSONB fields)
    annotations          JSONB NOT NULL DEFAULT '[]'::jsonb,
    alerts               JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata             JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Audit
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_worklist_accession ON pathology_worklist (accession_number);
CREATE INDEX IF NOT EXISTS idx_worklist_assigned_to ON pathology_worklist (assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_worklist_service ON pathology_worklist (service);
CREATE INDEX IF NOT EXISTS idx_worklist_status ON pathology_worklist (status);
CREATE INDEX IF NOT EXISTS idx_worklist_priority ON pathology_worklist (priority);
CREATE INDEX IF NOT EXISTS idx_worklist_case_date ON pathology_worklist (case_date DESC);

-- Composite index for "my cases" query (assigned + not signed out)
CREATE INDEX IF NOT EXISTS idx_worklist_my_cases
    ON pathology_worklist (assigned_to_id, status)
    WHERE status NOT IN ('SIGNED_OUT', 'AMENDED');

-- Composite index for service queue
CREATE INDEX IF NOT EXISTS idx_worklist_service_queue
    ON pathology_worklist (service, priority, case_date);
