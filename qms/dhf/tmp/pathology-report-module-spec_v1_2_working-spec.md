# Pathology Report Authoring Module — Working Specification (Phase 1)

**Document Version:** 1.2  
**Date:** January 2026  
**Status:** Working Specification for Phase 1 Implementation  
**Upstream Dependencies:** Okapi orchestration kernel, Authorization/Identity service, HL7/FHIR Gateway, Document Repository APIs (see §2)

---

## 0. Executive Summary

This module provides a **case-scoped diagnostic report authoring workspace** within Pathology Portal. It enables a pathologist to draft and review a diagnostic report, apply controlled AI-assisted conveniences (transcription, structuring, nomenclature harmonization), and transmit a **single finalized outbound report** to the LIS for official sign-out.

**Boundary conditions (non-negotiable):**
- The **LIS remains the system of record** for the final report and the formal amendment workflow.
- This module is a **clinical authoring and QA workspace**, not a clerical system (billing/coding/sign-out remain in LIS).
- **Permissions are defined externally** (Authorization app); this module enforces them at every state transition and edit action.
- **Single-editor concurrency is enforced** (one active editor session per case report at any time), with an auditable force-takeover mechanism.
- **If a case is signed out in the LIS**, it is removed from this module's active scope (draft becomes inaccessible/archived).

---

## 1. Scope

### 1.1 In Scope (Phase 1)

- Open a case report authoring session from worklist/search ("landing" into report workspace).
- Display the report structure with parts as received from LIS (A/B/C…).
- Create/edit report text via keyboard, optional transcription, and/or **voice editing commands** with LLM assistance.
- Enforce **part assignment safeguards**, including hard-stop on low-confidence placement.
- Maintain **session persistence** with immediate autosave and recovery after interruption.
- Support multi-author drafting (resident/fellow/attending), attribution, and state transitions.
- Provide **read-only access to peripheral clinical documents** (op note, radiology, endoscopy, prior path, etc.) retrieved asynchronously via internal APIs.
- **Nomenclature harmonization** with personal dictionary, institutional fallback, and arbitration routing.
- Log access and actions (viewed documents, edits, break-glass, force takeover, finalize attempts).
- Finalize a report and hand it off to the HL7/FHIR gateway for outbound transmission to LIS.
- Handle transmission failure by queueing and surfacing user-visible status.

### 1.2 Explicitly Out of Scope (Phase 1)

- UI/UX layout and interaction design details beyond the behavioral constraints defined here.
- CAP synoptic templates, structured cancer checklists.
- Inline educational commenting/mentoring workflows.
- Automated generation of clinical decision support or diagnostic suggestions.
- Full amendment authoring within this module (see §10: limited "initiate amendment" option only).
- System-wide configuration interfaces (session timeout configuration is a system-level concern).
- Data quality feedback interfaces for malformed case data (exists elsewhere in Pathology Portal).

---

## 2. System Context and Dependencies

### 2.1 External Systems (Authoritative Responsibilities)

- **Authorization/Identity Service (external):**
  - Defines roles/permissions and break-glass eligibility.
  - Issues identity claims/tokens used by this module.
- **HL7/FHIR Gateway (external):**
  - Handles inbound/outbound HL7/FHIR message transport, retries, acknowledgements.
  - This module produces/consumes *payloads* and status callbacks; it does not implement HL7 stacks.
- **LIS (system of record):**
  - Official report record, signature, distribution, billing/coding, formal amendments/addenda.
  - **Cases signed out in LIS are removed from this module's active scope.**
- **Document Repository / Clinical Systems (sources):**
  - Operative notes, radiology, endoscopy, outside consult PDFs, requisition images, etc.
  - Accessed asynchronously through internal APIs (which may be backed by FHIR).

### 2.2 Data Sources Used by This Module

- Case header and part list (from LIS/Okapi datastore, originally HL7-fed).
- Case workflow state (Okapi event/state store).
- Peripheral documents and summaries (via internal APIs, asynchronous).
- Nomenclature dictionary (module-owned datastore).

### 2.3 "Circles on Water" Principle (Behavioral Constraint)

