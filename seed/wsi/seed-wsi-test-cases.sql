-- WSI Test Case Seed Data
-- Source: large_image/test-cases/cases.json (v1.0, 2026-01-19)
--
-- This is NOT a Flyway migration. Run manually for dev/test:
--   psql -h localhost -U okapi_service -d okapi_auth -p 5433 -f Okapi/seed/wsi/seed-wsi-test-cases.sql
--
-- Requires:
--   V6__wsi_storage_schema.sql   (wsi schema)
--   V7__core_patient_schema.sql  (core.patients table + patient_id FK on wsi.cases)
--   seed-patients.sql            (Xenonym patients loaded into core.patients)
--
-- Patient linkage uses Xenonym synthetic patients (seed: azure-vale-9728).
-- Each case is assigned a Xenonym patient by MRN lookup.
-- Uses ON CONFLICT for idempotent re-runs.

BEGIN;

-- =========================================================================
-- Case 1: S26-0001 — Breast lumpectomy (2 slides)
--   Patient: XN-000024 → Thisovau Oquuski (F, 1967-08-24, age 58)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0001', 'clinical',
        'Breast, left, lumpectomy',
        '58 y/o female with palpable left breast mass, 2.1 cm on imaging',
        '2026-01-15',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000024'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, final_diagnosis, metadata)
    SELECT id, 'A', 'Tumor with margins',
           'Invasive ductal carcinoma, grade 2',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section of tumor'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
), s1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0001_A1_S1', '2026/S26-0001/S26-0001_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM b
    ON CONFLICT DO NOTHING
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0001_A1_S2', '2026/S26-0001/S26-0001_A1_S2.svs', 'svs', 'H&E', 'S2'
FROM b
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 2: S26-0002 — Prostate resection (2 parts, 1 slide each)
--   Patient: XN-000030 → Ngupla Inotos (M, 1958-01-20, age 68)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0002', 'clinical',
        'Prostate, radical prostatectomy',
        '68 y/o male with rising PSA, Gleason 7 on biopsy',
        '2026-01-15',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000030'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
),
-- Part A: Right lobe
pa AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Right lobe', 'Prostate',
           'Prostatic adenocarcinoma, Gleason 3+4=7',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), ba AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Apex section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), sa AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0002_A1_S1', '2026/S26-0002/S26-0002_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM ba
    ON CONFLICT DO NOTHING
),
-- Part B: Left lobe
pb AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT c.id, 'B', 'Left lobe', 'Prostate',
           'Prostatic adenocarcinoma, Gleason 3+4=7',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), bb AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Mid-gland section'
    FROM pb
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0002_B1_S1', '2026/S26-0002/S26-0002_B1_S1.svs', 'svs', 'H&E', 'S1'
FROM bb
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 3: S26-0003 — Cervix LEEP (1 slide)
--   Patient: XN-000037 → Tingioth Sinkizvitrath (F, 1974-03-07, age 51)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0003', 'clinical',
        'Cervix, LEEP excision',
        '51 y/o female with abnormal cervical cytology, HSIL',
        '2026-01-16',
        'pending_review', 'stat',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000037'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'LEEP specimen', 'Cervix',
           'High-grade squamous intraepithelial lesion (CIN 3)',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', '12 o''clock section'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0003_A1_S1', '2026/S26-0003/S26-0003_A1_S1.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 4: S26-0004 — Colon hemicolectomy (1 part, 3 blocks, 3 slides)
