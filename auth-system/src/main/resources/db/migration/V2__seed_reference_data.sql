-- Seed baseline reference data (roles and IdP group → role mappings)

INSERT INTO roles (name, description, is_system)
VALUES
    ('ADMIN', 'System Administrator with full access', true),
    ('PATHOLOGIST', 'Medical Pathologist - View Cases, Sign-out', true),
    ('TECHNICIAN', 'Lab Technician - Ingestion, Processing', true),
    ('RESIDENT', 'Pathology Resident - View Cases, Draft Reports', true),
    ('FELLOW', 'Pathology Fellow - View Cases, Edit Reports', true),
    ('HISTO_TECH', 'Histology Technician', true),
    ('CYTO_TECH', 'Cytology Technician', true)
ON CONFLICT (name) DO NOTHING;

-- Note: groups are issuer-scoped (provider_id), so seed for the default local Keycloak realm.
WITH inserted_groups AS (
    INSERT INTO idp_group_mappings (provider_id, idp_group_name)
    VALUES
        ('http://localhost:8180/realms/okapi', 'Okapi_Admins'),
        ('http://localhost:8180/realms/okapi', 'Okapi_Pathologists'),
        ('http://localhost:8180/realms/okapi', 'Okapi_Technicians'),
        ('http://localhost:8180/realms/okapi', 'Okapi_Residents')
    ON CONFLICT (provider_id, idp_group_name) DO UPDATE
        SET provider_id = EXCLUDED.provider_id
    RETURNING id, idp_group_name
)
INSERT INTO idp_group_role_mappings (idp_group_mapping_id, role_id)
SELECT
    g.id,
    r.id
FROM inserted_groups g
JOIN roles r ON (
    (g.idp_group_name = 'Okapi_Admins' AND r.name = 'ADMIN') OR
    (g.idp_group_name = 'Okapi_Pathologists' AND r.name = 'PATHOLOGIST') OR
    (g.idp_group_name = 'Okapi_Technicians' AND r.name = 'TECHNICIAN') OR
    (g.idp_group_name = 'Okapi_Residents' AND r.name = 'RESIDENT')
)
ON CONFLICT DO NOTHING;
