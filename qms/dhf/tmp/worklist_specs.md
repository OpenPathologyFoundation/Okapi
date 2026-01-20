**Pathology Portal Work List Module**

Narrative Specification — Phase 1

*Working Draft for Development Team*

| Document Version: | 1.1 |
| :---- | :---- |
| **Date:** | January 2026 |
| **Status:** | Working Draft |
| **Author:** | Peter Gershkovich |
| **Related Documents:** | Report Authoring Module Spec, Digital Viewer Module Spec |

 

**PART I: INTRODUCTION AND PURPOSE**

# **1\. Introduction and Purpose**

## **1.1 The Front Door to Everything**

The work list is the entry point to the Pathology Portal. Before a pathologist can use the sophisticated authoring tools we've designed, they need to find their cases. The work list serves this fundamental purpose: it shows what's ready to be worked on, helps pathologists organize their day, and provides quick access to any case in the system.

A work list may appear deceptively simple—it's just a table of cases. But the decisions about what information to display, how to filter and sort, and how to handle edge cases will determine whether pathologists actually use this system or maintain separate spreadsheets on the side.

## **1.2 What This Module Does**

The work list module receives case information from the Laboratory Information System (LIS) via HL7 interface and presents it in an actionable format. It also incorporates real-time data from the whole slide imaging (WSI) system to show digitization status. Finally, it reflects internal state from our own authoring module—particularly important for multi-author workflows where residents draft reports for attending review.

The work list is not a comprehensive case management system. It's a focused tool for answering the question: *What needs my attention, and what's ready to work on?*

## **1.3 Module Dependencies**

The work list is a **UI module within the orchestration kernel**. It does not define its own identity, authentication, or authorization systems. Instead, it consumes these services via established APIs and database views:

* **Identity and Authentication** — Provided by institutional identity services

* **Authorization** — Capability-based permissions from the orchestration kernel

* **Event ingestion** — Workflow events from LIS, imaging, and authoring systems

* **Alerting rules** — Derived from orchestration kernel's rules engine

The work list queries a **read model** populated by these upstream systems. It does not subscribe to raw event streams directly, nor does it compute complex business rules. It renders pre-computed state.

**PART II: ARCHITECTURE AND DATA MODEL**

# **2\. Data Sources and Integration**

## **2.1 The Worklist Read Model**

The work list queries a **Worklist Read Model**—a database view or table populated by orchestration events and upstream system integrations. This read model consolidates information from multiple sources into a single queryable structure, optimized for the work list's display and filtering needs.

The read model is updated by background processes that consume events from LIS, imaging, and authoring systems. The work list UI queries this read model; it does not query source systems directly.

## **2.2 Read Model Schema**

Each row in the worklist read model contains the following field groups:

### **Core Identification**

* `lab_code` — Laboratory identifier (for multi-lab deployments)

* `accession_number` — Lab-qualified unique case identifier

* `patient_display` — Mode-dependent patient rendering (see Privacy Modes)

### **Workflow State**

* `service` — Pathology service (surgical, derm, cyto, etc.)

* `queue` — Assignment queue within service

* `assigned_to` — Current assignee (if institution uses assignment)

* `status_tuple` — Composite of LIS processing stage \+ authoring stage

### **Enrichment Signals**

* `slide_summary` — Slides known, digitized, QC flags

* `annotation_summary` — Presence and counts of annotations

* `ai_summary` — AI pipeline status (queued/running/complete/failed)

* `alert_summary` — Derived alert levels from rules engine

### **Capability and Extensibility**

* `capabilities` — What the current user can see/do for this row

* `extensions` — JSONB column for future enrichment fields

## **2.3 Information from the LIS (via HL7)**

The following data elements are received from the Laboratory Information System and populate the read model:

* **Accession number** — The unique case identifier

* **Pathology service** — Surgical pathology, cytology, dermatopathology, etc.

* **Accession date** — When the case was received

* **Case status** — Processing stage (accessioned, grossed, slides cut, etc.)

* **Patient demographics** — Medical record number (MRN) and patient name

* **Specimen class** — Biopsy, excision, resection, etc.

* **Case priority** — Routine, urgent, or stat

* **Assigned pathologist** — If the institution uses explicit case assignment

## **2.4 Information from the Imaging System**

The whole slide imaging system provides digitization status via API, captured in the read model:

* **Slides digitized** — Count of slides that have been scanned

* **Total slides** — Total number of slides for the case

* **Verification status** — Whether scans have been quality-checked

* **QC flags** — Any quality issues (focus, tissue detection, etc.)

This information is queried asynchronously. The work list loads immediately with core data, and enrichment fields populate as they become available. This prevents the imaging system from becoming a bottleneck for page load.

## **2.5 Information from Our Authoring Module**

The work list also reflects internal state from the report authoring module:

* **Authoring status** — Draft in progress, submitted for review, etc.

* **Multi-author workflow state** — Who has worked on the draft, who it's waiting for

* **Amendment status** — If a signed case has a pending amendment

## **2.6 Data Precedence Rules**

When multiple systems could provide conflicting information, the following **source precedence** applies:

| Data Type | Authoritative Source |
| :---- | :---- |
| Demographics, accession data | LIS (via HL7) |
| Authoring state, drafts | Portal authoring database |
| Slide digitization, QC | WSI/Imaging system |
| AI pipeline status | AI orchestration module |
| Alerts and rules | Orchestration rules engine |

## **2.7 Refresh and Freshness**

The read model is updated with the following freshness targets:

* Event-to-read-model latency: within 30 seconds of source event

* UI auto-refresh interval: configurable, default every 2 minutes

* Manual refresh: clicking the timestamp triggers immediate refresh

If a row's data may be stale (e.g., during system delays), the UI may display a subtle "updating" indicator rather than potentially incorrect information.

**PART III: PERMISSIONS AND PRIVACY**

# **3\. Permissions and Capability Model**

## **3.1 Authorization-Filtered Results**

The work list service **only returns cases the user is authorized to see** for the requested scope. Authorization filtering happens at the service layer, not the UI. The UI receives only permitted rows and renders them according to the user's capabilities.

This is a fundamental security principle: the work list never relies on UI-level hiding to enforce access control. If a case shouldn't be visible, it isn't returned from the service.

## **3.2 Capability Payload Per Row**

Each work list row includes a `capabilities` object that determines what the current user can see and do for that specific case:

### **Display Capabilities**

* `can_view_full_phi` — Whether full patient identifiers are visible

* `can_view_ai_status` — Whether AI pipeline indicators appear

* `can_view_annotations` — Whether annotation badges are shown

* `can_view_qc_status` — Whether QC warning indicators appear

### **Action Capabilities**

* `can_open_case` — Whether "Open Case" action is available

* `can_launch_viewer` — Whether "Launch Viewer" action appears

* `can_reassign` — Whether reassignment controls are shown

* `can_assign_to_self` — Whether "Assign to Me" action appears

The UI renders controls conditionally based on these capabilities. A user without `can_reassign` simply doesn't see the reassignment option—they're not presented with a disabled button or an error message.

## **3.3 Worklist Scope Selector**

Users access the work list through one of several scopes:

### **Default Scope: Self**

By default, users see **their own work list**—cases assigned to them, or cases in their designated service queues. This is the normal operating mode.

### **Delegated Scope**

Service directors and supervisors may have the `VIEW_SERVICE_WORKLISTS` capability, allowing them to view work lists for pathologists within their service line. This supports coverage planning and workload management.

### **Admin Scope**

Users with `VIEW_OTHERS_WORKLISTS` capability can select another user's work list directly. This is typically limited to department administrators and IT support personnel.

# **4\. Privacy Display Modes**

## **4.1 Mode Definitions**

The work list supports multiple privacy display modes that control how patient information is rendered. These modes affect *display only*—underlying access is still governed by permissions.

| Mode | Behavior |
| :---- | :---- |
| `CLINICAL_VIEW` | Full patient identifiers displayed per user's permissions |
| `TEACHING_VIEW` | Masked identity: initials, age (not DOB), MRN optionally hidden |

Future modes (`DEMO_VIEW`, `RESEARCH_VIEW`) are reserved but not implemented in Phase 1\.

## **4.2 Mode Controls**

**Who can switch modes:** Any user can switch to TEACHING\_VIEW at any time. Switching back to CLINICAL\_VIEW requires the user to have appropriate PHI access permissions.

**Institutional defaults:** Institutions may configure a default mode (e.g., TEACHING\_VIEW for conference room displays). Users with appropriate permissions can override to CLINICAL\_VIEW.

