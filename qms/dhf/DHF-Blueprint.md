```markdown
# GitHub-Native Blueprint for Medical Device Software (SaMD / Software-in-a-Device)
document_id: DHF-BLUEPRINT-001  
version: 1.0  
status: DRAFT  
owner: Quality + Engineering  
last_updated: 2026-01-28  
scope: US (FDA) + EU (MDR/IVDR) software lifecycle, QMS, cybersecurity, and Agile/CI/CD execution

---

## 1. Purpose

Define a **defensible, audit-ready** software development system for medical device software where:

- **GitHub is the system of record** for controlled documents and objective evidence.
- Work is executed via **Agile + CI/CD**, with **risk-based gates**.
- **AI assistance is allowed** but explicitly governed.

This blueprint specifies:
- Repository structure (controlled docs vs generated evidence)
- SOP set (procedural controls)
- DHF structure (design outputs + trace)
- GitHub practices (branch protection, PR requirements, CODEOWNERS)
- CI evidence (build/test/security/trace artifacts)
- A starter **Controls Matrix** mapping controls → GitHub implementation → evidence

---

## 2. Core principles (non-negotiable)

1. **No silent change**: every change is a PR; direct pushes to protected branches are blocked.
2. **Risk drives rigor**: the higher the safety/security impact, the stronger the required reviews and evidence.
3. **Traceability is continuous**: requirements ↔ risks ↔ tests ↔ releases are linked by IDs and regenerated.
4. **Evidence is reproducible**: releases have immutable, checksummed evidence packets.
5. **AI is governed**: AI output is treated as untrusted input until a qualified human accepts it and evidence supports it.

---

## 3. Repository layout

### 3.1 Canonical tree

/  
├─ QMS/                                  # Controlled documents (SOPs + DHF + Records)  
│  ├─ SOPs/                               # "How we work" procedures (controlled)  
│  ├─ DHF/                                # "What we built" design outputs (controlled)  
│  ├─ Templates/                          # Approved templates for controlled docs  
│  └─ Records/                            # Controlled records (training, reviews, approvals)  
│     ├─ DesignReviews/                   # Minutes/decisions (or links) per major change  
│     ├─ ReleaseApprovals/                # Release sign-off records  
│     ├─ AI/                              # AI usage records (when AI influences controlled outputs)  
│     └─ Audits/                          # Internal audit records (optional)  
│
├─ Artifacts/                             # Generated evidence (CI outputs)  
│  ├─ PR/                                 # PR-scoped evidence snapshots (optional; can link to CI)  
│  └─ ReleasePackets/                     # Immutable per-release evidence (required)  
│     └─ vX.Y.Z/  
│        ├─ manifest.json                 # Build provenance, toolchain versions  
│        ├─ checksums.txt                 # SHA256 checksums for artifacts  
│        ├─ sbom.spdx.json                # SBOM snapshot  
│        ├─ test-report-unit.xml  
│        ├─ test-report-integration.xml  
│        ├─ test-report-system.xml  
│        ├─ sast-report.json  
│        ├─ deps-vuln-report.json  
│        ├─ trace-matrix.csv              # Req ↔ Risk ↔ Test ↔ Release  
│        ├─ release-notes.md  
│        └─ known-anomalies.md  
│
├─ src/                                   # Product source code  
├─ tools/                                 # Scripts to generate index/trace/release packets  
├─ docs/                                  # Non-controlled docs (optional, keep separate)  
└─ .github/                               # Workflows, templates, CODEOWNERS, policies  

### 3.2 Controlled vs non-controlled

**Controlled** (must follow SOP-DocControl + SOP-ChangeControl):
- Anything under `QMS/**`
- Release packets under `Artifacts/ReleasePackets/**`
- Any file explicitly marked as controlled (if using front matter flags)

**Not controlled** (still reviewed, but not part of the audited DHF):
- `/docs/**` (unless you decide otherwise)
- Some engineering notes (unless elevated by risk gates)

---

## 4. SOP set (minimum viable, audit-worthy)

Place in `QMS/SOPs/` with doc_id, version, approver(s), effective date.

- SOP-01-DocControl.md  
- SOP-02-SDLC-Agile.md  
- SOP-03-ChangeControl.md  
- SOP-04-ConfigMgmt.md  
- SOP-05-RiskMgmt.md  
- SOP-06-VnV.md  
- SOP-07-Cybersecurity.md  
- SOP-08-SupplierMgmt.md  
- SOP-09-CAPA-Incidents.md  
- SOP-10-AIUse.md  

Each SOP must define:
- scope
- roles/responsibilities
- required records/evidence
- required review/approval rules
- exceptions (e.g., emergency hotfix) and how they are controlled

---

## 5. DHF document set (minimum viable, complete)

Place in `QMS/DHF/`.

DHF-00-Index.md  
- Auto-generated index listing all DHF docs (doc_id, version, status, owner)

DHF-01-IntendedUse-UserNeeds.md  
- intended use, users, environment, contraindications (as applicable)

DHF-02-SystemRequirements.md  
- system requirements with IDs + acceptance criteria + verification method

DHF-03-SoftwareRequirements-SRS.md  
- software requirements with IDs + risk linkage + test linkage

DHF-04-Architecture.md  
- component diagram, data flows, trust boundaries, interface contracts

DHF-05-Cybersecurity.md  
- threat model, security controls mapping, update/patch strategy, SBOM pointers

DHF-06-RiskManagementPlan.md  
DHF-07-HazardAnalysis.md (or YAML/CSV with a markdown rendering)
- hazards, harms, causes, risk controls, residual risk acceptance rationale

DHF-08-VnV-Plan.md  
- verification strategy + validation strategy + release criteria

DHF-09-Traceability.md (generated)
- rendered summary + links to generated `trace-matrix.csv`

DHF-10-ReleaseHistory.md  
- per release: intent, clinical impact statement, known anomalies, evidence packet link

DHF-11-EU-Classification.md (EU scope)
- MDR/IVDR classification rationale and rule mapping (if applicable)

---

## 6. Requirement / risk / test IDs (traceability backbone)

### 6.1 ID conventions (example)

- User Needs: `UN-###`
- System Requirements: `SR-###`
- Software Requirements: `SWR-###`
- Hazards: `HAZ-###`
- Risk Controls: `RC-###`
- Test Cases: `TC-###`
- Defects: `BUG-###`
- Releases: `REL-vX.Y.Z`

### 6.2 Required trace links

Every SW requirement must link to:
- at least one hazard OR an explicit “no safety impact” rationale
- at least one test case

Every hazard control must link to:
- requirements implementing the control
- tests verifying the control

---

## 7. GitHub governance practices (controls implemented as workflow)

### 7.1 Branching model (simple, defensible)

- `main` = always releasable
- `feature/*` = all work branches
- `release/vX.Y.Z` (optional) = stabilization branch for regulated releases
- Tags: `vX.Y.Z` are immutable release markers

### 7.2 Branch protection (must enable)

Protect `main` and `release/*`:
- Require PRs
- Require 2 approvals (or more based on risk label)
- Require passing status checks (CI)
- Require linear history (optional)
- Restrict who can push (no one, except emergency process)
- Require signed commits or signed tags (recommended)
- Enforce CODEOWNERS reviews

### 7.3 Risk-based gates (labels)

Mandatory PR labels:
- `risk:low | risk:med | risk:high`
Optional impact labels:
- `safety`, `security`, `data`, `ui`, `performance`, `clinical-impact`

Gate rules:
- `risk:low` → 1 engineer + 1 peer approval, CI green
- `risk:med` → 2 approvals incl. module owner, CI green, updated tests required
- `risk:high` or `safety` or `security` → 2–3 approvals incl. safety/security owner,
  design note required, hazard analysis update required (or documented rationale),
  security evidence required if `security`

---

## 8. PR template (drop into .github/pull_request_template.md)

## Summary
- What changed?

## Rationale
- Why is this change needed? (UN/SR/SWR, BUG, or planned improvement)

## Impact Assessment (check all that apply)
- [ ] No safety impact
- [ ] Potential safety impact (requires hazard analysis link/update)
- [ ] No cybersecurity impact
- [ ] Potential cybersecurity impact (requires threat model / security evidence)
- [ ] Data/model impact
- [ ] UI/workflow impact
- [ ] Performance impact
- [ ] Clinical-facing behavior changes

## Trace Links
- User Needs: UN-…
- System Requirements: SR-…
- Software Requirements: SWR-…
- Hazards / Risk Controls: HAZ-… / RC-…
- Test Cases: TC-…

## Evidence
- CI run link:
- Test reports (unit/integration/system):
- If security-relevant: SAST/DAST + dependency scan links:
- If docs updated: DHF/SOP links:

## Release Note Fragment (required for clinical-impact changes)
- Proposed release note:

## Design Note (required if risk:high, safety, security, architecture changes)
- Link to QMS/Records/DesignReviews/… or attached summary:

---

## 9. CODEOWNERS example (drop into .github/CODEOWNERS)

# Default: at least one engineering owner
* @eng-team

# QMS controlled docs require Quality review
/QMS/ @quality-team

# Risk management and cybersecurity require specific owners
/QMS/DHF/DHF-06-RiskManagementPlan.md @quality-team @safety-owner
/QMS/DHF/DHF-07-HazardAnalysis* @quality-team @safety-owner
/QMS/DHF/DHF-05-Cybersecurity.md @quality-team @security-owner

# Security-sensitive code areas
/src/security/ @security-owner @eng-team
/tools/sbom/ @security-owner @eng-team

# Release packets (immutable evidence)
 /Artifacts/ReleasePackets/ @quality-team @release-manager

---

## 10. CI/CD evidence requirements

### 10.1 Required checks on every PR
- Build succeeds
- Unit tests + report
- Static analysis (SAST) report
- Dependency vulnerability scan report
- Trace regeneration (at least: requirements ↔ tests; full trace on main merge)

### 10.2 Required outputs on release tag vX.Y.Z
Generate `Artifacts/ReleasePackets/vX.Y.Z/` containing:
- manifest.json (SHA, build info, tool versions)
- checksums.txt (SHA256)
- signed binary/container references
- full test reports (unit/integration/system)
- sbom.spdx.json (+ license summary)
- sast-report.json
- deps-vuln-report.json
- trace-matrix.csv
- release-notes.md
- known-anomalies.md
- validation-summary.md (if validation executed for this release)

**Rule:** Release packet content is immutable once published.

---

## 11. AI use governance (minimum rules)

### 11.1 Allowed uses
- PR review assistance (logic errors, edge cases, security smells)
- requirements quality checks (ambiguity, testability)
- test suggestion generation (human-reviewed)
- threat modeling brainstorming (human-validated)
- documentation drafting (human-approved)

### 11.2 Required controls
- No PHI/sensitive production data in prompts unless explicitly approved.
- When AI influences controlled outputs (QMS/DHF, risk, cybersecurity):
  - store a short record in `QMS/Records/AI/AI-YYYYMMDD-<topic>.md` including:
    - purpose
    - tool/model identifier
    - prompt (or sanitized excerpt)
    - key outputs used
    - human acceptance decision + rationale

AI output is never a substitute for verification evidence.

---

## 12. Controls Matrix (starter)

Create: `QMS/DHF/DHF-ControlsMatrix.md`

| Control Objective | Standard Anchor | GitHub Implementation | Objective Evidence |
|---|---|---|---|
| Controlled documents are identified, versioned, approved | QMS Doc Control | `QMS/**` protected; PR approvals required | PR + approvals + file history |
| No uncontrolled changes to product or DHF | Change Control | Branch protection; PR-only merges | GitHub branch rules + PR logs |
| Requirements are uniquely identified and traceable | SDLC / traceability | ID conventions; trace script in CI | `trace-matrix.csv` in release packet |
| Risk controls are implemented and verified | Risk mgmt | PR label `safety`; HAZ/RC links required | Hazard analysis diff + tests linked |
| Architecture changes are reviewed by qualified owners | SDLC / design control | CODEOWNERS on architecture docs | PR approvals by owners |
| Verification evidence exists for each requirement | V&V | CI required checks; tests linked to IDs | test reports + trace matrix |
| Release is reproducible and auditable | Config mgmt | signed tag; release packet generation | manifest.json + checksums + tag |
| Security risks assessed and controlled | Cybersecurity guidance | `security` label gates; scans required | SBOM + vuln scans + security tests |
| Third-party components controlled (SBOM) | Supplier/OSS | SBOM generation pipeline | `sbom.spdx.json` per release |
| Defects triaged with impact assessment | Problem resolution | issue template + labeling rules | issue record + linked PR |
| CAPA initiated when threshold met | CAPA | SOP-CAPA triggers from issues | CAPA record under QMS/Records |
| Training and role qualification maintained | QMS training | training records folder | `QMS/Records/Training/...` |
| Emergency changes controlled | Change control | hotfix SOP + post-hoc review | hotfix PR + deviation record |
| Validation executed when required | V&V | release gate requires validation summary | validation-summary.md + sign-off |
| AI use is governed and attributable | AI SOP | AI record required when used in controlled outputs | `QMS/Records/AI/...` |

Add rows as needed; keep it concise and enforceable.

---

## 13. Operating procedure (how work flows day-to-day)

1. Define/modify requirement (SWR/SR) → commit to `QMS/DHF/` with IDs.
2. Update hazard analysis if impact is safety/security → link HAZ/RC IDs.
3. Implement code on `feature/*`.
4. Open PR → apply `risk:*` label + `safety/security` as applicable.
5. CI runs → produces required evidence; reviewers validate.
6. Merge to `main` only when:
   - approvals satisfied (CODEOWNERS + risk gates)
   - required checks pass
   - trace is regenerated
7. Cut release tag `vX.Y.Z` → pipeline generates release packet → release approval record added.
8. Postmarket: issues/complaints → triage → CAPA if triggers met → fixes through PRs.

---

## 14. Definition of Done (DoD) for regulated increments

A change is “Done” only when:
- requirements/risk links are present (or explicitly not applicable)
- tests exist and pass; evidence is captured
- docs updated when behavior changes
- security evidence included when security-relevant
- trace is regenerated and consistent
- approvals satisfy risk gates

---

## 15. Appendix: recommended GitHub templates

- `.github/ISSUE_TEMPLATE/defect.yml` (requires impact assessment + version + repro)
- `.github/ISSUE_TEMPLATE/change_request.yml` (requires UN/SR/SWR linkage)
- `.github/workflows/ci.yml` (build/test/SAST/depscan/trace)
- `.github/workflows/release.yml` (release packet generator)
- `.github/pull_request_template.md` (above)
- `.github/CODEOWNERS` (above)

---

## 16. One-page “audit navigation” checklist

An auditor should be able to:
- open `QMS/DHF/DHF-00-Index.md`
- find the requirement, risk, architecture, cybersecurity, and V&V documents
- open `DHF-10-ReleaseHistory.md` and pick a release
- open the corresponding `Artifacts/ReleasePackets/vX.Y.Z/`
- verify traceability and objective evidence
- inspect PR history for key changes with required approvals and CI checks

End.
```
