# 05-Worklist-Architecture
---
title: Work List Module Architecture
document_id: SDS-005
version: 1.0
status: DRAFT
owner: Lead System Engineer
created_date: 2026-01-20
trace_source: SRS-001 (SYS-WL-*)
---

# 1. Introduction
The Work List Module sends cases to pathologists. It is a **Read-Heavy** component that aggregates data from multiple sources (LIS, Imaging, Authoring) into a unified viewing experience.

# 2. Architecture: The Read Model
The core of the Work List is a **denormalized Read Model** (likely a dedicated database table or view `pathology_worklist_view`) that is populated by an event-driven ingestion pipeline.

## 2.1 Why a Read Model?
- **Performance:** Complex joins across LIS, Imaging, and Authoring tables would be too slow for real-time filtering and sorting.
- **Decoupling:** The Work List needs to display data even if the LIS or Imaging systems are temporarily slow or offline.
- **Aggregation:** We need to compute derived states (e.g., "Ready for Sign-out") ahead of time.

## 2.2 Data Flow
`[LIS (HL7)]` -> `[Event Ingestion]` -> `[Read Model]`
`[Imaging (API)]` -> `[Poller/Webhook]` -> `[Read Model]`
`[Authoring DB]` -> `[Internal Events]` -> `[Read Model]`

# 3. Data Model Schema
The Read Model contains the following core structures:

## 3.1 Core Identification
- `accession_number` (PK)
- `patient_mrn` (Encrypted/Protected)
- `patient_name` (Encrypted/Protected)
- `service_type` (e.g., "Surgical", "Cytology")

## 3.2 Workflow State
- `lis_status` (e.g., "Grossed", "Slides Cut")
- `wsi_status` (e.g., "3/12 Scanned", "QC_FAILED")
- `authoring_status` (e.g., "Draft", "Pending Review")
- `assignee_id` (User ID or Service Queue ID)

## 3.3 Enrichment Maps (JSONB)
- `annotations`: Summary of user annotations
- `alerts`: Active clinical alerts
- `capabilities`: Pre-computed permission flags for the current viewer

# 4. Permissions & Capability Gating
Security is enforced at the **Service Layer**, not just the UI.

## 4.1 Filtering Strategy
All queries to the Work List API must include a mandatory `scope` filter:
- `SCOPE_SELF`: `WHERE assignee_id = :current_user`
- `SCOPE_SERVICE`: `WHERE service_type IN (:user_services)`
- `SCOPE_ADMIN`: (Requires `VIEW_OTHERS_WORKLISTS`)

## 4.2 Break-Glass Access
Accessing a case outside the user's scope requires a "Break-Glass" token.
1. User requests access -> "Break Glass" UI prompt.
2. Logic records audit event: `type=BREAK_GLASS`, `reason=COVERAGE`, `target=CASE_123`.
3. Temporary access capability is granted for the session.

# 5. Performance Strategy
## 5.1 Async Enrichment
To ensure `<2s` First Contentful Paint (FCP):
1. **Load Core Data:** Accession, Patient, Status (Immediate)
2. **Async Fetch:** WSI Status, AI Results (Lazy loaded via separate API calls or WebSocket)

## 5.2 Refresh Rate
- **Auto-Refresh:** Configurable (default 2 mins).
- **Manual:** User triggered.
- **Push:** WebSocket notifications for critical updates (e.g., "New Urgent Case").
