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
- **administrative management** of identities, roles, permissions, grants, and audit
- tracking and orchestrating work on histology physical assets (slides/blocks/specimen parts)

# 3. User Needs

## 3.1 IAM - Authentication (AuthN)

User needs related to proving identity and establishing sessions.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-AUTHN-001** | The user needs a single, universal account to access multiple applications within the ecosystem. | Reduces password fatigue and improves workflow efficiency for clinicians. |
| **UN-AUTHN-002** | The user needs to choose their preferred institutional identity provider (e.g., Google, Microsoft, Hospital ID). | Facilitates use of existing institutional credentials; prevents creation of "shadow IT" accounts. |
| **UN-AUTHN-003** | The user needs the login process to be rapid but highly secure, utilizing modern verification. | "Rapid" prevents delay in care; "Secure" protects PHI. |
| **UN-AUTHN-004** | The user needs the system to remember trusted devices to minimize repetitive login actions while maintaining security. | Mitigates risk of users leaving screens unlocked due to frustration with frequent re-logins; device trust must be revocable. |
| **UN-AUTHN-005** | The user needs to view and manage their trusted devices, including the ability to revoke trust from devices they no longer use or control. | Supports user self-service for security hygiene; enables response to lost/stolen devices without admin intervention. |
| **UN-AUTHN-006** | The user needs the system to record authentication events (login, logout, session expiry, device trust changes) in an auditable manner. | Supports security monitoring, investigations, and regulatory expectations (HIPAA audit requirements). |
| **UN-AUTHN-007** | The user needs to see their identity information (name, email, provider, roles) within the system to verify they are logged in correctly. | Prevents misidentification errors; supports troubleshooting of access issues; essential for multi-provider environments. |

## 3.2 IAM - Authorization (AuthZ)

User needs related to permissions, roles, and access control decisions.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-AUTHZ-001** | The user needs access to be restricted by clinical role (e.g., Pathologist, Technician, Admin) so only appropriate actions are available. | Prevents inappropriate access and reduces risk of unsafe or non-compliant actions. |
| **UN-AUTHZ-002** | The user needs the system to automatically reflect institutional group membership (IdP groups) into Okapi roles without manual account provisioning. | Reduces admin burden and prevents inconsistent access assignments. |
| **UN-AUTHZ-003** | The user needs fine-grained permissions within roles (e.g., view vs. edit vs. sign-out) so that access is appropriately scoped to job function. | Supports least-privilege access; enables nuanced access models (e.g., resident can draft but not sign). |
| **UN-AUTHZ-004** | The user needs time-bounded role assignments for temporary access scenarios (e.g., coverage, training rotations, locum tenens). | Eliminates stale access from expired assignments; reduces admin burden of manual revocation. |
| **UN-AUTHZ-005** | The administrator needs a reliable, low-error way to grant and revoke access by managing centralized identity provider groups and Okapi-local permission mappings (without creating credentials in Okapi). | Reduces configuration mistakes and avoids "shadow accounts"; supports controlled access management aligned to institutional identity governance. |
| **UN-AUTHZ-006** | The user needs access provisioning and updates to occur in a timely manner to avoid care delays caused by missing or incorrect permissions. | Delayed access can cause clinical workflow interruption; timely provisioning reduces pressure to bypass security controls. |
| **UN-AUTHZ-007** | The user needs the system to record authorization-relevant events (role changes, permission grants/revocations, access denials) in an auditable manner. | Supports security monitoring, investigations, and regulatory expectations. |
| **UN-AUTHZ-008** | The user needs emergency "break-glass" access to resources outside their normal assignment when clinical necessity demands (e.g., covering for absent colleague, emergency consult). | Ensures patient safety is not compromised by rigid permission models; break-glass must be justified, time-limited, and audited. |
| **UN-AUTHZ-009** | The administrator needs to define which users are eligible for break-glass access and the scope of resources they may access in emergency situations. | Enables institutional policy enforcement while supporting clinical flexibility; prevents abuse of emergency access. |

## 3.3 IAM - User Profile and Preferences

