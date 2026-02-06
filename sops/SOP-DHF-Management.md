# Standard Operating Procedure: Design History File (DHF) Management

---
document_id: SOP-DHF-001
title: Design History File Management
version: 1.0
status: ACTIVE
owner: Quality Assurance
effective_date: 2026-01-21
review_cycle: Annual
references:
  - ISO 13485:2016 (Section 7.3 - Design and Development)
  - ISO 14971:2019 (Risk Management)
  - 21 CFR 820.30 (FDA Design Controls)
  - IEC 62304:2006+A1:2015 (Medical Device Software Lifecycle)
---

## 1. Purpose

This Standard Operating Procedure (SOP) defines the workflow process for creating, maintaining, and managing the Design History File (DHF) for medical device software projects. It establishes:

- The structure and organization of DHF artifacts
- The workflow for parsing design specifications into controlled documents
- Traceability requirements from User Needs through Verification
- Integration with Agile/Sprint-based development methodologies
- Compliance with FDA Design Controls and ISO 13485 requirements

## 2. Scope

This SOP applies to:
- All software-as-a-medical-device (SaMD) projects
- All personnel involved in design, development, verification, and quality assurance
- The complete product lifecycle from concept through post-market

## 3. Definitions

| Term | Definition |
|:-----|:-----------|
| **DHF** | Design History File — The compilation of records describing the design history of a finished device. |
| **PURS** | Product & User Requirements Specification — High-level, non-technical user needs. |
| **SRS** | System Requirements Specification — Technical "shall" statements derived from user needs. |
| **SDS** | Software Design Specification — Detailed technical architecture and module designs. |
| **VVP** | Verification and Validation Plan — How we test that requirements are met. |
| **Trace Matrix** | A document linking User Needs → Requirements → Design → Tests. |
| **Design Input** | Technical specifications derived from User Needs. |
| **Design Output** | Evidence of what was built (code, diagrams, specifications). |
| **Design Verification** | "Did we build the device right?" — Testing outputs against inputs. |
| **Design Validation** | "Did we build the right device?" — Testing against user needs in real/simulated use. |

## 4. DHF Directory Structure

The DHF SHALL be organized in the following structure within the repository:

```
qms/
├── dhf/                          # Design History File artifacts
│   ├── 00-Index.md               # DHF Index (the "map")
│   ├── 01-PURS.md                # Product & User Requirements
│   ├── 02-SRS.md                 # System Requirements Specification
│   ├── 03-Cybersecurity.md       # Threat Model & Security Controls
│   ├── 04-SDS/                   # Software Design Specifications (directory)
│   │   ├── 00-SDS-Overview.md    # High-level architecture
│   │   ├── 01-<Module>-Architecture.md
│   │   ├── 02-<Module>-Architecture.md
│   │   └── ...                   # One file per major module
│   ├── 05a-Risk-Plan.md          # Risk scoring tables (write once)
│   ├── 05b-Hazard-Analysis.md    # Risk matrix (edit often)
│   ├── 06-VVP.md                 # Verification & Validation Plan
│   └── records/                  # Release records (optional)
│       └── RELREC-<release>.md
├── sops/                         # Standard Operating Procedures
└── templates/                    # Document templates
```

### 4.1 Naming Conventions

| Artifact Type | ID Format | Example |
|:--------------|:----------|:--------|
| User Needs | UN-### | UN-001, UN-HAT-001 |
| System Requirements | SYS-<MODULE>-### | SYS-AUTH-001, SYS-HAT-001 |
| Interface Requirements | IR-### | IR-001 |
| Risks/Hazards | RISK-### | RISK-001 |
| Risk Controls | RC-### | RC-001 |
| Test Cases | TEST-### | TEST-AUTH-001 |
| Design Modules | MOD-### | MOD-AUTH, MOD-HAT |
| Releases | REL-YYYY.MM.DD | REL-2026.01.21 |

## 5. Design Control Workflow (The Waterfall)

Even when using Agile methodologies, the following linear logic MUST be maintained. For Agile teams, each Sprint represents a complete micro-waterfall cycle.

```
┌─────────────────┐
│   User Needs    │  ← "What does the user want?" (Non-technical)
│     (PURS)      │
└────────┬────────┘
         │ Trace: UN-### → SYS-###
         ▼
┌─────────────────┐
│  Design Inputs  │  ← Technical specifications ("shall" statements)
│     (SRS)       │
└────────┬────────┘
         │ Trace: SYS-### → MOD-###
         ▼
┌─────────────────┐
│ Design Outputs  │  ← What you built (code, architecture, diagrams)
│     (SDS)       │
└────────┬────────┘
         │ Trace: MOD-### → TEST-###
         ▼
┌─────────────────┐
│   Verification  │  ← "Did we build the device RIGHT?"
│     (VVP)       │     (Unit tests, integration tests)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Validation    │  ← "Did we build the RIGHT device?"
│  (Clinical/UX)  │     (Usability testing, clinical evaluation)
└─────────────────┘
```

