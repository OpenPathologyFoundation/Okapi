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

## 1.1 Authentication (AuthN) Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-AUTHN-001** | The system shall implement the OpenID Connect (OIDC) protocol to federate identity. | UN-AUTHN-001, UN-AUTHN-002 | Test |
| **SYS-AUTHN-002** | The system shall support integration with external IdPs supporting SAML 2.0 or OIDC. | UN-AUTHN-002 | Analysis |
| **SYS-AUTHN-003** | The system shall require MFA for accounts accessing PHI; MFA enforcement is delegated to the external IdP policy. | UN-AUTHN-003 | Analysis |
| **SYS-AUTHN-004** | The system shall persist a normalized identity record on successful authentication, including `provider_id`, external subject identifier, and normalized name components. | UN-AUTHN-001, UN-PROF-001 | Test |
| **SYS-AUTHN-005** | The system shall enforce identity uniqueness per issuer via `UNIQUE(provider_id, external_subject)`. | UN-AUTHN-001 | Inspection |
| **SYS-AUTHN-006** | The system shall fail closed when authentication context is missing or invalid (respond `401/403`). | UN-AUTHN-003 | Test |
| **SYS-AUTHN-007** | Session/cookie behavior (duration, remember-device) shall be configurable per deployment policy; defaults shall be documented and verified. | UN-AUTHN-004 | Analysis/Test |
| **SYS-AUTHN-008** | The system shall provide an authenticated endpoint to return the current normalized identity ("who am I"), including issuer/provider ID, external subject identifier, display name/email (if available), and derived roles/authorities. | UN-AUTHN-007 | Test |
| **SYS-AUTHN-009** | The system shall support trusted device registration with hashed fingerprints and configurable trust duration. | UN-AUTHN-004 | Test |
| **SYS-AUTHN-010** | The system shall allow users to view and revoke their trusted devices via self-service endpoints. | UN-AUTHN-005 | Test |
| **SYS-AUTHN-011** | The system shall record authentication events (login, logout, session expiry, device trust changes) in the audit log. | UN-AUTHN-006 | Test |

## 1.2 Authorization (AuthZ) Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-AUTHZ-001** | The system shall enforce authorization decisions using internal RBAC roles (e.g., `PATHOLOGIST`, `TECHNICIAN`, `ADMIN`). | UN-AUTHZ-001 | Test |
| **SYS-AUTHZ-002** | The system shall derive internal roles from IdP group attributes using issuer-scoped mappings. | UN-AUTHZ-002 | Test |
| **SYS-AUTHZ-003** | The system shall support fine-grained permissions assigned to roles (e.g., `CASE_VIEW`, `CASE_SIGN_OUT`) to enable least-privilege access. | UN-AUTHZ-003 | Test |
| **SYS-AUTHZ-004** | The system shall support time-bounded role assignments with `effective_from` and `effective_to` dates for temporary access scenarios. | UN-AUTHZ-004 | Test |
| **SYS-AUTHZ-005** | The system shall support administrative access management by deriving the internal `ADMIN` role from centralized IdP group membership and Okapi-local mappings; Okapi shall not create primary credentials for users. | UN-AUTHZ-005 | Analysis/Test |
| **SYS-AUTHZ-006** | The system shall support timely provisioning updates by relying on centralized identity management (IdP) plus Okapi-local permission mappings, reducing manual errors and pressure to bypass security controls. | UN-AUTHZ-006 | Analysis |
| **SYS-AUTHZ-007** | The system shall record authorization-relevant events (role changes, permission grants/revocations, access denials) in the audit log. | UN-AUTHZ-007 | Test |
| **SYS-AUTHZ-008** | The system shall support break-glass access grants that are time-bounded, scoped to specific entities, require justification, and are fully audited. | UN-AUTHZ-008 | Test |
| **SYS-AUTHZ-009** | The system shall allow administrators to configure which users are eligible for break-glass access via permission flags. | UN-AUTHZ-009 | Analysis/Test |
| **SYS-AUTHZ-010** | The system shall enforce default-deny authorization: permissions and roles are granted only by explicit mappings; unmapped IdP groups grant no access. | UN-AUTHZ-003 | Test |
| **SYS-AUTHZ-011** | The system shall enforce authorization on the server for every protected API; client/UI checks are advisory only. | UN-AUTHZ-001 | Inspection/Test |
| **SYS-AUTHZ-012** | The system shall augment access tokens with Okapi-derived `roles` and `permissions` (names), include mandatory `okapi_authz_version` (format `YYYY.MM.DD+<short-hash>`), and refresh/recompute them on session renewal; access token TTL shall default to 10 minutes (configurable). | UN-AUTHZ-002, UN-AUTHZ-003 | Analysis/Test |

