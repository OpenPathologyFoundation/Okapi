# 02-SRS
---
title: System Requirements Specification - Okapi Core
document_id: SRS-001
version: 1.1
status: DRAFT
owner: Lead System Engineer
created_date: 2026-01-09
effective_date: TBD
trace_source: URS-001
trace_destination: TP-001 (Test Plan)
---

# 1. Functional Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-AUTH-001** | The system shall implement the OpenID Connect (OIDC) protocol to federate identity. | UN-001, UN-002 | Test |
| **SYS-AUTH-002** | The system shall support integration with external IdPs supporting SAML 2.0 or OIDC. | UN-002 | Analysis |
| **SYS-AUTH-003** | The system shall require MFA for accounts accessing PHI; MFA enforcement is delegated to the external IdP policy. | UN-003 | Analysis |
| **SYS-AUTH-004** | The system shall enforce authorization decisions using internal RBAC roles (e.g., `PATHOLOGIST`, `TECHNICIAN`, `ADMIN`). | UN-005 | Test |
| **SYS-AUTH-009** | The system shall derive internal roles from IdP group attributes using issuer-scoped mappings. | UN-006 | Test |
| **SYS-AUTH-005** | The system shall persist a normalized identity record on successful authentication, including `provider_id` and external subject identifier. | UN-001, UN-006 | Test |
| **SYS-AUTH-006** | The system shall enforce identity uniqueness per issuer via `UNIQUE(provider_id, external_subject)`. | UN-006 | Inspection |
| **SYS-AUTH-007** | The system shall fail closed when authentication context is missing or invalid (respond `401/403`). | UN-003, UN-005 | Test |
| **SYS-AUTH-008** | Session/cookie behavior (duration, remember-device) shall be configurable per deployment policy; defaults shall be documented and verified. | UN-004 | Analysis/Test |
| **SYS-AUTH-010** | The system shall provide an authenticated endpoint to return the current normalized identity (“who am I”), including issuer/provider ID, external subject identifier, display name/email (if available), and derived roles/authorities. | UN-001, UN-005, UN-006 | Test |
| **SYS-AUTH-011** | The system shall support administrative access management by deriving the internal `ADMIN` role from centralized IdP group membership and Okapi-local mappings; Okapi shall not create primary credentials for users. | UN-008 | Analysis/Test |
| **SYS-AUTH-012** | The system shall support timely provisioning updates by relying on centralized identity management (IdP) plus Okapi-local permission mappings, reducing manual errors and pressure to bypass security controls. | UN-009 | Analysis |
| **SYS-REL-001** | The system shall provide a local cache for pending results to allow clinicians to work during intermittent connectivity. | UN-003 | Test |
| **SYS-SEC-001** | AI Model weights shall be stored in a read-only volume in the production environment. | N/A (Risk Control) | Inspection |
| **SYS-AUD-001** | The system shall record authentication and authorization-relevant events with sufficient context for auditing (who/what/when/outcome). | UN-007 | Test |
| **SYS-AUD-002** | The system shall provide a database schema to store audit events with correlation and structured metadata. | UN-007 | Inspection |
| **SYS-DATA-003** | The database schema for AuthN/AuthZ shall be managed via Flyway migrations at application startup. | UN-006 | Inspection |
| **SYS-SEC-010** | Secrets (DB credentials, OIDC client secret) shall not be committed to source control and shall be supplied via environment variables/secret store. | UN-003 | Inspection |

# 2. Interface Requirements

| ID | Interface Requirement | Trace | Verification |
| :--- | :--- | :--- | :--- |
| **IR-001** | The system shall validate all Epic write-back transactions using checksums and HL7 ACK/NACK protocols. | N/A | Test |
| **API-01** | The system shall support OIDC authorization-code redirect handling via Spring Security OAuth2 login endpoints. | UN-002 | Test |
| **UI-01** | The login screen shall display the "Log in with Institution" button prominently. | UN-002 | Inspection |