-- Seed baseline reference data (roles, permissions, IdP group → role mappings)

INSERT INTO iam.role (name, description, is_system)
VALUES
    ('ADMIN', 'System Administrator with full access', true),
    ('PATHOLOGIST', 'Medical Pathologist - View Cases, Sign-out', true),
    ('TECHNICIAN', 'Lab Technician - Ingestion, Processing', true),
    ('RESIDENT', 'Pathology Resident - View Cases, Draft Reports', true),
    ('FELLOW', 'Pathology Fellow - View Cases, Edit Reports', true),
    ('HISTO_TECH', 'Histology Technician', true),
    ('CYTO_TECH', 'Cytology Technician', true),
    ('RESEARCHER', 'Research User - Limited read access', true),
    ('RESEARCH_ADMIN', 'Research Administrator - Manage research grants', true)
ON CONFLICT (name) DO NOTHING;

INSERT INTO iam.permission (name, description)
VALUES
    ('CASE_VIEW', 'View case details and worklist'),
    ('CASE_EDIT', 'Edit case data and reports'),
    ('CASE_SIGN_OUT', 'Sign out / finalize cases'),
    ('CASE_REASSIGN', 'Reassign cases to other users'),
    ('HISTO_VIEW', 'View histology data'),
    ('HISTO_EDIT', 'Edit histology data and status'),
    ('HISTO_BATCH', 'Create and manage stain batches'),
    ('HISTO_QA', 'Perform QA on stain batches'),
    ('RESEARCH_VIEW', 'View research suitability metadata'),
    ('RESEARCH_REQUEST', 'Request research access grants'),
    ('RESEARCH_APPROVE', 'Approve research access grants'),
    ('ADMIN_USERS', 'Manage user identities and roles'),
    ('ADMIN_SYSTEM', 'System configuration and settings'),
    ('ADMIN_AUDIT', 'View audit logs'),
    ('BREAK_GLASS_INVOKE', 'Invoke break-glass access'),
    ('PROFILE_VIEW_OWN', 'View own profile'),
    ('PROFILE_EDIT_OWN', 'Edit own profile preferences'),
    ('PROFILE_VIEW_ANY', 'View any user profile (admin)'),
    ('PROFILE_EDIT_ANY', 'Edit any user profile (admin)')
ON CONFLICT (name) DO NOTHING;

-- Role → Permission mappings (baseline)
INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN (
    'CASE_VIEW', 'CASE_EDIT', 'CASE_SIGN_OUT', 'CASE_REASSIGN',
    'HISTO_VIEW', 'HISTO_EDIT', 'HISTO_BATCH', 'HISTO_QA',
    'RESEARCH_VIEW', 'RESEARCH_REQUEST', 'RESEARCH_APPROVE',
    'ADMIN_USERS', 'ADMIN_SYSTEM', 'ADMIN_AUDIT',
    'BREAK_GLASS_INVOKE',
    'PROFILE_VIEW_OWN', 'PROFILE_EDIT_OWN', 'PROFILE_VIEW_ANY', 'PROFILE_EDIT_ANY'
)
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('CASE_VIEW', 'CASE_EDIT', 'CASE_SIGN_OUT', 'CASE_REASSIGN', 'HISTO_VIEW')
WHERE r.name = 'PATHOLOGIST'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('CASE_VIEW', 'HISTO_VIEW', 'HISTO_EDIT', 'HISTO_BATCH', 'HISTO_QA')
WHERE r.name = 'TECHNICIAN'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('CASE_VIEW', 'CASE_EDIT')
WHERE r.name IN ('RESIDENT', 'FELLOW')
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('HISTO_VIEW', 'HISTO_EDIT', 'HISTO_BATCH', 'HISTO_QA')
WHERE r.name = 'HISTO_TECH'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('CASE_VIEW', 'HISTO_VIEW')
WHERE r.name = 'CYTO_TECH'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('RESEARCH_VIEW', 'RESEARCH_REQUEST')
WHERE r.name = 'RESEARCHER'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM iam.role r
JOIN iam.permission p ON p.name IN ('RESEARCH_VIEW', 'RESEARCH_REQUEST', 'RESEARCH_APPROVE')
WHERE r.name = 'RESEARCH_ADMIN'
ON CONFLICT DO NOTHING;

-- Note: groups are issuer-scoped (provider_id), so seed for the default local Keycloak realm.
WITH inserted_groups AS (
    INSERT INTO iam.idp_group (provider_id, group_name)
    VALUES
        ('http://localhost:8180/realms/okapi', 'Okapi_Admins'),
        ('http://localhost:8180/realms/okapi', 'Okapi_Pathologists'),
        ('http://localhost:8180/realms/okapi', 'Okapi_Technicians'),
        ('http://localhost:8180/realms/okapi', 'Okapi_Residents')
    ON CONFLICT (provider_id, group_name) DO UPDATE
        SET provider_id = EXCLUDED.provider_id
    RETURNING idp_group_id, group_name
)
INSERT INTO iam.idp_group_role (idp_group_id, role_id)
SELECT
    g.idp_group_id,
    r.role_id
FROM inserted_groups g
JOIN iam.role r ON (
    (g.group_name = 'Okapi_Admins' AND r.name = 'ADMIN') OR
    (g.group_name = 'Okapi_Pathologists' AND r.name = 'PATHOLOGIST') OR
    (g.group_name = 'Okapi_Technicians' AND r.name = 'TECHNICIAN') OR
    (g.group_name = 'Okapi_Residents' AND r.name = 'RESIDENT')
)
ON CONFLICT DO NOTHING;