## 1.3 User Profile Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-PROF-001** | The system shall capture and persist professional identity attributes (name components, credentials, prefix/suffix) from IdP claims where available. | UN-PROF-001 | Test |
| **SYS-PROF-002** | The system shall store user preferences in an extensible JSONB structure that persists across sessions. | UN-PROF-002 | Test |
| **SYS-PROF-003** | The system shall record profile and preference changes in the audit log. | UN-PROF-003 | Test |
| **SYS-PROF-004** | The system shall support account type classification (PRODUCTION, TEST, DEMO, SERVICE, RESEARCH) for operational and compliance purposes. | UN-PROF-004 | Test |

## 1.4 Research Access Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-RES-001** | The system shall support time-bounded, protocol-specific research access grants scoped to specimens, blocks, or cohorts. | UN-RES-001 | Test |
| **SYS-RES-002** | The system shall enforce PHI access levels (NONE, MASKED, LIMITED, FULL) on research access grants to support minimum necessary exposure. | UN-RES-002 | Test |
| **SYS-RES-003** | The system shall support research administrator approval workflow for research access grants with documented justification. | UN-RES-003 | Analysis/Test |
| **SYS-RES-004** | The system shall record all research access grant lifecycle events (creation, approval, revocation) in the audit log. | UN-RES-004 | Test |

## 1.5 General Security Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-SEC-001** | AI Model weights shall be stored in a read-only volume in the production environment. | N/A (Risk Control) | Inspection |
| **SYS-SEC-010** | Secrets (DB credentials, OIDC client secret) shall not be committed to source control and shall be supplied via environment variables/secret store. | UN-AUTHN-003 | Inspection |
| **SYS-REL-001** | The system shall provide a local cache for pending results to allow clinicians to work during intermittent connectivity. | UN-AUTHN-003 | Test |

## 1.6 Audit Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-AUD-001** | The system shall record authentication and authorization-relevant events with sufficient context for auditing (who/what/when/outcome). | UN-AUTHN-006, UN-AUTHZ-007 | Test |
| **SYS-AUD-002** | The system shall provide a database schema to store audit events with correlation and structured metadata. | UN-AUTHN-006, UN-AUTHZ-007 | Inspection |

## 1.7 Data Management Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-DATA-001** | The database schema for IAM shall be managed via Flyway migrations at application startup. | UN-AUTHZ-002 | Inspection |

## 1.8 HAT (Histology Asset Tracking) Requirements

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

## 1.9 Work List Module (WL) Requirements

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
| **SYS-WL-010** | The system shall provide a global search input that queries `GET /api/worklist?search=<term>&pageSize=8` with 300ms debounce and 2-character minimum, displaying results in a dropdown with accession number, patient name, service, and status. Selecting a result navigates to the case. A "View all" action navigates to the worklist with the search pre-applied. | UN-WL-008 | Test |