The report content is the primary artifact. Peripheral documents are **accessory context** and must be accessed without derailing authoring: async fetch, lightweight preview capabilities, and return-to-report continuity are required, but specific UI widgets are not mandated in Phase 1.

---

## 3. Core Concepts and Definitions

| Term | Definition |
|------|------------|
| **Report Session** | A time-bounded editing context for a case report. |
| **State Machine** | Report lifecycle states with permission-gated transitions (§6). |
| **Single Editor Lock** | Only one active editor session at a time (§5). |
| **Autosave** | Immediate persistence of edits to server (§7). |
| **Finalize** | A module action that locks the report and triggers outbound transmission; distinct from LIS sign-out (§9). |
| **Break-Glass** | Emergency override of access restrictions by privileged users, with immediate logging and notification (§11). |
| **Living Dictionary** | The nomenclature mapping system that learns from pathologist corrections (§8.4). |

---

## 4. Entry Points and Navigation

### 4.1 Entry Points

- From worklist: open report for a case selected by user.
- From search: open report by accession number / case identifier.

### 4.2 Landing Requirements

On entry, the module must:
1. Authenticate user session and retrieve identity claims.
2. Validate authorization to **view** the case report.
3. **Verify case is not already signed out in LIS** (if signed out, display read-only archived state or redirect).
4. Load case data and report draft (if any) from the module datastore.
5. Start async retrieval of peripheral documents via internal APIs.
6. Acquire **editing lock** only if user requests edit mode (or if UI defaults to edit mode); otherwise open read-only.

---

## 5. Concurrency and Editing Lock

### 5.1 Single-Editor Rule

At any time, a case report may have **at most one active editor on one screen**. Other users may open the report in **read-only** mode.

**Definition of "active editor":**
- An authenticated user holding an edit lock for the case report, with a heartbeat (renewal) interval.

### 5.2 Lock Lifecycle

- **Lock acquisition:** On entering edit mode.
- **Lock renewal:** Periodic heartbeat.
- **Lock release:** Explicit "exit edit mode" OR session end OR timeout.

### 5.3 Immediate Save Design

The system saves edits immediately upon entry. There is no "unsaved changes" state under normal operation. This eliminates most versioning conflict scenarios.

### 5.4 Takeover Request Mechanism

When a user requests to edit a report that is currently locked by another user:

1. **Request initiated:** System sends a takeover request to the current editor.
2. **Current editor notified:** "[User name] is requesting to take control of this report."
3. **Current editor responds:**
   - **Approve:** Lock is transferred; current editor moves to read-only.
   - **Reject:** Request is denied; requesting user is notified.
4. **Timeout:** If no response within a reasonable period (e.g., 60 seconds), the request may be treated as rejected, or escalated per policy.

Both request and response are logged as audit events.

### 5.5 Force Takeover (Administrative or Privileged Action)

A user with the appropriate permission (e.g., service director, clinical system administrator) may force takeover without approval. The module must:
- Display a warning that another active session will be force-closed.
- Record an audit event including: prior editor identity, takeover identity, timestamp, reason (required).
- Immediately revoke prior editor's ability to write further changes.
- Preserve last saved content as the authoritative draft state.

### 5.6 Session Timeout

If no heartbeat or edit activity is received for the configured timeout period (default: **30 minutes**), the edit lock is automatically released and the session transitions to read-only.

The user should be warned before timeout (e.g., at 25 minutes) and given opportunity to extend.

**Note:** Timeout duration is a system-wide configuration parameter, not module-specific. Configuration interface is out of scope for this module.

### 5.7 Switching Devices (Same User)

If the same user opens the report on a second device:
- The system must warn that an active session exists elsewhere.
- User may request transfer of the edit lock to the new device.
- Since saves are immediate, no data loss occurs; the lock simply moves.

---

## 6. State Machine and Permissions

### 6.1 States

| State | Description |
|-------|-------------|
| **Draft** | Report is editable (subject to permission). |
| **Review** | Report is editable, indicates another user has reviewed/commented. Optional state. |
| **Finalized** | Report is locked in this module; outbound transmission requested/completed. |

> "Empty" is an implicit condition: no draft exists yet.

### 6.2 State Transitions (Permission-Gated)

