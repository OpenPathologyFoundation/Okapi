# 06-VVP
---
title: Verification and Validation Plan
document_id: DHF-06
version: 1.1
status: DRAFT
owner: Verification Lead
created_date: 2026-01-11
trace_source: SRS-001
---

# 1. Purpose
Define verification activities and acceptance evidence for AuthN/AuthZ capabilities (OIDC/SAML federation, internal RBAC, issuer-scoped normalization, Flyway-managed schema).

# 2. Scope
In scope:
- AuthN delegation to external IdP (OIDC) and startup behavior
- Internal RBAC mapping (IdP groups → internal roles)
- Database schema creation and baseline reference-data seeding via Flyway

Out of scope (this iteration):
- End-to-end browser-based OAuth2 authorization-code flow (full redirect + callback)
- Production operational controls (WAF, SIEM routing, object-lock configuration)

# 3. Verification Strategy
## 3.1 Automated unit tests (fast)
- Run via `auth-system: ./gradlew test`
- Uses Testcontainers Postgres with Flyway migrations (identical to production schema)

## 3.2 Automated integration tests (Docker required)
- Run via `auth-system: ./gradlew integrationTest`
- Uses Docker/Testcontainers when available (auto-skips if Docker not available)
- Verifies:
  - OIDC discovery against Keycloak
  - Flyway migrations on Postgres
  - Baseline reference data exists

## 3.3 Manual developer verification (local)
- Start Postgres (Docker Compose) and run `./gradlew bootRun`.
- Acceptance signal: Flyway applies migrations (`v1`, `v2`) and app starts.

## 3.4 HAT verification approach (planned)
HAT verification will be implemented incrementally as the module is built. At minimum:
- Unit tests for identifier normalization and deterministic match outcomes.
- Integration tests for append-only history invariants, request lifecycle transitions, and RBAC constraints.
- Manual workflows for scanner-driven confirmation and exception handling.