## 1.10 IAM Administration Requirements

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-ADMIN-001** | The system shall provide admin API endpoints for CRUD operations on identities, role assignments, and IdP group mappings, gated by `ROLE_ADMIN` authorization. | UN-ADMIN-001, UN-ADMIN-002, UN-ADMIN-003, UN-ADMIN-004 | Integration Test |
| **SYS-ADMIN-002** | The system shall support identity soft-delete (deactivation via `is_active` flag), preserving all historical audit records and case attribution. | UN-ADMIN-002 | Integration Test |
| **SYS-ADMIN-003** | The system shall record all admin-initiated changes (role assignments, identity deactivation, mapping changes, grant revocations, device revocations) as audit events with actor attribution, timestamp, target entity, and metadata. | UN-ADMIN-011 | Integration Test |
| **SYS-ADMIN-004** | The system shall provide paginated, filterable query endpoints for audit events supporting filter by event type, actor, target, date range, and outcome. | UN-ADMIN-006 | Integration Test |
| **SYS-ADMIN-005** | The system shall provide admin endpoints to list and revoke break-glass grants, research access grants, and trusted devices for any user. | UN-ADMIN-007, UN-ADMIN-008 | Integration Test |
| **SYS-ADMIN-006** | The system shall expose a read-only role-permission matrix endpoint providing visibility into which permissions are assigned to each role. | UN-ADMIN-005 | Unit Test |
| **SYS-ADMIN-007** | The system shall render the admin UI within the main web application with role-conditional navigation: users with only `ROLE_ADMIN` see admin routes only; users with additional clinical roles see both clinical and admin sections. | UN-ADMIN-010 | E2E / Manual |
| **SYS-ADMIN-008** | The system shall provide a centralized auth context (current user identity, roles, permissions) to the web client on session load for role-based UI rendering. | UN-ADMIN-010, UN-AUTHN-007 | Unit Test |
| **SYS-ADMIN-009** | Admin UI authorization checks shall be advisory only; the server-side enforcement defined in `SYS-AUTHZ-011` remains the authoritative access control mechanism for all admin API endpoints. | UN-ADMIN-010 | Integration Test |

## 1.11 Orchestrator-Viewer Integration (OVI) Requirements

Requirements for the cross-window communication bridge between the Okapi web client (orchestrator) and the Digital Viewer. The orchestrator owns the lifecycle of the viewer window and is the authority for case context, authentication tokens, and case-switching decisions.

### 1.11.1 Viewer Window Lifecycle

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-OVI-001** | The orchestrator shall open the viewer via `window.open()` with a well-known URL and immediately transmit case context (case ID, patient identifier, JWT) via `postMessage` on the `VIEWER_OPEN` channel. | UN-OVI-001, UN-OVI-002 | Test |
| **SYS-OVI-002** | The orchestrator shall detect popup blocker interference within 3 seconds of the `window.open()` call and display a user-actionable message with instructions to allow popups for the application origin. | UN-OVI-001 | Test |
| **SYS-OVI-003** | The orchestrator shall track the viewer window reference and detect window closure via periodic liveness checks (polling `window.closed` at ≤1s intervals). | UN-OVI-007 | Test |
| **SYS-OVI-004** | When the viewer window is closed by the user (or by the OS), the orchestrator shall update its internal state to reflect no active viewer and display a "Viewer closed" indicator with the option to reopen. | UN-OVI-007 | Test |

### 1.11.2 postMessage Bridge Protocol

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-OVI-005** | All cross-window messages shall be typed (discriminated union on `type` field), validated against a shared schema, and carry a monotonically increasing sequence number for ordering and duplicate detection. | UN-OVI-002 | Inspection/Test |
| **SYS-OVI-006** | The orchestrator shall validate the `origin` of all incoming `postMessage` events against the expected viewer origin and silently discard messages from unexpected origins. | N/A (Security) | Test |
| **SYS-OVI-007** | The bridge shall implement a heartbeat protocol: the orchestrator sends `HEARTBEAT` messages at a configurable interval (default 15s), and the viewer responds with `HEARTBEAT_ACK`. Three consecutive missed ACKs shall trigger the bridge-degraded state. | UN-OVI-007 | Test |
| **SYS-OVI-008** | The bridge protocol shall define the following message types at minimum: `VIEWER_OPEN`, `VIEWER_READY`, `CASE_SWITCH`, `CASE_SWITCH_ACK`, `JWT_REFRESH`, `HEARTBEAT`, `HEARTBEAT_ACK`, `VIEWER_CLOSE`, `BRIDGE_RECONNECT`. | UN-OVI-002, UN-OVI-003, UN-OVI-004, UN-OVI-006 | Inspection |

### 1.11.3 Case Switching

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-OVI-009** | When the user selects a different case in the worklist while the viewer is open, the orchestrator shall display a confirmation dialog before sending `CASE_SWITCH` to the viewer. | UN-OVI-003 | Test |
| **SYS-OVI-010** | The `CASE_SWITCH` message shall carry the new case ID, patient identifier, and a fresh JWT scoped to the new case. The viewer shall acknowledge receipt with `CASE_SWITCH_ACK`. | UN-OVI-003, UN-OVI-004 | Test |
| **SYS-OVI-011** | If the viewer does not acknowledge `CASE_SWITCH` within 5 seconds, the orchestrator shall retry once and, on second failure, display a warning that the viewer may be out of sync. | UN-OVI-007 | Test |