All transitions require authorization checks via the external Authorization service.

| From | To | Intended Actor(s) | Required Permission |
|------|-----|-------------------|---------------------|
| (none) | Draft | Any authorized author | REPORT_CREATE |
| Draft | Review | Any authorized editor (optional transition) | REPORT_EDIT |
| Review | Draft | Any authorized editor (if reverting) | REPORT_EDIT |
| Draft | Finalized | Attending pathologist (by default) | REPORT_FINALIZE |
| Review | Finalized | Attending pathologist (by default) | REPORT_FINALIZE |

### 6.3 Role-Based Permission Defaults

| Role | REPORT_CREATE | REPORT_EDIT | REPORT_FINALIZE |
|------|---------------|-------------|-----------------|
| Resident | ✓ | ✓ | — |
| Fellow | ✓ | ✓ | — |
| Attending | ✓ | ✓ | ✓ |
| Service Director | ✓ | ✓ | ✓ |

**Notes:**
- REPORT_FINALIZE is **not granted by default** to residents and fellows, but **can be granted** by policy/exception.
- The Review state is **not mandatory**. A user with REPORT_FINALIZE permission can transition directly from Draft to Finalized.
- The Review state is useful when someone other than the original author has reviewed the report and wishes to indicate that review occurred.

### 6.4 Abandoned Drafts

If a draft is never finalized:
- It remains on the worklist and must be addressed.
- Service director or clinical system administrator may finalize or mark as administratively resolved.
- If the case is signed out directly in the LIS (bypassing this module), the draft becomes inaccessible/archived and is removed from active worklist.

---

## 7. Persistence, Autosave, and Retention

### 7.1 What This Module Persists

The module persists all activity required to support:
- Session recovery (draft text and structure).
- Multi-author attribution and edit timeline.
- Operational QA, training, and quality improvement efforts.
- Secure access monitoring (who accessed what).

Persisted categories:
1. **Report drafts** (structured representation + rendered text).
2. **Edit events** (who/when/what changed; at minimum document-level versioning).
3. **Attribution** (created_by, last_modified_by, finalized_by, etc.).
4. **Peripheral document access logs** (view events, sources).
5. **Break-glass and force-takeover events** (with required reasons).
6. **Outbound transmission queue entries** and delivery status.
7. **Nomenclature dictionary** (mappings, corrections, attributions, conflicts).

### 7.2 Immediate Save Design

- **Edits are saved immediately** to the server upon entry. There is no client-side "dirty" state under normal operation.
- Brief network interruptions may be buffered locally, but server persistence must occur as soon as connectivity resumes.
- On re-entry after interruption, the user resumes from the last saved state.

### 7.3 Retention and "System of Record" Clarification

- The LIS remains the **system of record** for the final signed report and formal amendments.
- This module's stored drafts and logs are maintained for operational and QA needs; they must be secured, access-controlled, and retention-governed per institutional policy.
- This specification does **not** attempt to define legal discoverability outcomes; it defines the intended clinical/operational purpose and access controls.

---

## 8. Report Content Model and Authoring Assistance

### 8.1 Internal Structure

The report must be represented internally as a structured object sufficient to:
- Address parts explicitly (A/B/C…).
- Support targeted edits (e.g., "modify Part C diagnosis").
- Render to LIS-compatible formatted text.

Illustrative structure (non-prescriptive):
```json
{
  "case_id": "S25-12345",
  "state": "DRAFT",
  "parts": [
    {
      "designation": "A",
      "original_label": "skin lesion left arm",
      "standardized_label": "Skin, left arm, excision",
      "diagnosis_text": "Compound nevus, benign"
    }
  ],
  "audit": {
    "created_by": "user123",
    "created_at": "2026-01-20T14:00:00Z",
    "last_modified_by": "user123",
    "last_modified_at": "2026-01-20T14:12:10Z"
  }
}
```

### 8.2 Input Modes

The module must support:
- **Manual typing/editing** at all times (baseline capability).
- **Dictation/transcription** integration (Whisper or equivalent) when available.
- **Voice editing commands** with LLM interpretation (see §8.3).
- **LLM-based structuring, formatting, and rewrite assistance** when available.

