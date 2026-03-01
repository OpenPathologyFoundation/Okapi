-- Corrective migration: rename role → designation, drop RESIDENT/FELLOW values
-- V8 used 'role' with 5 values (PRIMARY, SECONDARY, CONSULTING, RESIDENT, FELLOW).
-- SDS §4.10 specifies 'designation' with 4 values; organizational position comes from iam.identity_roles.

-- Safety: migrate any RESIDENT/FELLOW rows to SECONDARY before rename
UPDATE wsi.case_pathologists SET role = 'SECONDARY' WHERE role IN ('RESIDENT', 'FELLOW');

-- Rename column: role → designation
ALTER TABLE wsi.case_pathologists RENAME COLUMN role TO designation;

-- Drop old constraints/indexes that reference 'role'
ALTER TABLE wsi.case_pathologists DROP CONSTRAINT IF EXISTS ck_role;
DROP INDEX IF EXISTS uq_case_primary;
DROP INDEX IF EXISTS ix_role;

-- New CHECK with 4 values (PRIMARY, SECONDARY, CONSULTING, GROSSING)
ALTER TABLE wsi.case_pathologists
    ADD CONSTRAINT ck_designation CHECK (designation IN ('PRIMARY','SECONDARY','CONSULTING','GROSSING'));

-- Recreate partial unique index (one PRIMARY per case) — SYS-CA-002
CREATE UNIQUE INDEX uq_case_one_primary ON wsi.case_pathologists (case_id) WHERE designation = 'PRIMARY';

-- Recreate lookup index
CREATE INDEX ix_designation ON wsi.case_pathologists (designation);