**Important:** Privacy mode masking is a display-layer convenience for teaching and demonstration contexts. It does not replace or modify the underlying permission model. Access logging and audit trails reflect actual data access, not displayed format.

# **5\. Break-Glass Access**

## **5.1 When Break-Glass Applies**

Break-glass access provides a mechanism for viewing another user's work list when the requesting user does not normally have authorization. This addresses legitimate operational needs (coverage for absent colleague, urgent patient safety situation) while maintaining accountability.

**Break-glass is not required** when: the user has `VIEW_OTHERS_WORKLISTS` or `VIEW_SERVICE_WORKLISTS` capabilities that already authorize the access.

## **5.2 Break-Glass Workflow**

The break-glass workflow requires:

1. **Explicit action** — User must select "Break-Glass Access" explicitly; it's not the default path

2. **Reason selection** — User selects from a predefined list of reason codes (e.g., "Coverage for absent colleague," "Patient safety concern," "Administrative review")

3. **Confirmation** — User confirms understanding that access will be logged and reviewed

4. **Time-bounded access** — Break-glass grants access for a limited duration (e.g., 8 hours)

## **5.3 Break-Glass Audit Record**

Every break-glass invocation generates an audit event containing:

* `requester` — Who initiated break-glass

* `target_user` — Whose work list was accessed

* `timestamp` — When access was granted

* `reason_code` — Selected justification

* `duration` — How long access was granted

* `actions_taken` — What the requester did during the session

The work list emits this event; it does **not** retain it or provide reporting. Audit events are exported to the institution's audit/compliance platform, which handles retention, alerting, and review workflows.

This design keeps the work list focused on its purpose—showing cases—while ensuring accountability through the institutional audit infrastructure.

**PART IV: DISPLAY AND INTERACTION**

# **6\. Display Columns**

## **6.1 Column Specification**

The work list displays the following columns, left to right:

| Column | Description | Source |
| :---- | :---- | :---- |
| Priority | Visual indicator for stat/urgent cases | LIS via HL7 |
| Accession Number | Clickable link to open case view | LIS via HL7 |
| Patient Name | Mode-dependent (full or masked) | LIS via HL7 |
| MRN | Medical record number (may be hidden) | LIS via HL7 |
| Pathology Service | Surgical path, derm, cytology, etc. | LIS via HL7 |
| Specimen Class | Biopsy, excision, resection, etc. | LIS via HL7 |
| Case Age | Days since accession, color-coded | Calculated |
| Case Status | Composite: LIS stage \+ authoring state | LIS \+ Internal |
| Assigned Pathologist | If institution uses assignment | LIS via HL7 |
| WSI Status | Digitized/total with verification indicator | Imaging API |
| Prior Cases | Badge indicating patient history exists | Database lookup |
| Enrichment Badges | AI, annotations, QC, alerts (see below) | Read model |

## **6.2 Enrichment Badges**

The rightmost area of each row displays a set of **enrichment badges**—compact visual indicators for supplementary information. These badges are capability-gated; users without the relevant view capability don't see them.

| Badge | Meaning | Capability Required |
| :---- | :---- | :---- |
| Annotations | Annotations present (with count) | `can_view_annotations` |
| AI Status | Queued / Running / Complete / Failed | `can_view_ai_status` |
| QC Status | Warning or error on slide quality | `can_view_qc_status` |
| Alert Level | Soft / Prompt / Hard-stop indicator | (always visible if present) |

## **6.3 Visual Indicators**

**Priority indicator:** Stat and urgent cases display a red icon at the left edge of the row. Visible but not overwhelming—it catches the eye without making the entire list feel like an emergency.

**Case age color coding:** Thresholds are configurable per pathology service. A typical configuration might show green for 0-2 days, yellow for 3-4 days, orange for 5-6 days, and red for 7+ days.

**WSI status:** Shows as "X/Y" where X is slides digitized and Y is total slides. When all slides are digitized *and* verified, displays with a green checkmark. If there are QC issues, displays with a warning indicator.

**Prior cases badge:** A small icon indicating the patient has previous cases. Does not show count to avoid clutter.

# **7\. Alerting Model**

## **7.1 Alert Classification**

Alerts are derived from the orchestration kernel's rules engine and stored in the `alert_summary` field. The work list displays these alerts but does not compute them. Alert levels follow this taxonomy:

| Level | Worklist Display | Enforcement |
| :---- | :---- | :---- |
| `NONE` | No indicator | None |
| `SOFT` | Badge or tooltip; non-blocking | Information only |
| `PROMPT` | Highlighted badge | Acknowledgement required on case open |
| `HARD_STOP` | Warning badge \+ row highlight | Blocks certain actions in case view |

## **7.2 Division of Responsibility**

The work list and case view/viewer have distinct responsibilities in the alerting model:

**Worklist responsibilities:** Display alert indicators (badges, row highlights). Allow users to see at a glance which cases have alerts. Provide hover text with alert summary.

**Case view/viewer responsibilities:** Enforce acknowledgements (PROMPT level). Block actions when required (HARD\_STOP level). Record acknowledgement events. Display full alert details and required actions.

This division keeps the work list fast and focused on navigation while ensuring that enforcement happens where it matters—when the user is actually working on the case.

# **8\. Multi-Author Workflow Integration**

## **8.1 The Collaboration Challenge**

In academic medical centers, residents draft reports that attending physicians must review and finalize. The work list must clearly communicate where each case stands in this workflow.

## **8.2 Status Display for Multi-Author Cases**

The Case Status column shows composite information combining LIS processing status with internal authoring state:

| Status Display | Meaning |
| :---- | :---- |
| Ready for sign-out | Case is ready, no draft started |
| Draft in progress | Someone is actively working on the report |
| **Pending review** | Draft completed, awaiting attending sign-off |
| Amendment pending | Signed case has a pending amendment |

## **8.3 Hover Details for Collaboration Context**

When the status indicates multi-author involvement, hovering over the status cell reveals additional context:

* **"Draft in progress"** hover shows: "Being edited by Dr. Sharma"

* **"Pending review"** hover shows: "Completed by Dr. Sharma on Jan 14, awaiting attending review"

* **"Amendment pending"** hover shows: "Amendment initiated by Dr. Chen"

This hover behavior provides context without cluttering the primary display. A quick mouse-over answers the question "who's involved and what are they waiting on?"

## **8.4 Internal Notifications**

When a resident marks their draft as complete and ready for review, the system sends an internal notification to the assigned attending. This notification appears in the attending's work list with the case highlighted in the "Pending review" state.

Similarly, when two pathologists collaborate on a complex case, completing one's portion triggers a notification to the other. The work list becomes the central place where these handoffs are surfaced.

# **9\. Reassignment and Takeover**

## **9.1 Reassignment Actions**

The work list supports case reassignment for users with appropriate capabilities:

* **Assign to self** — Take ownership of an unassigned or pool case

* **Reassign to another** — Transfer a case to a specific pathologist

* **Return to pool** — Release assignment, returning case to service queue

All reassignment actions are **capability-gated**. The controls only appear for users with `can_reassign` or `can_assign_to_self` capabilities for that specific case.

## **9.2 Takeover for Coverage**

When a pathologist is unexpectedly absent, authorized users (typically service directors or administrators) may need to reassign their entire queue. This "takeover" workflow:

* Requires explicit capability (e.g., `can_takeover_queue`)

* May require selection of reason/comment

* Generates audit events for each reassignment

* Updates the read model so reassigned cases appear on new assignee's list

## **9.3 Safety Constraints**

Reassignment from the work list does **not** constitute sign-out. To finalize a report, the pathologist must open the case and complete the sign-out workflow in the authoring module. The work list facilitates workflow routing; it does not perform clinical actions.

# **10\. Filtering and Search**

## **10.1 Filter Panel**

The work list provides a filter panel with the following options:

* **Pathology service** — Multi-select dropdown

* **Assigned pathologist** — If assignment data is available

* **Case age threshold** — "Show cases older than X days"

* **Case status** — Filter by processing/authoring state

* **Specimen class** — For batching similar case types

* **Alert level** — Show only cases with alerts at or above a threshold

## **10.2 Smart Filters**

Pre-built "smart filters" combine multiple criteria to match actual workflow states:

**"Sign-out ready"** — Shows cases where: processing is complete, all slides are digitized and verified, no pending ancillary studies, and (if the user is an attending) includes cases with drafts awaiting their review.

**"My cases"** — Shows cases assigned to the current user or, if using subspecialty queues, cases in the user's designated services.

