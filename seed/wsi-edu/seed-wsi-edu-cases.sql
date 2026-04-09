-- WSI-EDU Seed Data — Educational teaching cases from TCGA public dataset
-- Source: Starling/seed/wsi-edu/wsi-edu-cases.v1.json (v1.0, 2026-03-02)
--
-- This is NOT a Flyway migration. Run manually for dev/test:
--   psql -h localhost -U starling_service -d starling_auth -p 5433 -f Starling/seed/wsi-edu/seed-wsi-edu-cases.sql
--
-- Requires:
--   wsi_edu schema created (Flyway migration TBD)
--   IAM identities loaded (for curator assignments)
--
-- Key differences from clinical wsi seed (see SDS-EDU-001):
--   - Schema: wsi_edu (not wsi)
--   - No patient_id column — de-identification by design
--   - source_lineage JSONB — tracks TCGA origin
--   - parts/blocks have provenance column (IMPLIED for TCGA imports)
--   - case_curators replaces case_pathologists (PRIMARY_CURATOR/CURATOR/CONTRIBUTOR)
--   - collection = 'educational', status = 'active', accession_date = NULL
--
-- Uses ON CONFLICT for idempotent re-runs.

BEGIN;

-- =========================================================================
-- Case 1: EDU26-00001 — Brain, Glioblastoma (TCGA-GBM)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00001', 'educational',
        'Brain, NOS',
        'Adult patient with high-grade glial neoplasm. Surgical resection performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-GBM", "tcga_case_id": "TCGA-06-6390", "gdc_file_ids": ["2820fb9e-7903-4c26-a304-b9756cf14630"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "neuropathology", "difficulty_level": "intermediate", "curriculum_tags": ["glioblastoma", "brain tumor", "high-grade glioma", "CNS neoplasm"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Brain',
           'Glioblastoma',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00001_01-01-01', '2026/EDU26-00001/EDU26-00001_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

-- ICD codes
INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C71.9', 'ICD-10', 'Malignant neoplasm of brain, unspecified'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00001'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '9440/3', 'ICD-O-3', 'Glioblastoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00001'
ON CONFLICT DO NOTHING;

