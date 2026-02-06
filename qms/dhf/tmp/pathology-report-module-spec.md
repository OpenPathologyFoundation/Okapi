# Pathology Report Authoring Module — Phase 1 Narrative Specification

**Document Version:** 1.0  
**Date:** January 2026  
**Status:** Working Draft for Development Team

---

## 1. Introduction and Vision

### The Problem We're Solving

Pathologists spend a disproportionate amount of their day on the mechanical aspects of report creation—dictating, formatting, correcting transcription errors, wrestling with inconsistent specimen nomenclature, and navigating between multiple systems. This time could be better spent on what pathologists are trained to do: diagnostic interpretation.

The development team has a rare opportunity to create something that fundamentally improves pathologists' working lives. Not by replacing their judgment, but by removing friction from the process of translating their diagnostic thinking into a finalized report.

### What This Module Is

This is the **diagnostic report authoring module** within the broader Pathology Portal system. Think of it as the "sign-out cockpit"—the focused workspace where a pathologist takes a case that's ready for diagnosis and produces a finalized report.

The module receives case information from the Laboratory Information System (LIS) via HL7 interface. It provides a voice-driven, intelligent authoring environment. When the pathologist finalizes the report, it returns to the LIS for official sign-out, coding, and distribution.

### What This Module Is Not

This module does not handle:

- **Worklist management** — Case selection and queue management happen elsewhere in Pathology Portal
- **Gross description authoring** — That's a separate workflow
- **Specimen tracking and accessioning** — Handled by the LIS
- **Official sign-out** — The LIS remains the system of record
- **Billing and coding** — Clerical functions remain in the LIS
- **Image management** — Images are accessible but managed elsewhere
- **Clinical decision support** — This system authors reports; it doesn't make diagnostic suggestions

### The "Circles on Water" Metaphor

The interface design follows a "circles on water" principle. At the center—in sharp focus—is the report being authored. This is where the pathologist's attention lives. On the periphery, like ripples extending outward, is contextual information: operative notes, radiology reports, prior pathology, clinical history. These are accessible via modal popups (mouseover to preview, click to open) but never intrude on the central authoring experience.

---

## 2. System Architecture Context

### Integration Points

```
┌─────────────────────────────────────────────────────────────┐
│                    PATHOLOGY PORTAL                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Worklist   │  │   Digital   │  │   Other     │         │
│  │   Module    │  │   Slides    │  │  Modules    │         │
│  └──────┬──────┘  └─────────────┘  └─────────────┘         │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │         REPORT AUTHORING MODULE                 │       │
│  │              (This Specification)               │       │
│  └─────────────────────────────────────────────────┘       │
│         │                                                   │
└─────────┼───────────────────────────────────────────────────┘
          │
          ▼ HL7
┌─────────────────────┐
│         LIS         │
│  (System of Record) │
└─────────────────────┘
```

### Data Flow

1. **Inbound (from LIS via HL7):**
   - Case accession number
   - Patient demographics
   - Specimen/part information (labels as received from OR, part designations A/B/C/D)
   - Block and slide information
   - Clinical history
   - Ordering physician information

2. **Accessible (via Pathology Portal):**
   - Operative notes
   - Radiology reports
   - Endoscopy notes
   - Prior pathology reports
   - Requisition form image
   - Gross images
   - Digital slide images (in external viewer)
   - Outside consultation reports

3. **Outbound (to LIS):**
   - Finalized diagnostic report (formatted text with RTF tags)
   - Author attribution (who created, who finalized)
   - Timestamps

### Technical Infrastructure

- **Transcription:** Whisper (OpenAI) or equivalent, deployed via Microsoft Azure AI Studio (HIPAA compliant)
- **Language Model:** Azure AI Studio for interpretation, paraphrasing, and intelligent assistance
- **Concurrent Users:** Up to 40 simultaneous users
- **Performance:** Standard cloud latency expectations; no special requirements beyond current Azure AI Studio capabilities

---

## 3. Core Workflow

### Entry Point

A pathologist arrives at this module by either:

1. Clicking a case from the worklist module, or
2. Searching for a specific case by accession number

Upon entry, the module loads all available case information and presents the authoring interface.

### The Authoring Session

#### Initial State

The screen presents:

- **Center:** The report canvas, pre-populated with part structure (Part A, Part B, etc.) as received from LIS
- **Periphery:** Icons/indicators for available contextual documents (op notes, radiology, etc.)
- **Status indicators:** Draft state, author information, timestamps

The part labels appear exactly as received from the OR/accessioning. For example:

```
Part A: "skin lesion left arm"
Part B: "skin lesion right arm"  
Part C: "the thing from the back"
```

#### Voice-Driven Authoring

The pathologist speaks naturally. The system:

1. **Transcribes** speech to text via Whisper
2. **Interprets** the transcription to understand intent and structure
3. **Populates** the appropriate fields in the report

Example dictation flow:

> "Part A. Sections show skin with a benign compound nevus. No evidence of malignancy. Part B. Sections show skin with a seborrheic keratosis. Part C. Sections show fibroepithelial polyp."

The system parses this into structured data, assigning each diagnosis to the correct part.

#### Part Assignment Logic

When the pathologist mentions a part (explicitly or implicitly), the system must determine where to place the dictated content. 

**Critical rule:** If confidence in part assignment falls below 90%, the system issues a **hard stop** and asks for clarification.

Example clarification prompt:
> "I heard a diagnosis but I'm not certain which part it belongs to. Are you describing Part B (skin lesion right arm) or Part C (the thing from the back)?"

The pathologist can dictate parts in any order. The structure is already established by the LIS; the system simply needs to know where to slot each piece of content.

#### Nomenclature Standardization

This is a key feature. When the pathologist dictates a diagnosis, the system:

1. **Displays** the diagnosis using standardized nomenclature
2. **Preserves** the original OR label in parentheses
3. **Learns** from corrections over time

Example transformation:

| OR Label | Dictated As | Displayed As |
|----------|-------------|--------------|
| "the thing from the back" | "fibroepithelial polyp" | **Fibroepithelial polyp, skin** (received as "the thing from the back") |

**The Living Dictionary:**

- When a pathologist corrects a nomenclature mapping, that correction is stored *with attribution*
- The system prioritizes the current pathologist's personal dictionary
- If two pathologists map the same input term differently, the discrepancy is flagged and routed to a separate arbitration system
- Over time, the department converges on standardized nomenclature
- The hope: OR staff see standardized terms in reports and begin using them, eventually eliminating the need for parenthetical clarifications

**Dictionary Lookup Priority:**

1. Current pathologist's personal corrections
2. Frequency-based selection from all pathologist corrections (if no personal preference exists)
3. Probabilistic inference (for novel terms)

### Confidence and Clarification

The system maintains a merged confidence score combining transcription accuracy and interpretation certainty. This is displayed to the pathologist but is **transient**—it is not stored after finalization.

**Clarification Behavior:**

| Situation | Response |
|-----------|----------|
| Part assignment <90% confident | Hard stop; ask which part |
| Ambiguous phrasing | Soft flag; highlight for review |
| Clear high-confidence interpretation | No interruption; proceed |

The threshold for flags is **configurable by the clinical systems administrator**. Initial settings should be conservative (flag more), with tuning based on real-world usage.

### Voice Editing

The pathologist can edit the report by voice:

> "Modify the diagnosis for Part C. Instead of fibroepithelial polyp, say acrochordon."

The system:

1. Identifies the target (Part C diagnosis)
2. Sends the modification instruction to the LLM
3. Paraphrases/regenerates the content per instruction
4. Displays the updated text with appropriate confidence

Other voice editing examples:

- "Delete the last sentence."
- "In Part A, remove the word 'benign' before 'compound nevus'."
- "Add a comment: clinical correlation recommended."

Manual editing via keyboard/mouse is always available as an alternative.

### Session Persistence

**Critical requirement:** Sessions persist across interruptions.

If a pathologist:

- Closes the browser accidentally
- Gets interrupted by a phone call
- Loses network connectivity

...the partial work is saved. Upon returning, the system prompts:

> "You have an in-progress report for case [accession number]. Would you like to continue?"

All content, draft state, and authorship information is preserved.

---

## 4. Multi-Author Workflow

Academic pathology involves teaching. Reports often pass through multiple hands:

1. **Resident** creates initial draft
2. **Fellow** reviews and refines
3. **Attending** finalizes

### Attribution Tracking

The system tracks:

- Who created the initial draft (and when)
- Who made subsequent modifications (and when)
- Who finalized the report (and when)

This attribution is stored with the draft and transmitted to the LIS upon finalization.

### Workflow States

```
┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐
│  Empty   │ ──▶  │  Draft   │ ──▶  │  Review  │ ──▶  │Finalized │
│          │      │(Resident)│      │(Attending)│     │          │
└──────────┘      └──────────┘      └──────────┘      └──────────┘
```

A draft can be saved and returned to later. The attending can see who created the draft and make modifications. The system does not currently support inline comments from attending to resident (educational feedback)—this may be considered for Phase 2.

### Second Pathologist Review

Some cases require consultation or second review. The system should capture when a second pathologist has reviewed the case, with their identity recorded.

---

## 5. Peripheral Document Access

### Available Documents

The following may be available for a given case (depending on what exists in source systems):

- Operative notes
- Endoscopy reports
- Radiology reports
- Prior pathology (same patient)
- Outside consultation reports
- Requisition form (as image)
- Clinical history/notes

### Interaction Model

Documents appear as **icons or indicators** in the peripheral area of the interface.

**Mouseover:** Shows a preview tooltip with document type, date, and brief excerpt

**Click:** Opens a **modal popup** containing the full document

The modal:

- Overlays the report canvas (does not navigate away)
- Can be dismissed easily (click outside, press Escape, click X)
- Does not interrupt dictation if voice input is active

**Phase 2 consideration:** Ability to pull excerpts from peripheral documents directly into the report context.

---

## 6. Finalization

### The Finalize Action

When the pathologist is satisfied with the report, they click the **Finalize** button.

(Voice command for finalization is deferred to Phase 2.)

### What Finalization Does

1. **Locks the report content** — No further modifications except through formal amendment
2. **Strips confidence scores** — These are transient and not persisted
3. **Applies RTF formatting** — The report is formatted for professional appearance
4. **Transmits to LIS** — The formatted report, with author attribution, is sent via HL7
5. **Updates status** — The case is marked as finalized in Pathology Portal

### Post-Finalization

The **LIS** handles:

- Official sign-out (pathologist's electronic signature)
- ICD coding
- Billing
- Report distribution to ordering physicians
- Permanent audit trail

This module's responsibility ends at finalization. It is a **clinical** system, not a clerical one.

---

## 7. The Nomenclature Harmonization System

This deserves special attention as it was identified as the highest-value, lowest-risk feature to prototype first.

### The Problem

Operating rooms use inconsistent, informal, sometimes creative labels for specimens:

- "skin tag thing"
- "the mole from her face"
- "lower left colon surgery specimen"
- "rash area"

Pathology reports require standardized nomenclature for clarity, searchability, and professional standards.

### The Solution

A **living dictionary** that learns from pathologist behavior and converges toward standardization.

#### How It Works

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   OR Label      │ ──▶ │   Probabilistic │ ──▶ │   Standardized  │
│ "skin tag thing"│     │   Mapping       │     │  "Acrochordon"  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │  Pathologist    │
                        │  Correction?    │
                        └────────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    ▼                         ▼
            ┌─────────────┐           ┌─────────────┐
            │   Accepted  │           │  Corrected  │
            │   (Done)    │           │  (Learn)    │
            └─────────────┘           └──────┬──────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │ Store correction│
                                    │ with attribution│
                                    └────────┬────────┘
                                             │
                              ┌──────────────┴──────────────┐
                              ▼                             ▼
                      ┌─────────────┐              ┌─────────────┐
                      │  Matches    │              │  Conflicts  │
                      │  existing   │              │  with other │
                      │  (Update)   │              │  (Flag)     │
                      └─────────────┘              └──────┬──────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ Route to        │
                                                │ arbitration     │
                                                │ system          │
                                                └─────────────────┘
```

#### Display Behavior

When a mapping occurs, the interface shows:

```
┌─────────────────────────────────────────────────────────────────┐
│ Part C: Acrochordon, skin                                    🔄 │
│         (received as "skin tag thing")                          │
└─────────────────────────────────────────────────────────────────┘
```

The 🔄 indicator (or similar subtle visual cue) indicates a transformation occurred. Hovering reveals:

> "Original label: 'skin tag thing'  
> Standardized to: 'Acrochordon, skin' based on your previous corrections.  
> Click to modify."

#### Dictionary Ownership

The living dictionary belongs to the system (institution). However:

- Nothing prevents export for a departing pathologist
- The system is intended as open-source, so dictionary portability is philosophically aligned

#### No Pre-Seeding

The dictionary starts empty. It will populate quickly through natural usage. Pre-seeding was considered but rejected to avoid importing assumptions that may not fit the institution's preferences.

---

## 8. Technical Considerations

### Data Model Concept

The report is internally structured (conceptually as JSON) but this structure is not exposed to users or persisted after finalization. The structure serves to:

- Enable intelligent editing ("modify Part C")
- Track confidence per field (transiently)
- Ensure proper formatting on output

Example internal structure (illustrative, not prescriptive):

```json
{
  "accession": "S25-12345",
  "parts": [
    {
      "designation": "A",
      "original_label": "skin lesion left arm",
      "standardized_label": "Skin, left arm, excision",
      "diagnosis": "Compound nevus, benign",
      "confidence": 0.95
    },
    {
      "designation": "B",
      "original_label": "skin lesion right arm", 
      "standardized_label": "Skin, right arm, excision",
      "diagnosis": "Seborrheic keratosis",
      "confidence": 0.98
    }
  ],
  "authors": {
    "created_by": "Dr. Sharma",
    "created_at": "2025-01-15T09:30:00Z",
    "finalized_by": "Dr. Okonkwo",
    "finalized_at": "2025-01-15T14:45:00Z"
  },
  "status": "finalized"
}
```

Upon finalization, this is rendered to formatted text (RTF) and transmitted. The JSON structure does not persist.

### Confidence Score Handling

**Critical:** Confidence scores are **transient**. They exist during the authoring session to guide pathologist attention. They are **not** stored, transmitted, or visible after finalization.

This is intentional:

- The pathologist is the final arbiter of accuracy
- Confidence scores are a tool, not a record
- No regulatory or liability implications of AI confidence
- The LIS receives a human-verified report, not a probabilistic one

### Error Handling

| Scenario | Behavior |
|----------|----------|
| Transcription fails | Display error, offer retry, allow manual typing |
| LLM interpretation fails | Preserve raw transcription, flag for review |
| Network interruption | Auto-save current state, reconnect and resume |
| LIS transmission fails | Queue for retry, notify user, prevent re-finalization |

### Access Control

Access is restricted to:

- **Pathologists** — Full authoring capabilities
- **Clinical systems administrators** — Configuration, training, troubleshooting (view only for reports)

No clerical access. This is a clinical system.

---

## 9. User Interface Principles

### Design Philosophy

1. **The report is the focus.** Everything else is peripheral.

2. **Voice-first, but not voice-only.** Every action achievable by voice is also achievable by keyboard/mouse.

3. **Minimal interruption.** Clarification questions are necessary but should be non-blocking where possible.

4. **Transparency in transformation.** When the system changes something (nomenclature mapping, formatting), show what changed and why.

5. **Speed over features.** A pathologist signing out 40 cases has no patience for unnecessary clicks or confirmations.

### Key Interface Elements

#### The Report Canvas

- Central, dominant screen real estate
- Shows the report as it will appear when finalized
- Editable inline (click to edit any section)
- Visual indicators for: draft status, confidence flags, transformed content

#### Part Navigation

- Parts listed in order received from LIS
- Clear visual distinction between parts
- Current dictation target highlighted
- Easy jump between parts (voice: "Part C"; click: click the part header)

#### Peripheral Document Tray

- Iconographic representation of available documents
- Mouseover for preview
- Click for modal full view
- Non-intrusive, does not compete with report canvas

#### Status Bar

- Draft/Finalized status
- Current author
- Last saved timestamp
- Finalize button (prominent when in draft, hidden when finalized)

#### Clarification Dialogs

- **Hard stops:** Modal, must be addressed before continuing
- **Soft flags:** Inline highlight, can be deferred
- Clear language, specific questions
- Easy to answer (click option or speak response)

---

## 10. Success Metrics

### Primary Metrics

| Metric | Description | Target |
|--------|-------------|--------|
| Time-on-record | Average time from opening case to finalization | Reduction vs. baseline |
| Amendment rate | Reports requiring amendment after finalization | Reduction vs. current rate |
| User satisfaction | Pathologist survey scores | High satisfaction |

### Secondary Metrics

| Metric | Description |
|--------|-------------|
| Transcription accuracy | % of dictation correctly transcribed |
| Interpretation accuracy | % of content correctly mapped to fields |
| Dictionary convergence | Reduction in arbitration queue over time |
| Adoption rate | % of pathologists actively using the module |

### Validation

Before deployment, the system will be validated against test cases with known correct outputs. Validation criteria TBD but will include:

- Accuracy of transcription
- Correctness of part assignment
- Appropriateness of nomenclature mapping
- Proper handling of edge cases

---

## 11. Phase 2 Roadmap (For Awareness)

The following features are explicitly deferred to Phase 2:

| Feature | Rationale for Deferral |
|---------|------------------------|
| Mentor/institutional pattern retrieval | Policy complexity; pathologists accumulate benign case patterns quickly |
| Synoptic reporting (CAP templates) | Sophisticated design required; separate specification needed |
| Pull excerpts from peripheral docs | Integration complexity |
| Voice command for finalization | Button sufficient for Phase 1 |
| Inline educational comments (attending → resident) | Workflow refinement needed |
| Accessibility accommodations | Address standard needs first; accessibility via existing LIS if needed |

---

## 12. Open Questions for Development Team

The following items need resolution during implementation:

1. **Flag threshold defaults** — What specific thresholds for soft flags vs. hard stops? (Clinical admin configurable, but need initial values)

2. **Dictionary conflict resolution timing** — When no personal preference exists and multiple pathologists have different mappings, what's the tiebreaker? (Current assumption: frequency-based)

3. **Session timeout** — How long before an inactive session is auto-saved and closed?

4. **RTF formatting specifics** — What exact formatting template should be used for finalized reports? (May need input from current LIS report formats)

5. **Arbitration system interface** — What data format does the arbitration system expect? (API specification needed)

---

## 13. Glossary

| Term | Definition |
|------|------------|
| **Accession number** | Unique identifier for a case in the LIS |
| **Part** | A distinct specimen within a case (Part A, Part B, etc.) |
| **LIS** | Laboratory Information System — the system of record |
| **Finalization** | The action of completing a report in this module (distinct from sign-out in LIS) |
| **Sign-out** | The official act of releasing a report, performed in LIS |
| **Nomenclature mapping** | Translation from informal OR labels to standardized pathology terminology |
| **Living dictionary** | The accumulated mappings of informal labels to standard terms, learned from pathologist corrections |
| **Arbitration** | Resolution of conflicting nomenclature preferences between pathologists |
| **Synoptic reporting** | Structured checklist-style reporting for cancer cases (Phase 2) |
| **CAP** | College of American Pathologists — source of synoptic reporting templates |

---

## 14. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | January 2026 | Development Team | Initial narrative specification |

---

*This document serves as the working specification for Phase 1 development. It will be formalized into User Needs statements and Design Inputs as the project progresses through the QMS process.*