**"Needs attention"** — Shows cases exceeding their service-specific age thresholds or cases with PROMPT or HARD\_STOP alerts.

## **10.3 Search Functionality**

The search box accepts accession numbers, patient MRNs, or patient names. Search behavior differs from filtering:

* Search results can include **signed-out cases** that would normally not appear

* The work list displays matching results, allowing visual verification before opening

* If exactly one case matches, pressing Enter again navigates directly to the case view

This search-then-verify pattern supports keyboard-driven workflows. A pathologist on the phone with a clinician can type an accession number, confirm it's the right case at a glance, and open it with a keystroke.

# **11\. Sorting and User Preferences**

## **11.1 Sortable Columns**

All columns are sortable. Clicking a column header sorts the list by that column; clicking again reverses the sort order. A visual indicator shows the current sort column and direction.

## **11.2 Preference Persistence**

The system remembers each user's last-used sort order, filters, and column visibility preferences. When a pathologist logs in, they see their work list configured the way they left it.

# **12\. Refresh and Performance**

## **12.1 Automatic Refresh**

The work list refreshes automatically at a configurable interval (default: every two minutes). The timestamp in the header shows when the list was last updated. Clicking the timestamp triggers an immediate manual refresh.

## **12.2 Asynchronous Data Loading**

To ensure responsive page load, data is loaded in priority order:

5. **Core data first** — The table renders immediately with case information from the read model

6. **Enrichment fields** — AI status, annotations, QC badges populate as available

7. **Prior case lookup** — Patient history indicator loads asynchronously

Users see a usable work list almost immediately. Enrichment columns may show loading indicators briefly before data appears. This pattern ensures the imaging system or AI pipeline cannot block page load.

## **12.3 Large List Handling**

For institutions with high case volumes (200+ cases), the system uses pagination or virtual scrolling to maintain performance. Typical workloads under 100 cases render instantly.

**PART V: EVENT INTERFACE**

# **13\. Outbound Events**

## **13.1 Events Emitted by Worklist**

The work list emits the following events to the orchestration kernel. The work list **emits but does not retain** these events—storage and processing happen in downstream systems.

| Event | When Emitted |
| :---- | :---- |
| `case_open_requested` | User clicks to open a case |
| `viewer_launch_requested` | User requests to launch digital viewer |
| `case_reassigned` | Case assignment changed |
| `assign_to_self` | User takes ownership of a case |
| `break_glass_invoked` | User initiates break-glass access |
| `privacy_mode_changed` | User switches display mode (optional) |

## **13.2 Event Payload Constraints**

Work list events are **case-level workflow events**. They capture what action was taken on which case by whom.

Explicitly **forbidden** in work list event payloads:

* Tile or viewport navigation telemetry

* Scroll position or dwell time

* Mouse movement or click coordinates within the list

This constraint ensures work list events remain focused on workflow transitions and do not become a source of discoverable behavioral telemetry. Navigation-level events, if captured at all, belong to the viewer module under its own governance model.

# **14\. Case Lifecycle on the Work List**

## **14.1 When Cases Appear**

A case appears on the work list when it is accessioned in the LIS and reaches a processing stage indicating it's relevant for pathologist attention (configurable per institution).

## **14.2 When Cases Disappear**

Signed-out cases are **removed from the default work list view**. The work list functions as a to-do list—once work is complete, the case no longer demands attention. To find a signed-out case, pathologists use the search function.

## **14.3 When Cases Reappear**

An **amended case** returns to the work list with status "Amendment pending" and a visual indicator distinguishing it from new cases. If something needs a pathologist's attention, it belongs on the work list.

# **15\. Summary: The Work List Promise**

The work list makes one promise to pathologists: **if something needs your attention, you'll find it here.** The design decisions flow from this commitment:

* Signed cases disappear—the list shows work to be done, not history

* Amendments reappear—if it needs attention again, it belongs on the list

* Multi-author handoffs surface clearly—residents and attendings see the same truth

* Smart filters match real workflows—not just column values, but workflow states

* Search finds anything—when you need a specific case, you can get to it

* Permissions are enforced at the service layer—the UI renders what you're allowed to see

* Break-glass provides accountability—emergency access is possible but audited

* Events are emitted, not retained—the work list stays focused on navigation

This is the front door to the Pathology Portal. If the work list is frustrating, pathologists won't make it to the authoring module. Get this right.