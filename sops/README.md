# QMS-Core: Reusable Quality Management System Module

## Overview

`qms-core` is a **project-agnostic** Quality Management System (QMS) module designed to be reused across multiple medical device software projects. It contains foundational Standard Operating Procedures (SOPs) and guidelines that apply universally to any SaMD (Software as a Medical Device) development effort.

## Purpose

This module provides:

1. **Standardized SOPs** — Procedures that define *how* to do things consistently across all projects
2. **Regulatory Alignment** — Pre-built compliance with ISO 13485, ISO 14971, IEC 62304, and FDA 21 CFR 820.30
3. **Reusability** — A single source of truth that can be referenced or symlinked from project-specific QMS directories

## Directory Structure

```
qms-core/
├── README.md                    # This file
└── sops/                        # Standard Operating Procedures
    └── SOP-DHF-Management.md    # Design History File management workflow
```

## Relationship to Project-Specific QMS

```
Project Repository
├── qms/                         # Project-specific QMS artifacts
│   ├── dhf/                     # Design History File (project-specific)
│   │   ├── 00-Index.md
│   │   ├── 01-PURS.md
│   │   ├── 02-SRS.md
│   │   └── ...
│   ├── sops/                    # Project-specific SOPs (if any)
│   └── templates/               # Document templates
│
└── qms-core/                    # Reusable QMS module (this directory)
    └── sops/                    # Universal SOPs
        └── SOP-DHF-Management.md
```

### Separation of Concerns

| Location | Contains | Scope |
|:---------|:---------|:------|
| `qms-core/sops/` | Universal procedures (DHF management, design controls) | All projects |
| `qms/sops/` | Project-specific procedures (Git workflow, deployment) | This project only |
| `qms/dhf/` | Design History File artifacts | This project only |
| `qms/templates/` | Document templates | This project only |

## Usage

### Option 1: Direct Inclusion (Current)

Include `qms-core/` directly in your project repository. Reference SOPs from your project's DHF Index.

### Option 2: Git Submodule (Recommended for Multi-Project)

```bash
# Add qms-core as a submodule
git submodule add <qms-core-repo-url> qms-core

# Update to latest version
git submodule update --remote qms-core
```

### Option 3: Symbolic Link

```bash
# Link to a shared qms-core directory
ln -s /path/to/shared/qms-core ./qms-core
```

## SOPs Included

### SOP-DHF-Management (SOP-DHF-001)

**Purpose**: Defines the complete workflow for creating and maintaining a Design History File.

**Key Topics**:
- DHF directory structure and naming conventions
- Design Control Waterfall (User Needs → Requirements → Design → Verification)
- Traceability requirements
- Agile/Sprint integration (micro-waterfall approach)
- Risk management (including P1/P2 probability split for software)
- Independent review requirements
- Software-specific requirements (SaMD)
- Unresolved anomalies documentation
- FDA and EU MDR regulatory considerations

## Synchronization with Project SOPs

When using `qms-core`, ensure your project-specific SOPs are harmonized:

| qms-core SOP | Project SOP | Relationship |
|:-------------|:------------|:-------------|
| SOP-DHF-Management | SOP-DocControl | DHF-Management defines *what* documents; DocControl defines *how* to version/approve |
| SOP-DHF-Management | SOP-ChangeControl | DHF-Management references change control for approvals |
| SOP-DHF-Management | SOP-SDLC | DHF-Management integrates with SDLC for sprint-based updates |
| SOP-DHF-Management | SOP-RiskMgmt | DHF-Management references risk process; RiskMgmt defines detailed risk procedures |

## Versioning

`qms-core` follows semantic versioning:
- **Major**: Breaking changes to SOP structure or requirements
- **Minor**: New SOPs or significant additions
- **Patch**: Clarifications, typo fixes, minor updates

Current Version: **1.0.0**

## Contributing

Changes to `qms-core` SOPs require:
1. Pull Request with detailed rationale
2. Independent review by QA or Regulatory Affairs
3. Impact assessment on all projects using this module
4. Version bump and changelog update

## Regulatory Notes

This module is designed to support compliance with:
- **ISO 13485:2016** — Quality management systems for medical devices
- **ISO 14971:2019** — Application of risk management to medical devices
- **IEC 62304:2006+A1:2015** — Medical device software lifecycle processes
- **21 CFR 820.30** — FDA Design Controls
- **EU MDR 2017/745** — Medical Device Regulation (Technical File requirements)

**Note**: Using these SOPs does not guarantee regulatory compliance. Each project must ensure proper implementation and maintain objective evidence of compliance.

## License

This QMS module is provided as part of the Okapi project. See the root LICENSE file for terms.