--   Patient: XN-000018 → Glilmezair Gusa (M, 1980-01-22, age 46)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0004', 'clinical',
        'Colon, right hemicolectomy',
        '46 y/o male with colon mass, biopsy positive for adenocarcinoma',
        '2026-01-16',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000018'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), pa AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Tumor', 'Colon',
           'Adenocarcinoma, moderately differentiated, pT3N1',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
),
-- Block 1: Tumor with deepest invasion
b1 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Tumor with deepest invasion'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0004_A1_S1', '2026/S26-0004/S26-0004_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM b1
    ON CONFLICT DO NOTHING
),
-- Block 2: Pericolic lymph nodes
b2 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT pa.id, '2', 'Pericolic lymph nodes'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s2 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0004_A2_S1', '2026/S26-0004/S26-0004_A2_S1.svs', 'svs', 'H&E', 'S1'
    FROM b2
    ON CONFLICT DO NOTHING
),
-- Block 3: Pericolic lymph nodes (OME-TIFF)
b3 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT pa.id, '3', 'Pericolic lymph nodes'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0004_A3_S1', '2026/S26-0004/S26-0004_A3_S1.ome.tiff', 'ome.tiff', 'H&E', 'S1'
FROM b3
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 5: S26-0005 — Breast mastectomy with sentinel nodes
--   Part A: Tumor (block 1, slides S1 + S2)
--   Part B: Sentinel lymph node (block 1, slides S1 + S2)
--   Patient: XN-000001 → Ourfir Bruntilavoul (F, 1955-02-15, age 71)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0005', 'clinical',
        'Breast, right, mastectomy with sentinel nodes',
        '71 y/o female with right breast mass, 3.5 cm, BI-RADS 5',
        '2026-01-17',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000001'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
),
-- Part A: Tumor
pa AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Tumor', 'Breast',
           'Invasive lobular carcinoma, grade 2, ER+/PR+/HER2-',
           '{"er_status": "positive", "pr_status": "positive", "her2_status": "negative"}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), ba AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative tumor section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), sa1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0005_A1_S1', '2026/S26-0005/S26-0005_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM ba
    ON CONFLICT DO NOTHING
), sa2 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0005_A1_S2', '2026/S26-0005/S26-0005_A1_S2.svs', 'svs', 'H&E', 'S2'
    FROM ba
    ON CONFLICT DO NOTHING
),
-- Part B: Sentinel lymph node
pb AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT c.id, 'B', 'Sentinel lymph node', 'Lymph node',
           'Invasive lobular carcinoma, grade 2, ER+/PR+/HER2-',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), bb AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Sentinel node #1'
    FROM pb
    ON CONFLICT DO NOTHING
    RETURNING id
), sb1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0005_B1_S1', '2026/S26-0005/S26-0005_B1_S1.svs', 'svs', 'H&E', 'S1'
    FROM bb
    ON CONFLICT DO NOTHING
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0005_B1_S2', '2026/S26-0005/S26-0005_B1_S2.tiff', 'tiff', 'H&E', 'S2'
FROM bb
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 6: S26-0006 — Breast infiltrating duct carcinoma (1 part, 1 block, 3 slides)
--   Patient: XN-000003 (F)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0006', 'clinical',
        'Breast, excisional biopsy',
        '62 y/o female with breast mass, infiltrating duct carcinoma on biopsy',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000003'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Tumor', 'Breast',
           'Infiltrating duct carcinoma, NOS',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
), s1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0006_A1_S1', '2026/S26-0006/S26-0006_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM b
    ON CONFLICT DO NOTHING
), s2 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0006_A1_S2', '2026/S26-0006/S26-0006_A1_S2.svs', 'svs', 'H&E', 'S2'
    FROM b
    ON CONFLICT DO NOTHING
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0006_A1_S3', '2026/S26-0006/S26-0006_A1_S3.svs', 'svs', 'H&E', 'S3'
FROM b
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 7: S26-0007 — Breast mucinous adenocarcinoma, metastatic (1 slide)
--   Patient: XN-000006 (F)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0007', 'clinical',
        'Breast, metastatic site biopsy',
        'Female with metastatic mucinous adenocarcinoma of the breast',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000006'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Metastatic tumor', 'Breast',
           'Mucinous adenocarcinoma',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0007_A1_S1', '2026/S26-0007/S26-0007_A1_S1.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 8: S26-0008 — Cervical squamous cell carcinoma (2 parts, 3 slides)