### 1.11.4 JWT Provisioning and Refresh

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-OVI-012** | The orchestrator shall provision a case-scoped JWT to the viewer via `postMessage` at launch and on every case switch. The JWT shall have a lifetime sufficient for the viewer to complete a case examination without bridge-mediated refresh (minimum 30 minutes, configurable). | UN-OVI-004, UN-OVI-005 | Analysis/Test |
| **SYS-OVI-013** | The orchestrator shall proactively send `JWT_REFRESH` messages before the current token expires (at 75% of token lifetime). | UN-OVI-004 | Test |
| **SYS-OVI-014** | The viewer shall hold the JWT in memory only (not `localStorage`, not cookies). On bridge disconnection, the viewer shall continue operating with the last-received token until it expires. | UN-OVI-004, UN-OVI-005 | Inspection/Test |
| **SYS-OVI-015** | When the viewer's JWT expires and the bridge is disconnected, the viewer shall display a non-blocking banner indicating that the session will expire and provide the option to reconnect or re-authenticate. The viewer shall NOT abruptly close or navigate away. | UN-OVI-005 | Test |

### 1.11.5 Bridge Degradation and Recovery

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-OVI-016** | When the bridge enters degraded state (heartbeat timeout), both windows shall display a visual indicator (e.g., amber status bar) distinguishable from the normal state. The indicator shall be non-intrusive and shall not obscure diagnostic content. | UN-OVI-007 | Test |
| **SYS-OVI-017** | In bridge-degraded state, the viewer shall continue to serve tiles, accept annotations, and support measurement — all functions that do not require orchestrator coordination. | UN-OVI-005 | Test |
| **SYS-OVI-018** | When the bridge recovers (heartbeat resumes), the orchestrator shall send a `BRIDGE_RECONNECT` handshake containing the current case ID, patient identifier, and a fresh JWT. The viewer shall validate that the case context matches and resynchronize silently. | UN-OVI-006 | Test |
| **SYS-OVI-019** | If the `BRIDGE_RECONNECT` handshake reveals a case context mismatch (orchestrator switched cases during disconnection), the viewer shall display a confirmation prompt before accepting the new case. | UN-OVI-003, UN-OVI-006 | Test |
| **SYS-OVI-020** | The viewer shall be capable of operating in standalone mode (launched directly, without an orchestrator) for scenarios where the orchestrator is unavailable or not used (education, demo, testing). | UN-OVI-005 | Test |

### 1.11.6 Session Awareness Service Integration

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-OVI-021** | The Session Awareness Service shall be an optional Layer 2 dependency. Its unavailability shall not prevent the orchestrator from launching the viewer, switching cases, or provisioning JWTs. | UN-OVI-008 | Test |
| **SYS-OVI-022** | The WebSocket connection to the Session Awareness Service shall implement automatic reconnection with exponential backoff (initial 1s, max 30s, jitter ±20%). | UN-OVI-008 | Test |
| **SYS-OVI-023** | The orchestrator shall implement a WebSocket heartbeat (ping/pong at 30s intervals) independent of the `postMessage` bridge heartbeat, and shall tolerate hospital network proxies that terminate idle connections after 60-120 seconds. | UN-OVI-008 | Analysis/Test |
| **SYS-OVI-024** | When the Session Awareness Service is unavailable, the multi-case warning feature (SYS-SES-004 in Viewer SRS) shall be gracefully disabled. All other clinical functions shall continue unaffected. | UN-OVI-008 | Test |

## 1.12 Case Assignment (CA) Requirements

Case assignment tracks which people are responsible for which cases. The `wsi.case_pathologists` table is the authoritative source of truth; the worklist read model (`pathology_worklist.assigned_to_identity_id`) is a denormalized projection of the PRIMARY assignment from this table. The assigned person's organizational position (Pathologist, Resident, Fellow, etc.) is resolved from their `iam.identity` role assignments — it is not stored redundantly in the case assignment table.