### 8.3 Voice Editing Commands (Phase 1 Critical Feature)

The module must support voice-based editing commands, not just dictation of content. The pathologist speaks instructions, and the system interprets and executes them.

**Examples of supported commands:**

| Voice Input | System Action |
|-------------|---------------|
| "Modify the diagnosis for Part C. Instead of fibroepithelial polyp, say acrochordon." | Locate Part C diagnosis field; replace content as instructed. |
| "Delete the last sentence." | Identify the last sentence in current context; remove it. |
| "In Part A, remove the word 'benign' before 'compound nevus'." | Locate specific text in Part A; perform targeted deletion. |
| "Add a comment: clinical correlation recommended." | Append the specified text to the current section or a comments field. |
| "Move Part C above Part B." | Reorder parts as instructed. |
| "Change all instances of 'polyp' to 'lesion'." | Find and replace within the report. |

**Implementation requirements:**
- Voice commands are transcribed and sent to the LLM for interpretation.
- The LLM determines intent and target (which part, which field, what action).
- The system executes the interpreted action and displays the result.
- If interpretation confidence is low, the system must request clarification before executing.
- All voice-initiated edits are saved immediately, same as manual edits.

### 8.4 Nomenclature Harmonization (Living Dictionary)

When a specimen label is received from the LIS, the module may suggest a standardized term.

#### Lookup Priority

1. **Current user's personal corrections** for this input term (highest priority).
2. **Frequency-weighted institutional corrections** from other pathologists.
3. **Probabilistic inference** (LLM-based) for novel terms.

#### Correction Handling

- If a user overrides a suggested mapping, the correction is stored **with user attribution**.
- The correction becomes part of that user's personal dictionary.
- If the correction conflicts with another user's prior correction for the same input term, a **conflict event** is emitted for routing to the arbitration system.

#### Conflict and Arbitration

- A conflict occurs when two or more pathologists have mapped the same input term to different standardized terms.
- Conflicts are routed to a **separate arbitration system** (out of scope for this module).
- Until arbitration resolves the conflict, each user sees mappings based on their own corrections or frequency-based fallback.

#### Initial Dictionary State

- The dictionary **starts empty**. No pre-seeding.
- If pre-seeding becomes necessary, it can be accomplished via administrative scripts (out of scope for this specification).

#### Display Behavior

Standardized terms are displayed with the original label preserved:

```
Part C: Acrochordon, skin (received as "skin tag thing")
```

A visual indicator (e.g., icon or subtle highlight) should signal that a transformation occurred. The user can click/tap to view details or override the mapping.

### 8.5 Graceful Degradation

If advanced services degrade (LLM/transcription unavailable):
- The module must **continue to function** for manual typing/editing.
- The module must clearly indicate that advanced features (voice commands, nomenclature suggestions) are unavailable, without blocking authoring.
- Previously saved dictionary mappings remain available for lookup.

### 8.6 Part Assignment and Clarification Safeguards

The module must place dictated/assisted content into the correct part.

**Hard stop rule:** If part assignment confidence is below the configured threshold (default 0.90), authoring assistance must request clarification before committing placement.

**Soft flag rule:** Ambiguities above the hard stop but below a "comfortable" threshold may be highlighted for later review without blocking.

**Confidence computation** is implementation-defined but must be:
- Deterministic and testable.
- Composed from relevant signals (e.g., transcription certainty + parser certainty).
- **Transient only**: confidence values must not be transmitted to LIS and must not persist as part of the finalized report payload.

---

## 9. Finalization and Outbound Transmission

### 9.1 Finalize Action

Finalize is a permission-gated action (REPORT_FINALIZE required) that:
1. Locks the report against further edits within this module.
2. Strips all transient metadata (confidence scores, intermediate states).
3. Renders the report into LIS-compatible formatted output (RTF or format per institutional LIS requirements).
4. Submits an outbound transmission request to the HL7/FHIR gateway.
5. Records a finalize event, including identity, timestamp, and report version hash.

### 9.2 Transmission Queue and Status

Because the gateway/LIS may be unavailable intermittently:
- Finalization must create a queue entry with idempotency token.
- Delivery status must be visible to authorized users (e.g., "queued", "sent", "acknowledged", "failed").
- Retries are performed by the gateway; this module consumes status callbacks.