# 4. Requirements Verification Matrix (AuthN/AuthZ subset)
| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-AUTHN-001` | OIDC federation | Integration test | `auth-system/src/integrationTest/.../KeycloakOidcIntegrationTest` |
| `SYS-AUTHN-003` | MFA enforced via IdP policy | Analysis | Site IdP policy review; documented as external control |
| `SYS-AUTHZ-001` | Enforce RBAC roles | Unit test + code review | `UserRoleMapperTest`, `SecurityConfig` |
| `SYS-AUTHZ-002` | Issuer-scoped group→role mapping | Unit test | `UserRoleMapperTest` (providerId + groups) |
| `SYS-AUTHN-004` | Persist normalized identity (incl. structured name fields when provided) | Integration test (when Docker) + manual | `KeycloakOidcIntegrationTest` (DB assertions) / `bootRun` logs; verify columns (`given_name`, `family_name`, `display_short`) after OIDC login |
| `SYS-AUTHN-005` | `UNIQUE(provider_id, external_subject)` | Inspection | Flyway `V1__init_schema.sql` |
| `SYS-AUTHN-006` | Fail closed (`401/403`) | Unit test (config) | `SecurityConfigTest` |
| `SYS-AUTHN-008` | Authenticated “who am I” endpoint | Unit test | `auth-system/src/test/java/com/okapi/auth/controller/UserControllerTest.java` |
| `SYS-AUTHN-011` | AuthN events recorded in audit log | Unit/Integration test | `AuthAuditServiceTest` (planned) |
| `SYS-AUTHZ-010` | Default-deny authorization (no unmapped access) | Unit test | `UserRoleMapperTest` + permission checks |
| `SYS-AUTHZ-011` | Server-side authorization on protected APIs | Inspection/Test | `SecurityConfig` + controller tests |
| `SYS-AUTHZ-012` | Token augmentation with `roles`, `permissions`, and `okapi_authz_version` (`YYYY.MM.DD+<short-hash>`, 10 min TTL) | Analysis/Test | Token claim inspection in integration test; inspection of short-lived token policy (no per-token revocation) in `qms/dhf/04-SDS/02-AuthZ-Architecture.md` |
| `SYS-AUD-002` | Audit-event schema exists | Inspection | Flyway `V1__init_schema.sql` (`iam.audit_event`) |
| `SYS-DATA-001` | Flyway-managed schema at startup | Integration + manual | Flyway logs; `flyway_schema_history` table |
| `SYS-SEC-010` | No committed secrets; env/secret store | Inspection | `.gitignore` + configuration review; no secrets in DHF |

## 4.1 Requirements Verification Matrix (HAT subset)
| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-HAT-001` | Scan-first lookup + identifier normalization | Unit test | (Planned) `hat` module tests for normalization |
| `SYS-HAT-002` | Deterministic match outcomes | Unit test | (Planned) lookup outcome tests |
| `SYS-HAT-003` | Privacy-limited search modes | Analysis + test | Policy review + targeted tests (when implemented) |
| `SYS-HAT-004` | Authoritative current state view | Integration test | (Planned) current-state projection tests |
| `SYS-HAT-005` | Append-only event history | Integration test | (Planned) persistence + immutability invariants |
| `SYS-HAT-006` | Non-destructive corrections | Integration test | (Planned) correction-event tests |
| `SYS-HAT-007` | Create requests encoding intent | Integration test | (Planned) request-create tests |
| `SYS-HAT-008` | Request lifecycle + partial fulfillment | Integration test | (Planned) state transition tests |
| `SYS-HAT-009` | Execution queue + scan-confirmation | Integration + manual | (Planned) API tests + manual scanner workflow |
| `SYS-HAT-010` | Milestones + completion evidence | Integration test | (Planned) evidence persistence tests |
| `SYS-HAT-011` | Placeholder assets + reconcile later | Integration test | (Planned) placeholder + reconciliation tests |
| `SYS-HAT-012` | Explicit conflict resolution workflow | Integration test | (Planned) conflict + resolution event tests |
| `SYS-HAT-013` | RBAC + governance for high-risk actions | Unit + integration | (Planned) authorization tests |
| `SYS-HAT-014` | Traceability request → events → current state | Integration test | (Planned) traceability assertions |

## 4.2 Requirements Verification Matrix (Work List subset)
| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|-----------------------|---------------------|-------------------|
| `SYS-WL-001` | Read Model aggregation & Latency (<30s) | Performance Test | Load test report (simulated LIS/WSI events) |
| `SYS-WL-003` | Service-layer Capability Gating | Unit/Integration Test | `WorklistServiceTest`: Verify `Scope` filter applied to DB queries |
| `SYS-WL-004` | Privacy Modes (`TEACHING` vs `CLINICAL`) | UI Test (E2E) | Screenshot/Cypress: Verify MRN masking in Teaching Mode |
| `SYS-WL-007` | **Break-Glass** Workflow | Integration Test | Verify `AuditLog` contains `BREAK_GLASS` event with reason code |
| `SYS-WL-008` | Smart Filters ("Needs Attention") | Unit Test | Verify logic correctly flags "Old" and "Stat" cases |