-- Curators: hlemsesor (PRIMARY_CURATOR), gmuklus (CONTRIBUTOR)
INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00001' AND i.username = 'hlemsesor'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00001' AND i.username = 'gmuklus'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 2: EDU26-00002 — Lung, Papillary adenocarcinoma (TCGA-LUAD)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00002', 'educational',
        'Lung, lower lobe, resection',
        'Adult patient with lung mass. Lobectomy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-LUAD", "tcga_case_id": "TCGA-86-8074", "gdc_file_ids": ["6a0ea716-a5f2-47f3-880b-537a5cdc2324"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "pulmonary_pathology", "difficulty_level": "intermediate", "curriculum_tags": ["lung adenocarcinoma", "papillary adenocarcinoma", "pulmonary neoplasm"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Lung',
           'Papillary adenocarcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00002_01-01-01', '2026/EDU26-00002/EDU26-00002_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C34.3', 'ICD-10', 'Malignant neoplasm of lower lobe, bronchus or lung'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00002'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8260/3', 'ICD-O-3', 'Papillary adenocarcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00002'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00002' AND i.username = 'bmodeswuv'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00002' AND i.username = 'vbezho'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 3: EDU26-00003 — Colon, Adenocarcinoma (TCGA-COAD)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00003', 'educational',
        'Colon, resection',
        'Adult patient with colonic mass. Resection performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-COAD", "tcga_case_id": "TCGA-AZ-4313", "gdc_file_ids": ["d65c5d21-6333-4a9e-9a2a-139a122a3c8a"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "gi_pathology", "difficulty_level": "beginner", "curriculum_tags": ["colon adenocarcinoma", "colorectal carcinoma", "GI neoplasm"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Colon',
           'Adenocarcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00003_01-01-01', '2026/EDU26-00003/EDU26-00003_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8140/3', 'ICD-O-3', 'Adenocarcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00003'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00003' AND i.username = 'ltsindi'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00003' AND i.username = 'gwukdaskornisauv'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 4: EDU26-00004 — Kidney, Clear cell RCC (TCGA-KIRC)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00004', 'educational',
        'Kidney, nephrectomy',
        'Adult patient with renal mass. Nephrectomy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-KIRC", "tcga_case_id": "TCGA-BP-4977", "gdc_file_ids": ["f2524bdf-18a5-45c7-b2be-fb0a535165ea"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "genitourinary_pathology", "difficulty_level": "intermediate", "curriculum_tags": ["renal cell carcinoma", "clear cell RCC", "kidney tumor"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Kidney',
           'Clear cell adenocarcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00004_01-01-01', '2026/EDU26-00004/EDU26-00004_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C64.9', 'ICD-10', 'Malignant neoplasm of kidney, except renal pelvis'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00004'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8310/3', 'ICD-O-3', 'Clear cell adenocarcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00004'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00004' AND i.username = 'gsneakhan'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00004' AND i.username = 'wnkunhalkevi'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 5: EDU26-00005 — Liver, Hepatocellular carcinoma (TCGA-LIHC)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00005', 'educational',
        'Liver, resection',
        'Adult patient with hepatic mass. Partial hepatectomy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-LIHC", "tcga_case_id": "TCGA-CC-A7IJ", "gdc_file_ids": ["553f70e1-d83d-4bc8-9cee-dac77068a458"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "hepatopathology", "difficulty_level": "intermediate", "curriculum_tags": ["hepatocellular carcinoma", "HCC", "liver tumor"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Liver',
           'Hepatocellular carcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00005_01-01-01', '2026/EDU26-00005/EDU26-00005_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C22.0', 'ICD-10', 'Liver cell carcinoma'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00005'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8170/3', 'ICD-O-3', 'Hepatocellular carcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00005'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00005' AND i.username = 'ppiolileibro'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00005' AND i.username = 'gmuklus'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 6: EDU26-00006 — Thyroid, Papillary adenocarcinoma (TCGA-THCA)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00006', 'educational',
        'Thyroid gland, thyroidectomy',
        'Adult patient with thyroid nodule. Thyroidectomy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-THCA", "tcga_case_id": "TCGA-DJ-A2Q6", "gdc_file_ids": ["47bd3f9d-bac3-40ca-aa0f-a8a7db127f20"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "endocrine_pathology", "difficulty_level": "beginner", "curriculum_tags": ["thyroid carcinoma", "papillary thyroid carcinoma", "endocrine neoplasm"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Thyroid gland',
           'Papillary adenocarcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00006_01-01-01', '2026/EDU26-00006/EDU26-00006_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C73', 'ICD-10', 'Malignant neoplasm of thyroid gland'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00006'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8260/3', 'ICD-O-3', 'Papillary adenocarcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00006'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00006' AND i.username = 'hlemsesor'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00006' AND i.username = 'vbezho'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 7: EDU26-00007 — Breast, Infiltrating duct carcinoma (TCGA-BRCA)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00007', 'educational',
        'Breast, excision',
        'Adult patient with breast mass. Excision performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-BRCA", "tcga_case_id": "TCGA-E2-A14P", "gdc_file_ids": ["4730b23e-aea1-49a2-ba63-2231fd88b592"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "breast_pathology", "difficulty_level": "intermediate", "curriculum_tags": ["breast carcinoma", "invasive ductal carcinoma", "IDC"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Breast',
           'Infiltrating duct carcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00007_01-01-01', '2026/EDU26-00007/EDU26-00007_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C50.9', 'ICD-10', 'Malignant neoplasm of breast, unspecified'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00007'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8500/3', 'ICD-O-3', 'Infiltrating duct carcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00007'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00007' AND i.username = 'gsneakhan'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00007' AND i.username = 'wnkunhalkevi'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00007' AND i.username = 'gwukdaskornisauv'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 8: EDU26-00008 — Skin, Basal cell carcinoma (TCGA-SKCM)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00008', 'educational',
        'Skin/soft tissue, excision',
        'Adult patient with cutaneous neoplasm. Excisional biopsy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-SKCM", "tcga_case_id": "TCGA-EE-A2MR", "gdc_file_ids": ["1c9e5b03-bec4-40e5-80c1-e74d669d6b2d"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "dermatopathology", "difficulty_level": "advanced", "curriculum_tags": ["melanoma", "skin carcinoma", "cutaneous neoplasm"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Skin',
           'Basal cell carcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00008_01-01-01', '2026/EDU26-00008/EDU26-00008_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8090/3', 'ICD-O-3', 'Basal cell carcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00008'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00008' AND i.username = 'ppiolileibro'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00008' AND i.username = 'gmuklus'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 9: EDU26-00009 — Prostate, Acinar cell carcinoma (TCGA-PRAD)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00009', 'educational',
        'Prostate, prostatectomy',
        'Adult patient with prostate neoplasm. Radical prostatectomy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-PRAD", "tcga_case_id": "TCGA-EJ-7123", "gdc_file_ids": ["6da0d6d5-bf47-43c7-a668-588ba97222b8"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "genitourinary_pathology", "difficulty_level": "intermediate", "curriculum_tags": ["prostate carcinoma", "acinar cell carcinoma", "prostatic adenocarcinoma"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Prostate gland',
           'Acinar cell carcinoma',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00009_01-01-01', '2026/EDU26-00009/EDU26-00009_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C61', 'ICD-10', 'Malignant neoplasm of prostate'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00009'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8550/3', 'ICD-O-3', 'Acinar cell carcinoma'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00009'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00009' AND i.username = 'bmodeswuv'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00009' AND i.username = 'ltsindi'
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 10: EDU26-00010 — Pancreas, Infiltrating duct carcinoma (TCGA-PAAD)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi_edu.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, source_lineage, metadata)
    VALUES (
        'EDU26-00010', 'educational',
        'Pancreas, Whipple resection',
        'Adult patient with pancreatic head mass. Pancreaticoduodenectomy performed.',
        NULL,
        'active', NULL,
        '{"type": "public_dataset", "dataset": "TCGA", "project": "TCGA-PAAD", "tcga_case_id": "TCGA-FB-AAPS", "gdc_file_ids": ["c673de35-2153-4fd0-916a-8a15e5f34054"], "terms_of_use": "GDC open access"}'::jsonb,
        '{"teaching_category": "gi_pathology", "difficulty_level": "advanced", "curriculum_tags": ["pancreatic adenocarcinoma", "ductal carcinoma", "pancreatic neoplasm"]}'::jsonb
    )
    ON CONFLICT (case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        source_lineage = EXCLUDED.source_lineage,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi_edu.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, provenance, metadata)
    SELECT id, '01', 'Tissue section', 'Pancreas',
           'Infiltrating duct carcinoma, NOS',
           'IMPLIED', '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi_edu.blocks (part_id, block_label, block_description, provenance)
    SELECT id, '01', 'Representative tumor section', 'IMPLIED'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi_edu.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'EDU26-00010_01-01-01', '2026/EDU26-00010/EDU26-00010_01-01-01.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, 'C25.0', 'ICD-10', 'Malignant neoplasm of head of pancreas'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00010'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