### 9.3 Idempotency

The module must generate and persist an idempotency key for each finalize attempt so that retries do not create duplicate finalized reports in the LIS.

### 9.4 Post-Finalization in LIS

Once the LIS acknowledges receipt and signs out the case:
- The case is removed from this module's active worklist.
- The draft and associated data may be archived per retention policy.
- The module displays the case as "Signed out in LIS" if accessed.

---

## 10. Amendments and Post-Finalization (Phase 1 + Optional Hook)

### 10.1 Default Phase 1 Behavior

- Amendments/addenda are performed in the LIS (system of record).
- This module does not author amendments in Phase 1.

### 10.2 Optional "Initiate Amendment" Hook (Non-blocking)

If enabled by policy, the module may provide a function to:
- Create a new Draft session marked "amending" (internal state: AMEND_IN_PROGRESS).
- Require explicit confirmation: "Proceed with amending this case?"
- Record an audit event.
- Hand off to LIS workflow or prepare a new draft intended for LIS amendment entry.

This hook must not bypass LIS amendment governance and must remain permission-gated.

---

## 11. Peripheral Document Retrieval and Access Logging

### 11.1 Asynchronous Retrieval

Peripheral documents are fetched asynchronously through internal backend APIs. The module must:
- Avoid blocking report authoring on document retrieval.
- Provide progressive availability (documents appear when fetched).
- Handle partial failures gracefully (display error for unavailable documents without affecting others).

### 11.2 Access Logging

Every document view must create a log event capturing:
- User identity, case ID, document type/source, timestamp, access method (preview vs. full open).
- Break-glass status if applicable.

---

## 12. Break-Glass (Emergency Override)

### 12.1 Capability

Privileged users may invoke break-glass to override access restrictions when clinically necessary.

### 12.2 Requirements

- Break-glass must require explicit action and a reason.
- Break-glass events must be logged immediately and surfaced to the monitoring/reporting system.
- Break-glass permission is restricted to a defined set of roles (configured in Authorization service).

---

## 13. Audit and Monitoring Events (Minimum Event Set)

The module must emit audit events for:

| Event | Description |
|-------|-------------|
| `report_opened` | User opened report (read-only or edit mode) |
| `edit_lock_acquired` | User acquired edit lock |
| `edit_lock_released` | User released edit lock (explicit or timeout) |
| `takeover_requested` | User requested to take over edit lock |
| `takeover_approved` | Current editor approved takeover |
| `takeover_rejected` | Current editor rejected takeover |
| `force_takeover_completed` | Privileged user forced takeover |
| `content_saved` | Report content saved (may be batched/sampled for high-frequency edits) |
| `state_transition` | Report state changed (Draft → Review → Finalized) |
| `finalize_requested` | User initiated finalization |
| `finalize_queued` | Report queued for outbound transmission |
| `finalize_delivery_status` | Transmission status updated (sent/acked/failed) |
| `document_viewed` | User viewed peripheral document |
| `break_glass_invoked` | User invoked break-glass access |
| `dictionary_correction` | User corrected nomenclature mapping |
| `dictionary_conflict` | Nomenclature conflict detected, routed to arbitration |

Events must include: case ID, user ID, timestamp, session ID, and relevant metadata.

---

## 14. API Contracts (Phase 1 Minimal Surface)

> The exact transport (REST/gRPC) and versioning mechanism follow industry standards. Below is the minimum logical contract set.

### 14.1 Report Session

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/cases/{caseId}/report` | GET | Retrieve current report state + metadata |
| `/cases/{caseId}/report/lock` | POST | Request edit lock |
| `/cases/{caseId}/report/lock` | DELETE | Release edit lock |
| `/cases/{caseId}/report/takeover-request` | POST | Request takeover from current editor |
| `/cases/{caseId}/report/takeover-response` | POST | Respond to takeover request (approve/reject) |
| `/cases/{caseId}/report/force-takeover` | POST | Force takeover (requires permission + reason) |
| `/cases/{caseId}/report` | PATCH | Submit edits (immediate save) |
| `/cases/{caseId}/report/state` | POST | Transition state (permission-gated) |
| `/cases/{caseId}/report/finalize` | POST | Request finalization and enqueue transmission |

### 14.2 Peripheral Documents

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/cases/{caseId}/documents` | GET | List available documents and fetch status |
| `/cases/{caseId}/documents/{docId}` | GET | Retrieve document payload/stream |
| `/cases/{caseId}/documents/{docId}/viewed` | POST | Record view event (if not implicit) |

