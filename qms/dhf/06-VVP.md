# 06-VVP
---
title: Verification and Validation Plan
document_id: DHF-06
version: 1.0
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
- Use in-memory H2; Flyway disabled; Hibernate `ddl-auto: create-drop`

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
| `SYS-AUTH-001` | OIDC federation | Integration test | `auth-system/src/integrationTest/.../KeycloakOidcIntegrationTest` |
| `SYS-AUTH-003` | MFA enforced via IdP policy | Analysis | Site IdP policy review; documented as external control |
| `SYS-AUTH-004` | Enforce RBAC roles | Unit test + code review | `UserRoleMapperTest`, `SecurityConfig` |
| `SYS-AUTH-009` | Issuer-scoped group→role mapping | Unit test | `UserRoleMapperTest` (providerId + groups) |
| `SYS-AUTH-005` | Persist normalized identity (incl. structured name fields when provided) | Integration test (when Docker) + manual | `KeycloakOidcIntegrationTest` (DB assertions) / `bootRun` logs; verify columns (`given_name`, `family_name`, `display_short`) after OIDC login |
| `SYS-AUTH-006` | `UNIQUE(provider_id, external_subject)` | Inspection | Flyway `V1__init_schema.sql` |
| `SYS-AUTH-007` | Fail closed (`401/403`) | Unit test (config) | `SecurityConfigTest` |
| `SYS-AUTH-010` | Authenticated “who am I” endpoint | Unit test | `auth-system/src/test/java/com/okapi/auth/controller/UserControllerTest.java` |
| `SYS-AUD-002` | Audit-event schema exists | Inspection | Flyway `V1__init_schema.sql` (`audit_events`) |
| `SYS-DATA-003` | Flyway-managed schema at startup | Integration + manual | Flyway logs; `flyway_schema_history` table |
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

# 5. Validation Notes
Clinical workflow validation (usability, human factors, and operational monitoring) will be defined in later V&V activities once clinical workflows are implemented beyond IAM.