## 4.3 Requirements Verification Matrix (IAM Administration subset)
| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-ADMIN-001` | Admin API endpoints gated by ROLE_ADMIN | Integration test | Controller tests: verify 200 with ADMIN role, 403 without |
| `SYS-ADMIN-002` | Identity soft-delete preserves audit history | Integration test | Deactivation test: set `is_active=false`, verify identity still queryable, audit events preserved |
| `SYS-ADMIN-003` | Admin actions recorded in audit log | Integration test | Perform admin action → verify corresponding audit event with actor/target/metadata |
| `SYS-ADMIN-004` | Audit event query with filters (type, actor, date range, outcome) | Integration test | Paginated query tests with each filter dimension |
| `SYS-ADMIN-005` | Grant and device revocation endpoints for any user | Integration test | Create grant → revoke via admin endpoint → verify state change |
| `SYS-ADMIN-006` | Read-only role-permission matrix endpoint | Unit test | Verify endpoint returns complete role→permission mapping |
| `SYS-ADMIN-007` | Role-conditional navigation rendering | E2E / Manual | Admin-only user screenshot vs clinician-admin user screenshot |
| `SYS-ADMIN-008` | Centralized auth context for frontend | Unit test | `/auth/me` returns full identity with roles and permissions list |
| `SYS-ADMIN-009` | Server-side enforcement is authoritative (UI advisory) | Integration test | Call admin API without ADMIN role → verify 403 regardless of UI state |

## 4.4 Requirements Verification Matrix (OVI — Orchestrator-Viewer Integration subset)

### 4.4.1 Window Lifecycle (SYS-OVI-001 to SYS-OVI-004)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-OVI-001` | Orchestrator opens viewer via `window.open()` with named window | Integration test | E2E test: verify `window.open()` call with correct URL and window name |
| `SYS-OVI-002` | Popup blocker detection within 3 seconds; user-actionable message | Integration test | E2E test: simulate popup blocker (null from window.open); verify error banner within 3s |
| `SYS-OVI-003` | Orchestrator polls viewer window liveness (checks `window.closed`) | Unit test | `ViewerBridge` test: verify polling interval and closed-state detection |
| `SYS-OVI-004` | Orchestrator displays indicator when viewer window is closed | UI test (E2E) | Screenshot: verify "Viewer closed" indicator in orchestrator after viewer window close |

### 4.4.2 Bridge Protocol (SYS-OVI-005 to SYS-OVI-008)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-OVI-005` | Typed message envelope: `{type, seq, payload}` | Unit test | `ViewerBridge` test: verify all outbound messages conform to typed envelope schema |
| `SYS-OVI-006` | Origin validation on every received postMessage | Unit test | `ViewerBridge` test: send message from wrong origin → verify silently dropped |
| `SYS-OVI-007` | Heartbeat protocol: 15s interval, configurable miss threshold | Unit test | `ViewerBridge` test: verify heartbeat every 15s; 3 misses → DEGRADED state |
| `SYS-OVI-008` | Message types: VIEWER_OPEN, VIEWER_READY, CASE_SWITCH, JWT_REFRESH, HEARTBEAT, etc. | Inspection | Code review: verify all 14 message types defined in bridge protocol |

### 4.4.3 Case Switching (SYS-OVI-009 to SYS-OVI-011)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-OVI-009` | Case switch requires user confirmation dialog when viewer is open | Integration test | E2E test: click different case in worklist → verify confirmation prompt |
| `SYS-OVI-010` | Case switch uses ACK protocol: orchestrator waits for CASE_SWITCH_ACK | Unit test | `ViewerBridge` test: send CASE_SWITCH → verify wait for ACK/REJECT/timeout |
| `SYS-OVI-011` | ACK timeout (configurable) triggers orchestrator warning | Unit test | `ViewerBridge` test: simulate ACK timeout → verify warning state |

### 4.4.4 JWT Provisioning (SYS-OVI-012 to SYS-OVI-015)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-OVI-012` | JWT minimum lifetime of 30 minutes | Analysis + Test | Token claim inspection: verify `exp - iat >= 1800` seconds |
| `SYS-OVI-013` | Proactive JWT refresh at 75% of lifetime when bridge connected | Unit test | `ViewerBridge` test: verify refresh message sent at 22.5 min for 30-min token |
| `SYS-OVI-014` | JWT stored in memory only — no localStorage/sessionStorage | Inspection + Test | Code review: no storage API calls for JWT; runtime inspection |
| `SYS-OVI-015` | JWT provisioned via VIEWER_OPEN and subsequent JWT_REFRESH messages | Unit test | `ViewerBridge` test: verify JWT in VIEWER_OPEN payload; verify JWT_REFRESH flow |