### 14.3 Dictionary / Nomenclature Harmonization

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/dictionary/lookup` | GET | Retrieve mapping candidates (query param: `term`, scoped to user + institution) |
| `/dictionary/correction` | POST | Submit correction with attribution |
| `/dictionary/conflict` | POST | Route conflict to arbitration system (or emit event) |

---

## 15. Non-Functional Requirements

### 15.1 Availability and Degradation

- Manual typing/editing must remain available if AI services fail.
- Autosave and lock management must continue with transient connectivity loss.
- Nomenclature lookup from cached/saved dictionary must function if LLM is unavailable.

### 15.2 Performance Targets

Performance targets follow industry standards for clinical applications:

| Operation | Target (95th percentile) |
|-----------|--------------------------|
| Report open (cached case + existing draft) | < 2 seconds |
| Lock acquire/release | < 500 ms |
| Edit save (immediate) | < 500 ms |
| Peripheral document list | < 2 seconds initial response |
| Voice command interpretation | < 3 seconds |

### 15.3 Security

- All endpoints require authenticated identity tokens.
- Authorization checks enforced per action.
- Audit logs are tamper-evident and access-restricted.
- PHI handling follows institutional policy; no client-side caching beyond session necessity.

---

## 16. Acceptance Criteria (Phase 1)

1. A user with REPORT_CREATE can create a draft; edits save immediately; session resumes after interruption.
2. Only one editor can edit at a time; a second user sees read-only with option to request takeover.
3. Takeover request notifies current editor; current editor can approve or reject.
4. Force takeover works only with permission; prior editor is blocked; audit logged with reason.
5. Draft → Finalized transition requires REPORT_FINALIZE permission (attending by default).
6. Finalize enqueues outbound transmission with idempotency key; delivery status is visible.
7. Cases signed out in LIS are removed from active worklist.
8. Peripheral documents are fetched asynchronously; viewing is logged.
9. Break-glass is permission-gated, requires a reason, and is reported/logged.
10. If LLM/transcription is unavailable, manual typing remains fully functional.
11. Voice editing commands are interpreted and executed; low-confidence interpretations request clarification.
12. Nomenclature harmonization uses personal dictionary first, then frequency-based institutional lookup.
13. Nomenclature conflicts between users are detected and routed to arbitration.

---

## 17. Phase 2 Considerations (For Awareness)

The following are explicitly deferred to Phase 2:

| Feature | Notes |
|---------|-------|
| CAP synoptic templates | Requires separate detailed specification |
| Mentor/institutional pattern retrieval | Policy complexity; cold-start feature |
| Inline educational commenting | Attending → resident feedback |
| Pull excerpts from peripheral docs | Integration complexity |
| Voice command for finalization | Button sufficient for Phase 1 |
| Diff view for takeover conflicts | Show prior editor's unsaved state |
| Pre-seeding dictionary via UI | Scripts available if needed |

---

## 18. Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Jan 2026 | Initial narrative specification |
| 1.1 | Jan 2026 | Converted to working build specification: explicit state machine, single-editor lock + takeover, autosave/persistence/retention posture, integration boundaries, break-glass, minimal API contracts, acceptance criteria |
| 1.2 | Jan 2026 | Added: voice editing commands (§8.3), nomenclature harmonization behavior (§8.4), takeover request/response mechanism (§5.4), role-based permission defaults (§6.3), abandoned draft handling (§6.4), session timeout (§5.6), LIS sign-out integration (§9.4), immediate save design (§5.3, §7.2), performance targets as 95th percentile, expanded audit events (§13), acceptance criteria updates |

---

*This document serves as the working specification for Phase 1 implementation. It will be formalized into User Needs statements and Design Inputs as the project progresses through the QMS process.*
