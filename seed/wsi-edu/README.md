# WSI-EDU Seed Data

Test data for the `wsi_edu` schema (SDS-EDU-001). Seeds 10 educational cases with 10 slides from TCGA public dataset into the case → part → block → slide hierarchy, with curator assignments and ICD codes.

## Prerequisites

1. Postgres running: `docker compose -f Okapi/auth-system/docker-compose.yml up -d postgres`
2. `wsi_edu` schema migration V12 applied (via `./gradlew bootRun` in `Okapi/auth-system/`)
3. IAM identities loaded (for curator assignments)

## Usage

```bash
# From workspace root
source Okapi/auth-system/.env
PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f Okapi/seed/wsi-edu/seed-wsi-edu-cases.sql
```

The script is idempotent (`ON CONFLICT DO NOTHING`) — safe to re-run.

## Verification

```sql
-- Connect with: source Okapi/auth-system/.env && PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB"

-- 10 cases
SELECT case_id, collection, status, metadata->>'teaching_category' AS category
  FROM wsi_edu.cases ORDER BY case_id;

-- 10 slides
SELECT slide_id, relative_path, format FROM wsi_edu.slides ORDER BY slide_id;

-- Full hierarchy
SELECT c.case_id, p.part_label, p.provenance, b.block_label, s.slide_id
  FROM wsi_edu.cases c
  JOIN wsi_edu.parts p ON p.case_id = c.id
  JOIN wsi_edu.blocks b ON b.part_id = p.id
  JOIN wsi_edu.slides s ON s.block_id = b.id
  ORDER BY c.case_id;

-- Curator assignments (replaces case_pathologists)
SELECT c.case_id, i.username, cu.role
  FROM wsi_edu.case_curators cu
  JOIN wsi_edu.cases c ON c.id = cu.case_id
  JOIN iam.identity i ON i.identity_id = cu.identity_id
  ORDER BY c.case_id, cu.role;
```

## Files

| File | Description |
|------|-------------|
| `manifest.json` | Slide manifest with accession numbers, file paths, barcode IDs |
| `wsi-edu-cases.v1.json` | Source JSON with full case definitions, source lineage, ICD codes |
| `edu-curator-assignments.v1.json` | Curator assignments (PRIMARY_CURATOR / CURATOR / CONTRIBUTOR) |
| `seed-wsi-edu-cases.sql` | INSERT statements for `wsi_edu.*` tables including curator assignments |

## Key Differences from Clinical (`seed/wsi/`)

| Aspect | Clinical (`wsi`) | Educational (`wsi_edu`) |
|--------|-------------------|--------------------------|
| Schema | `wsi` | `wsi_edu` |
| Patient link | `patient_id` FK → `core.patients` | Column does not exist |
| Collection | `'clinical'` | `'educational'` |
| Status values | `'pending_review'`, etc. | `'active'`, `'archived'`, `'draft'` |
| Accession date | Set from LIS | Always `NULL` |
| Source lineage | N/A | JSONB tracking TCGA origin |
| Part/block provenance | N/A | `'IMPLIED'` for TCGA imports |
| Identity link | `case_pathologists` (PRIMARY/SECONDARY/CONSULTING/GROSSING) | `case_curators` (PRIMARY_CURATOR/CURATOR/CONTRIBUTOR) |

## Filesystem Layout

The corresponding slide files live in `large_image/test-cases/edu/2026/` following the year-partitioned layout defined in SDS-STR-001. The `relative_path` column in `wsi_edu.slides` stores paths relative to the educational collection root (e.g., `2026/EDU26-00001/EDU26-00001_01-01-01.svs`).