### 4.4.5 Degradation and Recovery (SYS-OVI-016 to SYS-OVI-020)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-OVI-016` | Amber degradation indicator on heartbeat miss | UI test (E2E) | Screenshot: verify amber indicator after simulated heartbeat timeout |
| `SYS-OVI-017` | Viewer continues full functionality in standalone mode | Integration test | Close orchestrator → verify viewer tiles, annotations, measurements work |
| `SYS-OVI-018` | Reconnect handshake: orchestrator sends BRIDGE_RECONNECT with case context | Unit test | `ViewerBridge` test: simulate reload → verify reconnect handshake message |
| `SYS-OVI-019` | Context mismatch on reconnect prompts user confirmation | Integration test | Reload orchestrator with different case → verify mismatch prompt in viewer |
| `SYS-OVI-020` | Viewer falls back to standalone mode if reconnection fails | Unit test | (Planned) `OrchestratorBridge` test: reconnect failure → standalone mode |

### 4.4.6 Session Awareness Service (SYS-OVI-021 to SYS-OVI-024)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-OVI-021` | Session Awareness Service is optional Layer 2; unavailability does not block viewing | Integration test | Disable session service → verify viewer opens and operates normally |
| `SYS-OVI-022` | WebSocket reconnection uses exponential backoff | Unit test | Session service client test: verify backoff intervals (1s, 2s, 4s, 8s...) |
| `SYS-OVI-023` | WebSocket heartbeat interval is 30s (proxy-compatible) | Unit test + Analysis | Verify heartbeat config; network analysis for proxy compatibility |
| `SYS-OVI-024` | Session service features degrade gracefully when unavailable | Integration test | Disable session service → multi-case warning absent but no error/crash |

## 4.5 Requirements Verification Matrix (Case Assignment subset)

### 4.5.1 Assignment Data Model (SYS-CA-001 to SYS-CA-008)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-CA-001` | `wsi.case_pathologists` table with `designation` column (PRIMARY, SECONDARY, CONSULTING, GROSSING); person's organizational position resolved from `iam.identity_roles` at query time | Inspection + Integration test | Flyway migration; integration test inserting assignments with each designation and verifying position resolved from identity |
| `SYS-CA-002` | At most one PRIMARY designation per case (partial unique index) | Integration test | Test: insert two PRIMARY assignments for same case → verify constraint violation; verify one PRIMARY + one SECONDARY succeeds |
| `SYS-CA-003` | No duplicate (case_id, identity_id) assignments | Integration test | Test: insert same identity twice on same case → verify unique constraint violation |
| `SYS-CA-004` | ON DELETE RESTRICT prevents identity deletion with active assignments | Integration test | Test: attempt to delete identity with case assignment → verify FK violation; verify deletion succeeds after unassigning |
| `SYS-CA-005` | ON DELETE CASCADE removes assignments when case is deleted | Integration test | Test: delete case → verify corresponding `case_pathologists` rows are removed |
| `SYS-CA-006` | `assigned_by` and `assigned_at` recorded on each assignment | Inspection | Flyway migration: verify columns exist with correct types and defaults |
| `SYS-CA-007` | Sequence field controls display ordering within designation group | Unit test | Test: insert multiple SECONDARY assignments with sequence values → verify query returns in sequence order |
| `SYS-CA-008` | Worklist `assigned_to_identity_id` syncs from PRIMARY in `case_pathologists` | Integration test | Test: assign PRIMARY → verify worklist row reflects assignment; change PRIMARY → verify worklist updates |

