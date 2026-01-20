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

## 1.1 HAT (Histology Asset Tracking) requirements

HAT requirements are written to enforce the separation between:
- **Asset facts** (identifiers, current status/location/custody, provenance, history)
- **Intent/work** (requests, execution events, and completion evidence)

| ID | System Requirement                                                                                                                                                                                                                            | Trace to User Need | Verification Method |
| :--- |:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| :--- | :--- |
| **SYS-HAT-001** | The system shall support scan-first lookup of histology assets by barcode/identifier and apply deterministic identifier normalization (prefix variants, whitespace/hyphens, case, leading zeros).                                             | UN-HAT-001 | Test |
| **SYS-HAT-002** | The system shall return deterministic lookup outcomes: `MATCHED`, `NOT_FOUND`, or `AMBIGUOUS`, and provide sufficient data to guide the next step (e.g., candidate list for ambiguous matches).                                               | UN-HAT-001 | Test |
| **SYS-HAT-003** | The system shall support fallback asset search by accession/part/date/stain/etc. subject to deployment privacy policy constraints (e.g., accession-based workflows when PHI search is restricted).                                            | UN-HAT-001, UN-HAT-007 | Analysis/Test |
| **SYS-HAT-004** | The system shall display the current asset state as a single authoritative view including: current status, current location, custody (holder + timestamps + due-back when applicable), lifecycle flags, and provenance of the asserted state. | UN-HAT-002 | Test |
| **SYS-HAT-005** | The system shall maintain an append-only event history for asset status/location/custody changes including actor, timestamp, optional comment, and linked request context when applicable.                                                    | UN-HAT-003 | Test |
| **SYS-HAT-006** | The system shall support non-destructive corrections by appending correcting events without deleting or editing prior events.                                                                                                                 | UN-HAT-003 | Inspection/Test |
| **SYS-HAT-007** | The system shall support creating asset-work requests encoding intent: target assets (or asset selectors), requested action(s), priority, due date, requester identity, and assignee (team/person).                                           | UN-HAT-004 | Test |
| **SYS-HAT-008** | The system shall support request lifecycle tracking: `OPEN` → `IN_PROGRESS` → `DONE`/`CANCELLED`, including partial fulfillment at the per-asset/per-action level.                                                                            | UN-HAT-004 | Test |
| **SYS-HAT-009** | The system shall provide a histology execution/work-queue view filtered by role/permissions and ensure execution steps are scan-confirmed (asset identity confirmed at the time of action).                                                   | UN-HAT-005 | Test |
| **SYS-HAT-010** | The system shall record intermediate execution milestones as events (e.g., pulled, stained, in QA, packaged, shipped) and store completion evidence fields relevant to the action (e.g., tracking number, destination, receipt/return).       | UN-HAT-005 | Test |
| **SYS-HAT-011** | The system shall support creation of placeholder assets for unknown identifiers and later reconciliation to authoritative sources, while preserving provenance and history.                                                                   | UN-HAT-006 | Test |
| **SYS-HAT-012** | The system shall support explicit reconciliation workflows when source systems disagree (orchestration kernel vs LIS vs physical), including recording the conflict, resolution decision, and actor attribution.                              | UN-HAT-003, UN-HAT-006 | Test |
| **SYS-HAT-013** | The system shall enforce role-based access controls for HAT and apply additional governance controls for high-risk actions (e.g., external distribution/research release), including approvals when required by deployment policy.            | UN-HAT-007 | Analysis/Test |
| **SYS-HAT-014** | The system shall provide audit defensibility for HAT by ensuring traceability from request → execution events → current state and by preventing unauthorized modification of history.                                                         | UN-HAT-003, UN-HAT-007 | Test/Inspection |

# 2. Interface Requirements

## 1.2 Work List Module (WL) Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-WL-001** | The system shall maintain a "Read Model" that aggregates data from LIS (HL7), Imaging (API), and Internal Authoring services, ensuring <30s latency from source events. | UN-WL-001, UN-WL-007 | Test |
| **SYS-WL-002** | The system shall enforce data precedence when conflicts occur: LIS > Authoring > WSI > Derived Rules. | UN-WL-001 | Analysis/Test |
| **SYS-WL-003** | The system shall enforce "Capability-Gated" rendering: strictly filtering returned cases at the service layer based on user permissions (e.g., `VIEW_SERVICE_WORKLISTS`). | UN-WL-003, UN-WL-006 | Test |
| **SYS-WL-004** | The system shall support rendering of privacy modes: `CLINICAL_VIEW` (Full PHI) and `TEACHING_VIEW` (Masked PHI), toggleable by authorized users. | UN-WL-003 | Test |
| **SYS-WL-005** | The system shall provide visual indicators for "Enrichment Signals" (WSI status, AI status, Annotations), loading them asynchronously to prevent blocking core list display. | UN-WL-002 | Test |
| **SYS-WL-006** | The system shall display composite status states for multi-author workflows (e.g., `Draft in progress`, `Pending review`) derived from internal authoring events. | UN-WL-005 | Test |
| **SYS-WL-007** | The system shall support a "Break-Glass" workflow demanding: explicit action, reason selection, and generation of a specific audit event (`break_glass_invoked`). | UN-WL-006 | Test |
| **SYS-WL-008** | The system shall support "Smart Filters" including: "Sign-out ready" (all prerequisites met) and "Needs attention" (old/urgent/alerted). | UN-WL-004 | Test |
| **SYS-WL-009** | The work list shall emit workflow events (`case_open_requested`, `viewer_launch_requested`) but shall NOT emit or retain fine-grained telemetry (mouse coords, dwell time). | UN-WL-003 | Inspection |

# 2. Interface Requirements

| ID | Interface Requirement                                                                                          | Trace | Verification |
| :--- |:---------------------------------------------------------------------------------------------------------------| :--- | :--- |
| **IR-001** | The system shall validate all EHR write-back transactions using checksums and HL7 ACK/NACK protocols.          | N/A | Test |
| **API-01** | The system shall support OIDC authorization-code redirect handling via Spring Security OAuth2 login endpoints. | UN-002 | Test |
| **UI-01** | The login screen shall display the "Log in with Institution" button prominently.                               | UN-002 | Inspection |