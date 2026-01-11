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

# 5. Validation Notes
Clinical workflow validation (usability, human factors, and operational monitoring) will be defined in later V&V activities once clinical workflows are implemented beyond IAM.