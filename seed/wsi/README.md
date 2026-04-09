# WSI Test Case Seed Data

Test data for the `wsi` schema (Flyway migration V6+). Seeds **12 clinical cases with 27 slides** into the case-part-block-slide hierarchy.

## Prerequisites

1. Postgres running: `docker compose -f starling/auth-system/docker-compose.yml up -d postgres`
2. V6 migration applied (via `./gradlew bootRun` in `starling/auth-system/`, or manually)
3. Patients seeded (cases reference patients by MRN)

## Usage

Two paths — choose one:

### Via Admin API (recommended)

See `seed/README.md` for browser-console or curl instructions:

```
POST /admin/seed/cases            → seeds cases + slides from wsi-test-cases.v1.json
POST /admin/seed/case-assignments → seeds pathologist assignments
POST /admin/seed/worklist-sync    → populates worklist from wsi.cases
```

### Via psql (direct SQL)

```bash
source Starling/auth-system/.env
PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f Starling/seed/wsi/seed-wsi-test-cases.sql
```

The SQL script is idempotent (`ON CONFLICT DO NOTHING`) — safe to re-run.

## Verification

```sql
-- 12 cases
SELECT case_id, collection, status FROM wsi.cases ORDER BY accession_date;

-- 27 slides
SELECT slide_id, relative_path, format FROM wsi.slides ORDER BY slide_id;

-- Full hierarchy
SELECT c.case_id, p.part_label, b.block_label, s.slide_id
  FROM wsi.cases c
  JOIN wsi.parts p ON p.case_id = c.id
  JOIN wsi.blocks b ON b.part_id = p.id
  JOIN wsi.slides s ON s.block_id = b.id
  ORDER BY c.case_id, p.part_label, b.block_label, s.slide_id;
```

## Files

| File | Description |
|------|-------------|
| `wsi-test-cases.v1.json` | Source JSON: 12 cases with slides, patient MRNs, diagnoses |
| `wsi-test-cases.v1.schema.json` | JSON Schema (draft 2020-12) for the above |
| `case-assignments.v1.json` | Pathologist-to-case assignment mappings |
| `case-assignments.v1.schema.json` | JSON Schema for assignments |
| `manifest.json` | Slide file manifest (32 entries with TCGA provenance) |
| `seed-wsi-test-cases.sql` | Direct SQL INSERT statements for `wsi.*` tables |

## Filesystem Layout

The corresponding slide files live in `large_image/test-cases/clinical/2026/` following the year-partitioned layout defined in SDS-STR-001. The `relative_path` column in `wsi.slides` stores paths relative to the collection root (e.g., `2026/S26-0001/S26-0001_A1_S1.svs`).