User needs related to identity attributes and personalization.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-PROF-001** | The user needs the system to capture and display their professional identity (name components, credentials, title) accurately for clinical documentation and attribution. | Supports accurate report attribution; enables proper display of credentials (Dr., MD, PhD) in clinical contexts. |
| **UN-PROF-002** | The user needs to store personal preferences (display settings, notification preferences, default views) that persist across sessions and devices. | Improves efficiency by reducing repetitive configuration; supports personalized workflows. |
| **UN-PROF-003** | The user needs changes to their profile and preferences to be recorded for audit purposes. | Supports investigation of profile-related issues; regulatory compliance. |
| **UN-PROF-004** | The administrator needs to distinguish between production users, test users, demo users, and service accounts for operational and compliance purposes. | Enables exclusion of test/demo data from production metrics; supports compliance reporting; prevents accidental production access by test accounts. |

## 3.4 IAM - Research Access

User needs related to governed research access with PHI controls.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-RES-001** | The researcher needs time-bounded, protocol-specific access to specimens/cases for approved research studies. | Supports research workflows while maintaining governance; enables IRB compliance. |
| **UN-RES-002** | The researcher needs PHI exposure to be controlled and limited to the minimum necessary for their research protocol (none, masked, limited, full). | Supports HIPAA minimum necessary standard; enables de-identified research workflows. |
| **UN-RES-003** | The research administrator needs to approve, modify, and revoke research access grants with documented justification. | Supports institutional oversight of research access; enables audit trail for IRB compliance. |
| **UN-RES-004** | The system needs to record all research access grants, approvals, and revocations for compliance audit purposes. | Supports IRB reporting requirements; enables investigation of research access patterns. |

## 3.5 HAT (Histology Asset Tracking)

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

## 3.6 Work List Module (WL)

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

## 3.7 IAM Administration (Admin)

User needs for the institutional administrator managing identities, roles, permissions, IdP mappings, grants, and audit.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-ADMIN-001** | The administrator needs to view, search, and filter all user identities in the system to manage access across the institution. | Enables efficient user lookup for access troubleshooting, onboarding verification, and compliance auditing; prevents reliance on ad hoc database queries. |
| **UN-ADMIN-002** | The administrator needs to activate and deactivate user identities without deleting historical records. | Supports offboarding workflows (e.g., departed staff, expired affiliations) while preserving audit trail and case attribution integrity for regulatory defensibility. |
| **UN-ADMIN-003** | The administrator needs to assign and revoke roles for individual users with documented justification. | Supports local role overrides beyond IdP-derived assignments (e.g., temporary coverage, cross-training); justification requirement supports audit trail. |
| **UN-ADMIN-004** | The administrator needs to manage IdP group-to-role mappings to control how institutional directory groups translate to Okapi permissions. | Enables centralized identity governance; reduces configuration drift between IdP groups and Okapi roles; supports multi-tenant/multi-IdP scenarios. |
| **UN-ADMIN-005** | The administrator needs to view the role-permission matrix to understand what capabilities each role provides. | Supports access review, compliance reporting, and informed decision-making when assigning roles or responding to access requests. |
| **UN-ADMIN-006** | The administrator needs to view, filter, and export audit logs for security investigations and compliance reporting. | Supports HIPAA audit requirements, incident response, and institutional security monitoring without requiring direct database access. |
| **UN-ADMIN-007** | The administrator needs to view and revoke active break-glass and research access grants across all users. | Enables institutional oversight of emergency and research access; supports timely response to grant abuse or policy violations. |
| **UN-ADMIN-008** | The administrator needs to view and revoke trusted devices for any user in response to security incidents (e.g., lost/stolen devices, compromised accounts). | Supports incident response workflows; complements user self-service device management with institutional authority. |
| **UN-ADMIN-009** | The administrator needs a dashboard summarizing system state: active user counts, recent authentication activity, active emergency/research grants, and security-relevant events. | Provides situational awareness for system health and security posture; enables proactive identification of anomalies. |
| **UN-ADMIN-010** | An administrator who manages only user access needs to see only administrative functions; an administrator who also holds a clinical role (e.g., Pathologist) needs seamless access to both clinical and admin views within the same application. | Avoids cognitive overload for admin-only users; eliminates the need for dual-app login for clinician-administrators; supports least-privilege UI presentation. |
| **UN-ADMIN-011** | The administrator needs all administrative actions (identity changes, role assignments, mapping updates, grant revocations, device revocations) to be recorded in the audit log with actor attribution. | Supports non-repudiation of administrative actions; enables investigation of privilege changes; required for regulatory compliance. |

# 4. Notes
System requirements are defined in `qms/dhf/02-SRS.md` and verified per `qms/dhf/06-VVP.md`.