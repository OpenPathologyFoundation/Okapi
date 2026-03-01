# 04-SDS

---
title: Software Design Specification - Overview
document_id: DHF-04
version: 2.0
status: DRAFT
owner: Lead Architect
created_date: 2026-01-09
updated_date: 2026-01-26
trace_source: SRS-001
---

# 1. Introduction

This Software Design Specification (SDS) describes the technical architecture and design for the Okapi system. The SDS is organized into the following component documents:

| Document | ID | Description |
|----------|-----|-------------|
| [01-AuthN-Architecture.md](01-AuthN-Architecture.md) | DHF-04-01 | Authentication: Identity federation, session management, device trust |
| [02-AuthZ-Architecture.md](02-AuthZ-Architecture.md) | DHF-04-02 | Authorization: RBAC, permissions, IdP group mapping, break-glass, research grants |
| [03-IAM-Schema.md](03-IAM-Schema.md) | DHF-04-03 | IAM database schema specification |
| [04-HAT-Architecture.md](04-HAT-Architecture.md) | DHF-04-04 | Histology Asset Tracking module design |
| [05-Worklist-Architecture.md](05-Worklist-Architecture.md) | DHF-04-05 | Work List module design |

# 2. Architectural Overview

## 2.1 IAM Architecture

Okapi follows a "Connector + Normalization" architecture for identity, with clear separation between:

- **Authentication (AuthN)** — Proving identity; delegated to external Enterprise Identity Providers (IdPs)
- **Authorization (AuthZ)** — Determining permissions; managed internally via Role-Based Access Control (RBAC)

Institutional SSO is the source of authentication truth (Keycloak used to emulate OIDC/SAML in development). Authorization is derived from Okapi's internal IAM schema (roles, permissions, grants) and is used to augment access tokens for downstream services.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌──────────┐         ┌──────────────────┐         ┌──────────────────┐
    │          │         │                  │         │                  │
    │   User   │────────▶│   Okapi Web UI   │────────▶│  Okapi Auth API  │
    │          │         │  (Clinical +     │         │                  │
    │          │         │   Admin views)   │         │                  │
    └──────────┘         └──────────────────┘         └────────┬─────────┘
                                                               │
                         ┌─────────────────────────────────────┼─────────────┐
                         │                                     │             │
                         ▼                                     ▼             │
              ┌─────────────────────┐              ┌─────────────────────┐   │
              │   AuthN Module      │              │   AuthZ Module      │   │
              │   (DHF-04-01)       │              │   (DHF-04-02)       │   │
              │                     │              │                     │   │
              │  • OIDC Client      │──Identity───▶│  • RBAC Engine      │   │
              │  • SAML 2.0 SP      │              │  • Permission Check │   │
              │  • Session Mgmt     │              │  • Break-Glass      │   │
              │  • Device Trust     │              │  • Research Grants  │   │
              └──────────┬──────────┘              └──────────┬──────────┘   │
                         │                                    │              │
                         ▼                                    ▼              │
              ┌─────────────────────┐              ┌─────────────────────┐   │
              │   External IdPs     │              │   IAM Schema        │   │
              │                     │              │   (DHF-04-03)       │   │
              │  • Okta             │              │                     │   │
              │  • Entra ID         │              │  • iam.identity     │   │
              │  • Auth0            │              │  • iam.role         │   │
              │  • Hospital SAML    │              │  • iam.permission   │   │
              └─────────────────────┘              │  • iam.audit_event  │   │
                                                  └─────────────────────┘   │
                                                                             │
                         ┌───────────────────────────────────────────────────┘
                         ▼
              ┌─────────────────────┐
              │   Audit Service     │
              │                     │
              │  • Login/Logout     │
              │  • Access Denied    │
              │  • Role Changes     │
              │  • Break-Glass      │
              └─────────────────────┘
```

## 2.2 HAT Architecture

Histology Asset Tracking follows a separation-of-concerns pattern:
- **Facts/state** about physical assets (identifiers, location, custody, provenance, history)
- **Intent/work** captured as requests and executed steps, recorded as events

See [04-HAT-Architecture.md](04-HAT-Architecture.md) for detailed design.

## 2.3 Work List Architecture

The Work List module aggregates case data from multiple sources (LIS, imaging, internal authoring) into a unified view with privacy controls.

See [05-Worklist-Architecture.md](05-Worklist-Architecture.md) for detailed design.

# 3. Cross-Cutting Concerns

## 3.1 Security Controls

| Control | Implementation | Requirement |
|---------|----------------|-------------|
| **External AuthN** | Authentication delegated to external IdPs; no password storage | SYS-AUTHN-001, SYS-AUTHN-002 |
| **Fail Closed** | Invalid tokens or missing auth context yields 401/403 | SYS-AUTHN-006 |
| **Least Privilege RBAC** | Fine-grained permissions with IdP group derivation | SYS-AUTHZ-001, SYS-AUTHZ-003 |
| **Break-Glass Audit** | Emergency access is justified, time-bounded, fully audited | SYS-AUTHZ-008 |
| **PHI Controls** | Research grants enforce minimum necessary PHI exposure | SYS-RES-002 |
| **No Committed Secrets** | Secrets via env vars/secret store | SYS-SEC-010 |
| **Admin API Gating** | All admin endpoints require `ROLE_ADMIN`; UI checks advisory only | SYS-ADMIN-001, SYS-ADMIN-009 |
| **Admin Audit Trail** | All admin operations recorded with actor attribution | SYS-ADMIN-003 |

## 3.2 Audit Foundation

All IAM-related events are recorded in `iam.audit_event` with:
- Actor identity and context
- Target entity type and ID
- Outcome and reason
- Request/session correlation
- Structured metadata

## 3.3 Data Management

- Schema is managed via Flyway migrations
- IAM uses a dedicated `iam` PostgreSQL schema
- No cross-schema foreign keys; domain tables reference IAM by ID

# 4. Traceability Summary

| Design Element | System Requirement | Risk Control |
|----------------|-------------------|--------------|
| AuthN Gateway (OIDC/SAML) | SYS-AUTHN-001, SYS-AUTHN-002 | RISK-001 |
| Issuer-scoped identity | SYS-AUTHN-005 | RISK-009 |
| Session/device trust | SYS-AUTHN-007, SYS-AUTHN-009 | RISK-011 |
| RBAC enforcement | SYS-AUTHZ-001, SYS-AUTHZ-002 | RISK-010 |
| Time-bounded roles | SYS-AUTHZ-004 | RISK-011 |
| Break-glass access | SYS-AUTHZ-008 | RISK-015 |
| Research grants | SYS-RES-001, SYS-RES-002 | RISK-016 |
| Audit events | SYS-AUD-001, SYS-AUD-002 | RISK-006 |
| Flyway migrations | SYS-DATA-001 | RISK-012 |
| Admin API (ROLE_ADMIN gated) | SYS-ADMIN-001, SYS-ADMIN-009 | RISK-ADMIN-003 |
| Admin audit trail | SYS-ADMIN-003 | RISK-ADMIN-001 |
| Role-conditional navigation | SYS-ADMIN-007 | RISK-ADMIN-001 |