### 5.1 Traceability Requirements

**CRITICAL**: You MUST be able to draw a line from:
```
User Need → Design Input → Design Output → Verification Test
```

If any link is broken, the design is non-compliant.

Each document SHALL include:
- `trace_source`: Reference to upstream document(s)
- `trace_destination`: Reference to downstream document(s)
- Traceability tables mapping specific IDs

## 6. Document Creation Workflow

### 6.1 Phase 1: User Needs Capture (PURS)

**Input**: Stakeholder interviews, clinical workflows, market requirements
**Output**: `qms/dhf/01-PURS.md`

1. Capture user needs as high-level, non-technical statements
2. Assign unique identifiers (UN-###)
3. Document rationale/clinical justification for each need
4. Group by functional area (e.g., IAM, HAT, Worklist)

**Format**:
```markdown
| ID | User Need Statement | Rationale/Clinical Justification |
|:---|:--------------------|:---------------------------------|
| UN-001 | The user needs... | Reduces risk of... |
```

### 6.2 Phase 2: System Requirements (SRS)

**Input**: `01-PURS.md`
**Output**: `qms/dhf/02-SRS.md`

1. Derive technical "shall" statements from each User Need
2. Assign unique identifiers (SYS-<MODULE>-###)
3. Link each requirement to source User Need(s)
4. Specify verification method (Test, Analysis, Inspection, Demonstration)

**Format**:
```markdown
| ID | System Requirement | Trace to User Need | Verification Method |
|:---|:-------------------|:-------------------|:--------------------|
| SYS-AUTH-001 | The system shall... | UN-001, UN-002 | Test |
```

### 6.3 Phase 3: Software Design Specification (SDS)

**Input**: `02-SRS.md`
**Output**: `qms/dhf/04-SDS/` directory

#### 6.3.1 SDS Overview Document

Create `04-SDS/00-SDS-Overview.md` containing:
- Introduction and scope
- High-level architectural overview
- Component boundaries (with diagrams)
- Cross-cutting concerns (security, audit, etc.)

#### 6.3.2 Module Architecture Documents

For each major module, create `04-SDS/##-<Module>-Architecture.md`:

1. **Introduction**: Module purpose and scope
2. **Design Decisions**: Key architectural choices with rationale
3. **Data Model**: Entity relationships, schemas
4. **Interface Contracts**: APIs, events, protocols
5. **Security Controls**: Module-specific security measures
6. **Traceability Table**: Map design elements to SRS requirements

**Format for Traceability**:
```markdown
| Design Element | System Requirement | Risk Control |
|:---------------|:-------------------|:-------------|
| AuthN Gateway  | SYS-AUTH-001       | RISK-001     |
```

### 6.4 Phase 4: Risk Management

**Input**: All design documents
**Output**: `05a-Risk-Plan.md`, `05b-Hazard-Analysis.md`

#### 6.4.1 Risk Plan (Write Once)

Define and document:
- Severity scoring criteria (1-5 scale)
- Probability scoring criteria (1-5 scale)
- Risk acceptability matrix (Low/Medium/High thresholds)
- ALARP (As Low As Reasonably Practicable) criteria

#### 6.4.2 Hazard Analysis (Edit Often)

For each identified hazard:
1. Map: **Hazard** → **Foreseeable Event** → **Hazardous Situation** → **Harm**
2. Score initial risk (Severity × Probability)
3. Define risk controls
4. Score residual risk
5. Document acceptance rationale

**The P1/P2 Probability Split for Software**:

> **CRITICAL PEARL**: Software doesn't "wear out" — a bug is present 100% or 0% of the time.

Split probability assessment into:
- **P1**: Probability of the software anomaly occurring (often assumed 100% if bug exists)
- **P2**: Probability that IF the anomaly occurs, it leads to harm

You cannot argue code is "unlikely to fail." You MUST argue that even if it fails (P1=1), architecture prevents patient harm (low P2).

### 6.5 Phase 5: Verification & Validation Plan (VVP)

**Input**: `02-SRS.md`, `04-SDS/`
**Output**: `qms/dhf/06-VVP.md`

1. Define test cases for each system requirement
2. Specify pass/fail criteria BEFORE running tests
3. Link tests to requirements (TEST-### → SYS-###)
4. Document test environment and prerequisites

**Format**:
```markdown
| Test ID | Requirement | Test Description | Pass Criteria | Status |
|:--------|:------------|:-----------------|:--------------|:-------|
| TEST-AUTH-001 | SYS-AUTH-001 | Verify OIDC... | Token valid... | PASS |
```

## 7. Agile Integration: Sprint-Based DHF Updates

### 7.1 The Micro-Waterfall Principle

> **PEARL**: Agile is just "Micro-Waterfall" — you don't abandon the waterfall; you shrink it.

Each Sprint is a complete design control cycle:
1. Define the input for sprint features (update SRS if needed)
2. Design and code (update SDS)
3. Test and document (update VVP)
4. Review and approve (Design Review)

### 7.2 Sprint DHF Checklist

| Activity | DHF Artifact | When |
|:---------|:-------------|:-----|
| Sprint Planning | Update SRS with new requirements | Sprint Start |
| Design | Update/create SDS module docs | During Sprint |
| Implementation | Code + unit tests | During Sprint |
| Risk Review | Update Hazard Analysis if new risks | During Sprint |
| Sprint Review | Update VVP with test results | Sprint End |
| Design Review | PR approval by independent reviewer | Before Merge |

## 8. Independent Review Requirement

> **CRITICAL COMPLIANCE POINT**: ISO 13485 requires an independent reviewer for Design Reviews.

### 8.1 Requirements

- The reviewer CANNOT be the person who wrote the code
- The reviewer MUST be competent in the design stage being reviewed
- The reviewer MUST NOT be directly responsible for the design

### 8.2 Implementation via Pull Request

- All DHF document changes require PR approval
- PR reviewer acts as the independent reviewer
- PR approval is the approval record
- CI checks must pass before merge

## 9. Software-Specific Requirements (SaMD)

### 9.1 Design Transfer

For software, "Design Transfer" is the moment code moves from **Development** to **Production**.

Document:
- Build/release process
- Environment configurations
- Deployment verification steps

### 9.2 Unresolved Anomalies List

> **PEARL**: You don't have to fix every bug before release. You DO have to document every known bug you didn't fix.

Maintain a list of known issues with:
- Bug description
- Severity assessment
- Justification for non-fix (e.g., "Cosmetic only," "Workaround exists")
- Risk assessment

### 9.3 Cybersecurity Documentation

Maintain `03-Cybersecurity.md` with:
- Threat model
- Security controls
- Reference to applicable standards (NIST, FDA guidance)

### 9.4 FDA Submission Considerations

> **PEARL**: For SaMD, the FDA wants to see your Architecture, Revision History, and Unresolved Anomaly list — unlike hardware where you often submit only summaries.

Ensure these are readily exportable:
- Software architecture documentation
- Version/revision history
- Unresolved anomalies list
- SBOM (Software Bill of Materials)

## 10. Document Control

### 10.1 Version Control

- All DHF documents are version-controlled in Git
- Document version is tracked in YAML frontmatter
- Git history provides audit trail

### 10.2 Approval Mechanism

- Approval via Pull Request review and merge
- Required reviewers defined per document type
- CI checks enforce format and completeness

### 10.3 Document Metadata Requirements

Each DHF document SHALL include:

```yaml
---
document_id: <unique identifier>
title: <document title>
version: <semantic version>
status: DRAFT | ACTIVE | SUPERSEDED
owner: <role or name>
created_date: <YYYY-MM-DD>
effective_date: <YYYY-MM-DD or TBD>
trace_source: <upstream document ID(s)>
trace_destination: <downstream document ID(s)>
---
```

## 11. Generated Artifacts

The following artifacts are generated automatically and stored in `/artifacts` (gitignored):

| Artifact | Source | Purpose |
|:---------|:-------|:--------|
| Trace-Matrix.csv | PURS, SRS, VVP | Complete traceability report |
| Test-Results.pdf | JUnit/PyTest | Verification evidence |
| SBOM.json | package.json/pom.xml | Software composition |
| Coverage-Report.html | Test framework | Code coverage evidence |

## 12. Release Process

### 12.1 Pre-Release Checklist

- [ ] All User Needs traced to System Requirements
- [ ] All System Requirements traced to Design Elements
- [ ] All System Requirements have verification tests
- [ ] All verification tests have documented results
- [ ] Risk analysis complete with acceptable residual risk
- [ ] Unresolved anomalies documented and justified
- [ ] Independent design review completed
- [ ] Cybersecurity review completed

### 12.2 Release Record

Create `qms/dhf/records/RELREC-<release>.md` containing:
- Git tag / commit SHA
- CI workflow run identifiers
- Links to stored artifacts (SBOM, test reports, scans)
- Deployment manifest / environment snapshot
- Release notes

## 13. Regulatory Considerations

### 13.1 US (FDA)

- DHF is the internal design history
- 510(k) submission is a summary of the DHF
- For SaMD: Architecture, revision history, and anomaly list are typically requested

### 13.2 EU (MDR)

- Technical File is similar to DHF
- Requires GSPR (General Safety and Performance Requirements) checklist
- Requires Clinical Evaluation Report (CER)

## 14. References

- ISO 13485:2016 — Medical devices — Quality management systems
- ISO 14971:2019 — Medical devices — Application of risk management
- IEC 62304:2006+A1:2015 — Medical device software — Software life cycle processes
- 21 CFR 820.30 — FDA Quality System Regulation — Design Controls
- FDA Guidance: Content of Premarket Submissions for Software Contained in Medical Devices

## 15. Revision History

| Version | Date | Author | Description |
|:--------|:-----|:-------|:------------|
| 1.0 | 2026-01-21 | QA | Initial release |

---

**Document Control**: This SOP is a controlled document. Changes require review and approval per SOP-DocControl.
