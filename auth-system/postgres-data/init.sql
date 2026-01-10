-- Create Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Create Identities Table
CREATE TABLE IF NOT EXISTS identities (
    id SERIAL PRIMARY KEY,
    external_subject VARCHAR(255) UNIQUE NOT NULL,
    provider_id VARCHAR(255),
    display_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Create Join Table for Identity Roles
CREATE TABLE IF NOT EXISTS identity_roles (
    identity_id INTEGER REFERENCES identities(id) ON DELETE CASCADE,
    role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (identity_id, role_id)
);

-- Create IdP Group Mappings Table
CREATE TABLE IF NOT EXISTS idp_group_mappings (
    id SERIAL PRIMARY KEY,
    idp_group_name VARCHAR(100) UNIQUE NOT NULL,
    role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE
);

-- Audit Events Table (Stub for future)
CREATE TABLE IF NOT EXISTS audit_events (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(50),
    identity_id INTEGER,
    outcome VARCHAR(50),
    details TEXT
);

-- ==========================================
-- Initial Data Seeding
-- ==========================================

-- Seed Roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'System Administrator with full access'),
('PATHOLOGIST', 'Medical Pathologist - View Cases, Sign-out'),
('TECHNICIAN', 'Lab Technician - Ingestion, Processing'),
('RESIDENT', 'Pathology Resident - View Cases, Draft Reports'),
('FELLOW', 'Pathology Fellow - View Cases, Edit Reports'),
('HISTO_TECH', 'Histology Technician'),
('CYTO_TECH', 'Cytology Technician')
ON CONFLICT (name) DO NOTHING;

-- Seed IdP Group Mappings
INSERT INTO idp_group_mappings (idp_group_name, role_id) VALUES
('Okapi_Admins', (SELECT id FROM roles WHERE name = 'ADMIN')),
('Okapi_Pathologists', (SELECT id FROM roles WHERE name = 'PATHOLOGIST')),
('Okapi_Technicians', (SELECT id FROM roles WHERE name = 'TECHNICIAN')),
('Okapi_Residents', (SELECT id FROM roles WHERE name = 'RESIDENT'))
ON CONFLICT (idp_group_name) DO NOTHING;

-- Seed Sample Identity (Optional, matches Keycloak test user)
INSERT INTO identities (external_subject, provider_id, display_name, email) VALUES
('test-subject-id', 'http://localhost:8180/realms/okapi', 'Test User', 'testuser@okapi.com')
ON CONFLICT (email) DO NOTHING;

-- Assign Role to Sample Identity
INSERT INTO identity_roles (identity_id, role_id) 
SELECT i.id, r.id 
FROM identities i, roles r 
WHERE i.email = 'testuser@okapi.com' AND r.name = 'PATHOLOGIST'
ON CONFLICT DO NOTHING;
