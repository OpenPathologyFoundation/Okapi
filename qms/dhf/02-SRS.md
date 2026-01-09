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
| **SYS-AUTH-001** | The system shall implement the OpenID Connect (OIDC) protocol to federate identity. | UN-001 | Analysis |
| **SYS-AUTH-002** | The system shall support integration with external IdPs supporting SAML 2.0 or OIDC. | UN-002 | Test |
| **SYS-AUTH-003** | The system shall enforce Multi-Factor Authentication (MFA) for all accounts accessing PHI. | UN-003 | Test |
| **SYS-AUTH-004** | The system shall provide a "Remember this device" option issuing a token valid for 12 hours. | UN-004 | Test |
| **SYS-AUTH-005** | The system shall revoke "Remembered Device" tokens upon explicit logout or password change. | UN-003, UN-004 | Test |
| **SYS-REL-001** | The system shall provide a local cache for pending results to allow clinicians to work during intermittent connectivity. | UN-003 | Test |
| **SYS-SEC-001** | AI Model weights shall be stored in a read-only volume in the production environment. | N/A (Risk Control) | Inspection |
| **SYS-AUD-001** | The system shall stream all clinical action logs to an immutable cloud storage service. | N/A (Security) | Test |

# 2. Interface Requirements

| ID | Interface Requirement | Trace | Verification |
| :--- | :--- | :--- | :--- |
| **IR-001** | The system shall validate all Epic write-back transactions using checksums and HL7 ACK/NACK protocols. | N/A | Test |
| **API-01** | The system shall expose a `/auth/callback` endpoint to handle OIDC redirects. | UN-002 | Test |
| **UI-01** | The login screen shall display the "Log in with Institution" button prominently. | UN-002 | Inspection |