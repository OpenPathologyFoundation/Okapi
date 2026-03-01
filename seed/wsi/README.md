# WSI Test Case Seed Data

Test data for the `wsi` schema (Flyway migration V6). Seeds 6 clinical cases with 13 slides into the case→part→block→slide hierarchy.

## Prerequisites

1. Postgres running: `docker compose -f Okapi/auth-system/docker-compose.yml up -d postgres`
2. V6 migration applied (via `./gradlew bootRun` in `Okapi/auth-system/`, or manually)

## Usage

```bash
# From workspace root
psql -h localhost -U okapi_service -d okapi_auth -p 5433 -f Okapi/seed/wsi/seed-wsi-test-cases.sql
```

The script is idempotent (`ON CONFLICT DO NOTHING`) — safe to re-run.

## Verification

```sql
-- 6 cases
-- Connect with: psql -h localhost -U okapi_service -d okapi_auth -p 5433

SELECT case_id, collection, status FROM wsi.cases ORDER BY accession_date;

-- 13 slides
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
| `wsi-test-cases.v1.json` | Source JSON (copy of `large_image/test-cases/cases.json`) |
| `seed-wsi-test-cases.sql` | INSERT statements for `wsi.*` tables |

## Filesystem Layout

The corresponding slide files live in `large_image/test-cases/clinical/2026/` following the year-partitioned layout defined in SDS-STR-001. The `relative_path` column in `wsi.slides` stores paths relative to the collection root (e.g., `2026/S26-0001/S26-0001_A1_S1.svs`).
