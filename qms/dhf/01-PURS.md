# 01-PURS
---
title: Product & User Requirements Specification (PURS) - IAM + Histology Asset Tracking
document_id: URS-001
version: 1.0
status: DRAFT
owner: Product Owner
created_date: 2026-01-09
effective_date: TBD
trace_destination: SRS-001
---

# 1. Purpose
To define user needs for Okapi modules in scope for the current baseline, including:
- Identity and Access Management (IAM) (authentication + authorization)
- Histology Asset Tracking (HAT)

# 2. Scope
This document covers user needs for:
- login, identity federation, and **role-based access** for clinical users of Okapi
- tracking and orchestrating work on histology physical assets (slides/blocks/specimen parts)

# 3. User Needs

## 3.1 IAM (Authentication + Authorization)

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-001** | The user needs a single, universal account to access multiple applications within the ecosystem. | Reduces password fatigue and improves workflow efficiency for clinicians. |
| **UN-002** | The user needs to choose their preferred institutional identity provider (e.g., Google, Microsoft, Hospital ID). | Facilitates use of existing institutional credentials; prevents creation of "shadow IT" accounts. |
| **UN-003** | The user needs the login process to be rapid but highly secure, utilizing modern verification. | "Rapid" prevents delay in care; "Secure" protects PHI. |
| **UN-004** | The user needs the system to remember their device to minimize repetitive login actions. | Mitigates risk of users leaving screens unlocked due to frustration with frequent re-logins. |
| **UN-005** | The user needs access to be restricted by clinical role (e.g., Pathologist, Technician, Admin) so only appropriate actions are available. | Prevents inappropriate access and reduces risk of unsafe or non-compliant actions. |
| **UN-006** | The user needs the system to automatically reflect institutional group membership (IdP groups) into Okapi permissions without manual account provisioning. | Reduces admin burden and prevents inconsistent access assignments. |
| **UN-007** | The user needs the system to record authentication and authorization-relevant events in an auditable manner. | Supports security monitoring, investigations, and regulatory expectations. |
| **UN-008** | The administrator needs a reliable, low-error way to grant and revoke access by managing centralized identity provider groups and Okapi-local permission mappings (without creating credentials in Okapi). | Reduces configuration mistakes and avoids “shadow accounts”; supports controlled access management aligned to institutional identity governance. |
| **UN-009** | The user needs access provisioning and updates to occur in a timely manner to avoid care delays caused by missing or incorrect permissions. | Delayed access can cause clinical workflow interruption; timely provisioning reduces pressure to bypass security controls. |

## 3.2 HAT (Histology Asset Tracking)

HAT separates **facts** (what the system knows about an asset: identifiers/status/location/custody/provenance/history) from **intent/work** (requests and execution steps performed by staff).

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-HAT-001** | The user needs scan-first asset lookup (barcode-first) with robust normalization and deterministic outcomes (matched / not found / ambiguous). | Prevents wrong-asset handling and reduces delays at the bench. |
| **UN-HAT-002** | The user needs to see an asset’s current state: status, location, custody (who/when/due back), lifecycle flags, and provenance (which system asserted what). | Supports safe chain-of-custody and reduces loss/misplacement. |
| **UN-HAT-003** | The user needs a defensible, immutable event history for asset status/location/custody changes, including who/when/comment and non-destructive corrections. | Supports traceability, investigation, and regulatory defensibility. |
| **UN-HAT-004** | The user needs to request work on assets (retrieve, stain, QA, distribute, scan, etc.) with priority/due date, assignee/team, and partial fulfillment support. | Enables orchestration without relying on ad hoc email/paper processes; supports clinical throughput. |
| **UN-HAT-005** | Histology staff needs an execution/work-queue view where tasks are scan-confirmed and intermediate milestones are recorded with completion evidence (e.g., tracking number, destination, receipt/return). | Prevents wrong-asset actions and creates end-to-end accountability. |
| **UN-HAT-006** | The user needs list-driven workflows (bulk lookup and bulk request creation) and resilient handling of unknown assets (create placeholder + reconcile later). | Supports real-world batch workflows and reduces operational friction when assets are not yet in the system. |
| **UN-HAT-007** | The system must enforce role-based access and governance for high-risk actions (e.g., external distribution, research release) and apply privacy controls for searching where required. | Reduces risk of unauthorized release/misuse and avoids inappropriate exposure of identifiers/PHI. |

## 3.3 Work List Module (WL)

The Work List is the "Front Door" to the Pathology Portal, serving as the primary case discovery and organization tool.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-WL-001** | The pathologist needs a central work list that aggregates cases from LIS, imaging, and internal authoring workflows into a single view. | Eliminates the need to check multiple systems; provides a single "source of truth" for daily work. |
| **UN-WL-002** | The user needs to see the real-time status of slide digitization and report drafting directly on the work list without opening the case. | Saves time by preventing users from opening cases that are not yet ready (e.g., slides not scanned) or are already being worked on by others. |
| **UN-WL-003** | The user needs to distinguish between "clinical view" (full PHI) and "teaching view" (masked PHI) on demand. | Facilitates safe use of the system in semi-public settings (conference rooms, educational rounds) without risking PHI exposure. |
| **UN-WL-004** | The user needs to filter and sort cases by clinical priority (stat/urgent), service type, and age. | Ensures critical cases are addressed first; helps manage large caseloads efficiently. |
| **UN-WL-005** | The user needs visibility into multi-author collaboration states (e.g., "Draft by Resident", "Pending Attending Review"). | Essential for academic workflows; prevents "blind handoffs" and clarifies responsibilities. |
| **UN-WL-006** | The user needs a "Break-Glass" mechanism to access cases not normally assigned to them in emergency or coverage situations. | Ensures patient safety isn't compromised by rigid permission models during staff absences or emergencies. |
| **UN-WL-007** | The user needs the system to clearly indicate when information is "stale" or updating. | Prevents clinical decisions based on outdated information; builds trust in system reliability. |

# 4. Notes
System requirements are defined in `qms/dhf/02-SRS.md` and verified per `qms/dhf/06-VVP.md`.