-- Add missing idp_group → role mappings for Fellows, HistoTechs, and CytoTechs.
-- V2 only seeded Okapi_Admins, Okapi_Pathologists, Okapi_Technicians, Okapi_Residents.
-- Without these, login-time group→role resolution fails for fellows, histotechs, and cytotechs.

WITH inserted_groups AS (
    INSERT INTO iam.idp_group (provider_id, group_name)
    VALUES
        ('http://localhost:8180/realms/okapi', 'Okapi_Fellows'),
        ('http://localhost:8180/realms/okapi', 'Okapi_HistoTechs'),
        ('http://localhost:8180/realms/okapi', 'Okapi_CytoTechs')
    ON CONFLICT (provider_id, group_name) DO UPDATE
        SET provider_id = EXCLUDED.provider_id
    RETURNING idp_group_id, group_name
)
INSERT INTO iam.idp_group_role (idp_group_id, role_id)
SELECT g.idp_group_id, r.role_id
FROM inserted_groups g
JOIN iam.role r ON (
    (g.group_name = 'Okapi_Fellows'    AND r.name = 'FELLOW') OR
    (g.group_name = 'Okapi_HistoTechs' AND r.name = 'HISTO_TECH') OR
    (g.group_name = 'Okapi_CytoTechs'  AND r.name = 'CYTO_TECH')
)
ON CONFLICT DO NOTHING;