### 4.5.2 Assignment Pathways (SYS-CA-009 to SYS-CA-012)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-CA-009` | Single assignment API endpoint used by all pathways | Integration test | API test: POST assignment with case_id, identity_id, designation → verify row created; verify same endpoint works for all three pathway scenarios |
| `SYS-CA-010` | LIS-driven assignment via ingestion pipeline | Analysis + Integration test | Integration test: simulate HL7 message with assigned pathologist → verify identity resolved and case_pathologists row created |
| `SYS-CA-011` | Algorithmic assignment via Qupanda integration | Analysis + Integration test | (Planned) Integration test with Qupanda service mock → verify proportional assignment and case_pathologists row created |
| `SYS-CA-012` | Manual assignment via application interface | Integration test | E2E test: admin assigns case to pathologist through UI → verify case_pathologists row created with correct assigned_by |

## 4.6 Requirements Verification Matrix (Educational WSI Collections subset)

### 4.6.1 Educational Schema and Data Model (SYS-EDU-001 to SYS-EDU-005)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-EDU-001` | `wsi_edu` schema mirrors clinical `wsi` tables (cases, parts, blocks, slides, case_icd_codes, case_curators) | Inspection | Flyway migration: verify all tables exist with correct column definitions matching `wsi` counterparts |
| `SYS-EDU-002` | `wsi_edu.cases.collection` enforced as `'educational'`; no `patient_id` column | Inspection + Test | Migration inspection: verify CHECK constraint and column absence; test: attempt INSERT with `collection = 'clinical'` → verify constraint violation |
| `SYS-EDU-003` | Educational accession format `EDU{YY}-{NNNNN}` with auto-increment | Test | Integration test: create 3 cases → verify accession numbers `EDU26-00001`, `EDU26-00002`, `EDU26-00003` with correct year and zero-padding |
| `SYS-EDU-004` | Parts and blocks have `provenance` column (ACCESSIONED, IMPLIED, CURATED) | Inspection + Test | Migration: verify column exists with CHECK constraint; test: insert parts with each provenance value → verify acceptance; insert invalid value → verify rejection |
| `SYS-EDU-005` | `source_lineage` JSONB column on cases with structured origin types | Inspection + Test | Migration: verify column exists; test: insert cases with each lineage type (clinical_transfer, external_upload, public_dataset) → verify JSONB queryability |

### 4.6.2 Curator Assignment (SYS-EDU-006 to SYS-EDU-009)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-EDU-006` | `wsi_edu.case_curators` table with role column (PRIMARY_CURATOR, CURATOR, CONTRIBUTOR) | Inspection | Flyway migration: verify table definition, columns, FK relationships |
| `SYS-EDU-007` | At most one PRIMARY_CURATOR per case (partial unique index) | Integration test | Test: insert two PRIMARY_CURATOR assignments for same case → verify constraint violation; verify one PRIMARY_CURATOR + one CURATOR succeeds |
| `SYS-EDU-008` | Any active identity can be assigned as curator regardless of organizational role | Integration test | Test: assign identities with roles PATHOLOGIST, RESIDENT, PA, ADMIN as curators → verify all succeed |
| `SYS-EDU-009` | Curator and metadata changes recorded in audit trail | Integration test | Test: change curator assignment → verify audit event with actor, timestamp, old/new values; change case metadata → verify audit event |

### 4.6.3 Clinical-to-Educational Transfer (SYS-EDU-010 to SYS-EDU-012)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-EDU-010` | "Send to Education" creates educational case with full hierarchy, provenance=ACCESSIONED, source_lineage, primary curator | Integration test | Test: execute transfer for clinical case with 2 parts, 3 blocks, 5 slides → verify educational case created with correct hierarchy, all provenance=ACCESSIONED, source_lineage contains clinical case ID, requesting user is PRIMARY_CURATOR |
| `SYS-EDU-011` | Transferred slides have patient-identifying metadata stripped from file headers | Integration test | Test: transfer a slide with known patient name in SVS ImageDescription → verify educational copy does not contain the patient name; verify label associated image removed or replaced |
| `SYS-EDU-012` | Transferred slides have new HMAC computed; clinical HMAC unchanged | Integration test | Test: transfer slide → verify `wsi_edu.slides.hmac` differs from `wsi.slides.hmac` (different file content after stripping); verify clinical `wsi.slides.hmac` unchanged |

