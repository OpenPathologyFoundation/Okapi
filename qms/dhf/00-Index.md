# Design History File (DHF) Index — Okapi

## 1. Purpose
This file is the authoritative index for the Design History File (DHF) for the Okapi system.
It provides:
- The complete list of controlled DHF artifacts (documents and records)
- Where each artifact lives in the repository
- How the artifact is approved, versioned, and linked to objective evidence
- The current DHF status by release/baseline

This DHF index is maintained in source control and updated as part of change control.

## 2. Scope
This DHF covers the design and evolution of the Okapi platform, including:
- Cloud-hosted deployment (AWS)
- Clinical decision support (CDS) and workflow automation
- AI-assisted suggestions (assistive, clinician-in-the-loop)
- Write-back / interoperability with Traditional AP LIS / EHR via HL7 v2, FHIR, and other export channels
- Identity, authorization, and auditability across trust boundaries (hospital network ↔ cloud)

Out of scope (unless explicitly pulled into scope by change control):
- Traditional AP LIS internal configuration artifacts (owned by hospital teams)
- Third-party vendor software DHFs (retained as supplier documentation)

## 3. Document Control and Approvals
DHF documents are controlled records and must follow:
- SOP-DocControl (format, required metadata, approvals)
- SOP-ChangeControl (PR-based approvals and required reviewers)

### 3.1 Approval mechanism (GitHub-native)
- Controlled DHF artifacts are approved via Pull Request review and merge.
- The PR is the approval record; reviewers act as approvers.
- Required checks (CI) must pass unless a documented deviation/waiver is approved per SOP-ChangeControl.

### 3.2 Document metadata requirements
Each controlled DHF document SHALL include, at minimum:
- Document ID
- Title
- Owner
- Approver(s) or approval role(s)
- Effective date
- Revision history (or reference to Git history)

## 4. Naming Conventions and IDs
### 4.1 Requirement / risk / test identifiers
- User needs (Authentication): UN-AUTHN-###
- User needs (Authorization): UN-AUTHZ-###
- User needs (Profile): UN-PROF-###
- User needs (Research): UN-RES-###
- User needs (HAT): UN-HAT-###
- User needs (Worklist): UN-WL-###
- User needs (Administration): UN-ADMIN-###
- System requirements (Authentication): SYS-AUTHN-###
- System requirements (Authorization): SYS-AUTHZ-###
- System requirements (Profile): SYS-PROF-###
- System requirements (Research): SYS-RES-###
- System requirements (HAT): SYS-HAT-###
- System requirements (Worklist): SYS-WL-###
- System requirements (Administration): SYS-ADMIN-###
- Interface requirements: IR-###
- Risks/hazards: RISK-### (module-specific: RISK-HAT-###, RISK-WL-###, RISK-ADMIN-###)
- Risk controls: RC-###
- Test cases: TEST-###
- Design elements/modules: MOD-### (optional but recommended)
- Releases/baselines: REL-YYYY.MM.DD or REL-<semantic>

### 4.2 Repository structure
/qms
/sops           (procedures)
/dhf            (design history file artifacts)
/templates      (document templates)
/records        (optional: exported evidence snapshots if not stored in CI artifacts)

## 5. DHF Artifact Index
The following artifacts constitute the DHF for this system.

### 5.1 DHF Index and Core Definition
| ID | Artifact | Purpose | Path | Status |
|---|---|---|---|---|
| 00 | Index | The "Map" (Links to specific versions of files) | qms/dhf/00-Index.md | Active |

### 5.2 REQUIREMENTS (The "What")
| ID | Artifact | Purpose | Path | Status |
|---|---|---|---|---|
| 01 | PURS | Product & User Requirements (Merged for speed) | qms/dhf/01-PURS.md | Draft/Active |
| 02 | SRS | System Requirements (The technical "shall" statements) | qms/dhf/02-SRS.md | Draft/Active |