--   Part A: block 1 (2 slides), Part A: block 2 (1 slide)
--   Patient: XN-000011 (F)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0008', 'clinical',
        'Cervix, excisional biopsy',
        '54 y/o female with cervical squamous cell carcinoma',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000011'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), pa AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Cervical biopsy', 'Cervix',
           'Squamous cell carcinoma, NOS',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
),
-- Block 1: 2 slides
b1 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0008_A1_S1', '2026/S26-0008/S26-0008_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM b1
    ON CONFLICT DO NOTHING
), s2 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0008_A1_S2', '2026/S26-0008/S26-0008_A1_S2.svs', 'svs', 'H&E', 'S2'
    FROM b1
    ON CONFLICT DO NOTHING
),
-- Block 2: 1 slide
b2 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT pa.id, '2', 'Additional section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0008_A2_S1', '2026/S26-0008/S26-0008_A2_S1.svs', 'svs', 'H&E', 'S1'
FROM b2
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 9: S26-0009 — Prostate adenocarcinoma (1 part, 3 blocks, 3 slides)
--   Patient: XN-000013 (M)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0009', 'clinical',
        'Prostate, radical prostatectomy',
        '60 y/o male with prostatic adenocarcinoma',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000013'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), pa AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Prostate, section 1', 'Prostate',
           'Adenocarcinoma, NOS',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
),
-- Block 1
b1 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0009_A1_S1', '2026/S26-0009/S26-0009_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM b1
    ON CONFLICT DO NOTHING
),
-- Block 2
b2 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT pa.id, '2', 'Representative section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s2 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0009_A2_S1', '2026/S26-0009/S26-0009_A2_S1.svs', 'svs', 'H&E', 'S1'
    FROM b2
    ON CONFLICT DO NOTHING
),
-- Block 3
b3 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT pa.id, '3', 'Representative section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0009_A3_S1', '2026/S26-0009/S26-0009_A3_S1.svs', 'svs', 'H&E', 'S1'
FROM b3
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 10: S26-0010 — Colon adenocarcinoma (1 part, 2 blocks, 3 slides)
--   Patient: XN-000021 (F)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0010', 'clinical',
        'Colon, resection',
        'Female with colon adenocarcinoma',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000021'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), pa AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Colon tumor', 'Colon',
           'Adenocarcinoma, NOS',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
),
-- Block 1: 1 slide
b1 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s1 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0010_A1_S1', '2026/S26-0010/S26-0010_A1_S1.svs', 'svs', 'H&E', 'S1'
    FROM b1
    ON CONFLICT DO NOTHING
),
-- Block 2: 2 slides
b2 AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT pa.id, '2', 'Additional section'
    FROM pa
    ON CONFLICT DO NOTHING
    RETURNING id
), s2 AS (
    INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
    SELECT id, 'S26-0010_A2_S1', '2026/S26-0010/S26-0010_A2_S1.svs', 'svs', 'H&E', 'S1'
    FROM b2
    ON CONFLICT DO NOTHING
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0010_A2_S2', '2026/S26-0010/S26-0010_A2_S2.svs', 'svs', 'H&E', 'S2'
FROM b2
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 11: S26-0011 — Breast lobular carcinoma (1 slide)
--   Patient: XN-000031 (F)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0011', 'clinical',
        'Breast, excisional biopsy',
        '65 y/o female with breast lobular carcinoma',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000031'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Tumor', 'Breast',
           'Lobular carcinoma, NOS',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0011_A1_S1', '2026/S26-0011/S26-0011_A1_S1.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

-- =========================================================================
-- Case 12: S26-0012 — Colon sigmoid adenocarcinoma (1 slide)
--   Patient: XN-000023 (M)
-- =========================================================================
WITH c AS (
    INSERT INTO wsi.cases (case_id, collection, specimen_type, clinical_history, accession_date, status, priority, patient_id, metadata)
    VALUES (
        'S26-0012', 'clinical',
        'Colon, sigmoid resection',
        '44 y/o male with sigmoid colon adenocarcinoma',
        '2026-01-20',
        'pending_review', 'routine',
        (SELECT id FROM core.patients WHERE mrn = 'XN-000023'),
        '{}'::jsonb
    )
    ON CONFLICT (collection, case_id) DO UPDATE SET
        clinical_history = EXCLUDED.clinical_history,
        patient_id = EXCLUDED.patient_id,
        metadata = EXCLUDED.metadata
    RETURNING id
), p AS (
    INSERT INTO wsi.parts (case_id, part_label, part_designator, anatomic_site, final_diagnosis, metadata)
    SELECT id, 'A', 'Sigmoid colon tumor', 'Colon',
           'Adenocarcinoma, NOS',
           '{}'::jsonb
    FROM c
    ON CONFLICT DO NOTHING
    RETURNING id
), b AS (
    INSERT INTO wsi.blocks (part_id, block_label, block_description)
    SELECT id, '1', 'Representative section'
    FROM p
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO wsi.slides (block_id, slide_id, relative_path, format, stain, level_label)
SELECT id, 'S26-0012_A1_S1', '2026/S26-0012/S26-0012_A1_S1.svs', 'svs', 'H&E', 'S1'
FROM b
ON CONFLICT DO NOTHING;

COMMIT;

-- =========================================================================
-- Verification queries
-- =========================================================================
-- Case overview with patient linkage:
-- SELECT c.case_id, c.collection, c.status, p.mrn, p.display_name
--   FROM wsi.cases c
--   LEFT JOIN core.patients p ON p.id = c.patient_id
--   ORDER BY c.accession_date;
--
-- Full hierarchy:
-- SELECT c.case_id, pt.mrn, pt.display_name, p.part_label, b.block_label, s.slide_id
--   FROM wsi.cases c
--   LEFT JOIN core.patients pt ON pt.id = c.patient_id
--   JOIN wsi.parts p ON p.case_id = c.id
--   JOIN wsi.blocks b ON b.part_id = p.id
--   JOIN wsi.slides s ON s.block_id = b.id
--   ORDER BY c.case_id, p.part_label, b.block_label, s.slide_id;
--
-- Verify de-identification semantics (should be empty for seeded clinical data):
-- SELECT case_id FROM wsi.cases WHERE patient_id IS NULL;