### 1.12.1 Assignment Data Model

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-CA-001** | The system shall maintain a `wsi.case_pathologists` table relating cases to identities with a `designation` column indicating the person's function on the case: `PRIMARY`, `SECONDARY`, `CONSULTING`, or `GROSSING`. The person's organizational position (Pathologist, Resident, Fellow, etc.) is not stored in this table — it is resolved from `iam.identity_roles` at query time. | UN-CA-001, UN-CA-002 | Inspection/Test |
| **SYS-CA-002** | The system shall enforce at most one `PRIMARY` designation per case via a partial unique index (`UNIQUE (case_id) WHERE designation = 'PRIMARY'`). A case may have zero or more SECONDARY and CONSULTING assignments. | UN-CA-001 | Test |
| **SYS-CA-003** | The system shall prevent duplicate assignments of the same identity to the same case via a unique constraint on `(case_id, identity_id)`. | UN-CA-002 | Test |
| **SYS-CA-004** | The system shall prevent deletion of an identity record (`iam.identity`) that has active case assignments (`ON DELETE RESTRICT` on `identity_id` FK). The identity must first be unassigned from all cases or deactivated. | UN-CA-001 | Test |
| **SYS-CA-005** | The system shall cascade-delete case assignments when a case is deleted (`ON DELETE CASCADE` on `case_id` FK), so that removing a case does not leave orphaned assignment records. | N/A (Data integrity) | Test |
| **SYS-CA-006** | The system shall record the identity of the user or service account that made the assignment (`assigned_by`) and the timestamp of the assignment (`assigned_at`) for audit purposes. | UN-CA-004 | Inspection |
| **SYS-CA-007** | The system shall support a `sequence` field for controlling display ordering of people within a given designation group on a case (e.g., ordering multiple SECONDARY assignments). | UN-CA-002 | Test |
| **SYS-CA-008** | The worklist read model (`pathology_worklist.assigned_to_identity_id` and `assigned_to_display`) shall be synchronized from the `wsi.case_pathologists` table, reflecting the PRIMARY designation. The case_pathologists table is the source of truth; the worklist is a projection. | UN-CA-003 | Analysis/Test |

### 1.12.2 Assignment Pathways

| ID | System Requirement | Trace to User Need | Verification Method |
| :--- | :--- | :--- | :--- |
| **SYS-CA-009** | The system shall provide a single assignment API endpoint that accepts a case identifier, an identity identifier, a designation (`PRIMARY`, `SECONDARY`, `CONSULTING`, `GROSSING`), and an optional `assigned_by` identity. All assignment pathways (LIS-driven, algorithmic, manual) shall converge on this API. | UN-CA-001, UN-CA-002 | Test |
| **SYS-CA-010** | The system shall support LIS-driven assignment: when a case is ingested with pathologist metadata from the LIS (e.g., via HL7 ORM/OBR), the ingestion pipeline shall resolve the pathologist identity against `iam.identity` and create a case assignment via the assignment API. | UN-CA-001 | Analysis/Test |
| **SYS-CA-011** | The system shall support algorithmic assignment via integration with an external scheduling service (Qupanda): the system queries for the optimal pathologist based on specimen type, service line, and current availability, then writes the assignment via the assignment API. | UN-CA-001 | Analysis/Test |
| **SYS-CA-012** | The system shall support manual assignment: an authorized user (administrator or supervising pathologist) assigns or reassigns a case to a person directly through the application interface, using the same assignment API. | UN-CA-001, UN-CA-002 | Test |

# 2. Interface Requirements

| ID | Interface Requirement                                                                                          | Trace | Verification |
| :--- |:---------------------------------------------------------------------------------------------------------------| :--- | :--- |
| **IR-001** | The system shall validate all EHR write-back transactions using checksums and HL7 ACK/NACK protocols.          | N/A | Test |
| **API-01** | The system shall support OIDC authorization-code redirect handling via Spring Security OAuth2 login endpoints. | UN-AUTHN-002 | Test |
| **UI-01** | The login screen shall display the "Log in with Institution" button prominently.                               | UN-AUTHN-002 | Inspection |
| **UI-ADMIN-01** | The admin section shall be accessible at `/app/admin/*` routes with navigation integrated into the main application sidebar, visible only to users with `ROLE_ADMIN`. | UN-ADMIN-010 | E2E / Manual |
| **UI-ADMIN-02** | The admin identity list shall support search by email, name, and username, and filter by role and active status. | UN-ADMIN-001 | E2E / Manual |
