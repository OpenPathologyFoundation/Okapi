# 01-PURS
---
title: User Requirements Specification - Authentication Module
document_id: URS-001
version: 1.0
status: DRAFT
owner: Product Owner
created_date: 2026-01-09
effective_date: TBD
trace_destination: SRS-001
---

# 1. Purpose
To define the user needs for the Authentication and Authorization module of the [Project Name] system, ensuring secure and efficient access for clinical users.

# 2. Scope
This document covers user needs for login, identity federation, and **role-based access** for clinical users of Okapi.

# 3. User Needs

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

# 4. Notes
System requirements are defined in `qms/dhf/02-SRS.md` and verified per `qms/dhf/06-VVP.md`.