### 4.6.4 External Upload and Cold Ingestion (SYS-EDU-013 to SYS-EDU-015)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-EDU-013` | Direct upload creates case with implied hierarchy (1 part, 1 block, provenance=IMPLIED) | Integration test | Test: upload a single SVS file → verify educational case created with Part A (IMPLIED), Block 1 (IMPLIED), 1 slide |
| `SYS-EDU-014` | Scanner metadata extracted from uploaded file headers automatically | Integration test | Test: upload SVS with known magnification/dimensions → verify `wsi_edu.slides` populated with correct magnification, width_px, height_px, mpp values |
| `SYS-EDU-015` | Uploaded educational slides have HMAC-SHA256 computed with same key as clinical | Test | Test: compute HMAC externally with known key → verify `wsi_edu.slides.hmac` matches |

### 4.6.5 Teaching Annotations (SYS-EDU-016 to SYS-EDU-020)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-EDU-016` | `wsi_edu.annotations` table with required columns (type, geometry, visibility, author_id) | Inspection + Test | Migration: verify table definition; test: create annotations of each type with each visibility level → verify persistence |
| `SYS-EDU-017` | Visibility enforcement: PERSONAL → author only; SHARED → curators; TEACHING → instructor-controlled; PUBLIC → all | Integration test | Test: create annotations at each visibility level; query as author → see all own; query as non-author curator → see SHARED + PUBLIC; query as non-curator with VIEW_EDU → see PUBLIC only; query with teaching flag → see TEACHING |
| `SYS-EDU-018` | Annotation author_id recorded independently of case curator assignments | Inspection + Test | Test: identity NOT in case_curators creates annotation → verify author_id recorded; verify annotation queryable by author |
| `SYS-EDU-019` | Annotation geometry uses level-0 pixel coordinates (GeoJSON format) | Inspection | Code review: verify geometry stored in same coordinate system as SDS-ANN-001; verify PostGIS GIST index on geometry column |
| `SYS-EDU-020` | Teaching annotation toggle: session parameter controls TEACHING visibility without modifying records | Test | Test: create TEACHING annotation → query without flag → not returned; query with `show_teaching=true` → returned; verify annotation record unchanged |

### 4.6.6 Access Control and Discovery (SYS-EDU-021 to SYS-EDU-024)

| SRS ID | Requirement (summary) | Verification method | Evidence/artifact |
|--------|------------------------|---------------------|------------------|
| `SYS-EDU-021` | `VIEW_EDU_COLLECTION` permission independent of clinical permissions | Integration test | Test: user with VIEW_EDU but no clinical permissions → can browse educational cases; user with clinical permissions but no VIEW_EDU → cannot access educational collection |
| `SYS-EDU-022` | Named collections (name, owner, visibility) with many-to-many case membership | Integration test | Test: create named collection → add 3 cases → verify membership; add same case to 2 collections → verify dual membership; query cases by collection → verify correct set returned |
| `SYS-EDU-023` | Search by ICD code, anatomic site, stain, diagnosis text, annotation text, curator name | Integration test | Test: populate 10 educational cases with varied metadata → search by each dimension → verify correct result sets; full-text search across diagnosis and annotation labels |
| `SYS-EDU-024` | Educational cases carry ICD codes and metadata in same structure as clinical | Inspection + Test | Test: insert educational case with ICD codes → query by ICD code → verify returned; query metadata JSONB fields → verify GIN index used |

# 5. Validation Notes
Clinical workflow validation (usability, human factors, and operational monitoring) will be defined in later V&V activities once clinical workflows are implemented beyond IAM.

Educational workflow validation (teaching session usability, annotation discovery, curriculum management) will be defined once the educational module UI is implemented.