### 5.3 ARCHITECTURE (The "How")
| ID | Artifact | Purpose | Path | Status |
|---|---|---|---|---|
| 03 | Cybersecurity | Threat Model & Security Controls | qms/dhf/03-Cybersecurity.md | Draft/Active |
| 04 | SDS | Software Design Spec (folder) | qms/dhf/04-SDS/ | Draft/Active |
| 04-00 | SDS Overview | High-level architecture, component boundaries | qms/dhf/04-SDS/00-SDS-Overview.md | Draft/Active |
| 04-01 | AuthN Architecture | Authentication: Identity federation, sessions, device trust | qms/dhf/04-SDS/01-AuthN-Architecture.md | Draft/Active |
| 04-02 | AuthZ Architecture | Authorization: RBAC, permissions, break-glass, research grants | qms/dhf/04-SDS/02-AuthZ-Architecture.md | Draft/Active |
| 04-03 | IAM Schema | IAM database schema specification | qms/dhf/04-SDS/03-IAM-Schema.md | Draft/Active |
| 04-04 | HAT Architecture | Histology Asset Tracking architecture, data concepts, and controls | qms/dhf/04-SDS/04-HAT-Architecture.md | Draft/Active |
| 04-05 | Worklist Architecture | Work List module design | qms/dhf/04-SDS/05-Worklist-Architecture.md | Draft/Active |

### 5.4 RISK & QUALITY (The "Safety")
| ID | Artifact | Purpose | Path | Status |
|---|---|---|---|---|
| 05a | Risk-Plan | The Rules (Severity/Prob Tables). Write once. | qms/dhf/05a-Risk-Plan.md | Draft/Active |
| 05b | Hazard-Analysis | The Matrix (List of Risks). Edit often. | qms/dhf/05b-Hazard-Analysis.md | Draft/Active |
| 06 | VVP | Verification Plan (How we test). | qms/dhf/06-VVP.md | Draft/Active |

## 6. THE ROBOT ZONE (Generated Artifacts)
These artifacts are generated automatically and stored in the `/artifacts` directory (gitignored).

| Artifact | Source |
|---|---|
| Trace-Matrix.csv | Generated from 01, 02, 06 |
| Test-Results.pdf | Generated from JUnit/PyTest |
| SBOM.json | Generated from package.json |

## 7. SOP Index (Procedures referenced by DHF)

### 7.1 Core SOPs (Reusable across projects)
| SOP ID | Title | Path | Status |
|---|---|---|---|
| SOP-DHF-001 | Design History File Management | sops/SOP-DHF-Management.md | Active |

### 7.2 Project-Specific SOPs
Project-specific SOPs live in the top-level `/sops` folder for easy discovery and versioned approvals in this repo.

| SOP ID | Title | Path | Status |
|---|---|---|---|
| SOP-DC | Document & Record Control | sops/SOP-DocControl.md | Active |
| SOP-CC | Change Control & Config Mgmt | sops/SOP-ChangeControl.md | Active |
| SOP-SDLC | Software Development Lifecycle | sops/SOP-SDLC.md | Active |
| SOP-RISK | Risk Management | sops/SOP-RiskMgmt.md | Active |
| SOP-VULN | Vulnerability Mgmt + Incident Response | sops/SOP-VulnMgmt.md | Active |
| SOP-CAPA | Problem Resolution / CAPA-lite | sops/SOP-CAPA-Lite.md | Active |

## 8. Objective Evidence and CI/CD Records
Certain DHF records are generated or collected by CI/CD (GitHub Actions) and stored as build artifacts and/or in releases.

### 8.1 Evidence types (minimum)
- Build provenance: commit SHA, build ID, container image digests
- Unit/integration test reports
- SAST results
- Dependency scan results
- IaC scan results (if applicable)
- SBOM
- Deployment manifest / environment snapshot
- Release notes
- Interface conformance tests (HL7/FHIR/CSV golden tests)

### 8.2 Evidence locations
- CI artifacts: GitHub Actions artifacts for workflow runs
- Release attachments: GitHub Releases (optional, recommended for formal baselines)
- Optional repository snapshots: qms/records/<release>/ (only if you must keep evidence in-repo)

### 8.3 Evidence-to-release linkage
Each RELREC-<release>.md SHALL reference:
- the Git tag / commit SHA
- CI workflow run identifiers
- stored artifact locations for SBOM, test reports, scans, and deployment manifests

## 9. Baselines and Current Status
### 9.1 Current baseline
- Baseline name: <TBD>
- Baseline date: <TBD>
- Git tag: <TBD>
- Release record: qms/dhf/records/RELREC-<release>.md

### 9.2 Open items
- List high-level DHF gaps or pending approvals (if any)

## 10. Revision History (Index)
- v0.1: Initial DHF index created (YYYY-MM-DD)
- v0.2: <TBD>