SELECT id, '8500/3', 'ICD-O-3', 'Infiltrating duct carcinoma, NOS'
FROM wsi_edu.cases WHERE case_id = 'EDU26-00010'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'PRIMARY_CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00010' AND i.username = 'ltsindi'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CURATOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00010' AND i.username = 'ppiolileibro'
ON CONFLICT DO NOTHING;

INSERT INTO wsi_edu.case_curators (case_id, identity_id, role)
SELECT c.id, i.identity_id, 'CONTRIBUTOR'
FROM wsi_edu.cases c, iam.identity i
WHERE c.case_id = 'EDU26-00010' AND i.username = 'vbezho'
ON CONFLICT DO NOTHING;

COMMIT;

-- =========================================================================
-- Verification queries
-- =========================================================================
-- Case overview:
-- SELECT case_id, collection, status, specimen_type,
--        source_lineage->>'dataset' AS source,
--        metadata->>'teaching_category' AS category,
--        metadata->>'difficulty_level' AS difficulty
--   FROM wsi_edu.cases
--   ORDER BY case_id;
--
-- Full hierarchy:
-- SELECT c.case_id, p.part_label, p.provenance, b.block_label, s.slide_id, s.relative_path
--   FROM wsi_edu.cases c
--   JOIN wsi_edu.parts p ON p.case_id = c.id
--   JOIN wsi_edu.blocks b ON b.part_id = p.id
--   JOIN wsi_edu.slides s ON s.block_id = b.id
--   ORDER BY c.case_id;
--
-- Curator assignments:
-- SELECT c.case_id, i.username, i.display_name, cu.role
--   FROM wsi_edu.case_curators cu
--   JOIN wsi_edu.cases c ON c.id = cu.case_id
--   JOIN iam.identity i ON i.identity_id = cu.identity_id
--   ORDER BY c.case_id, cu.role;
--
-- ICD codes:
-- SELECT c.case_id, ic.icd_code, ic.code_system, ic.code_description
--   FROM wsi_edu.case_icd_codes ic
--   JOIN wsi_edu.cases c ON c.id = ic.case_id
--   ORDER BY c.case_id, ic.code_system;
