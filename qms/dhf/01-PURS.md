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

> **Project rename notice (2026-04-08, v2):** This project was renamed from **Okapi** to **Starling**. An initial cosmetic rename retained structural identifiers; the full rename was completed on this date across Java packages (`com.starling.auth.*`), Spring configuration, database (`starling_auth`), Keycloak realm (`starling`), JWT issuer, protocol field names, seed group names (`Starling_*`), and documentation. Historical traceability of the Okapi name is preserved via git history and `qms/dhf/00-Index.md` revision history; no legacy Okapi identifiers remain.

# 1. Purpose
To define user needs for Starling modules in scope for the current baseline, including:
- Identity and Access Management (IAM) (authentication + authorization)
- Histology Asset Tracking (HAT)

# 2. Scope
This document covers user needs for:
- login, identity federation, and **role-based access** for clinical users of Starling
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
| **UN-AUTHZ-002** | The user needs the system to automatically reflect institutional group membership (IdP groups) into Starling roles without manual account provisioning. | Reduces admin burden and prevents inconsistent access assignments. |
| **UN-AUTHZ-003** | The user needs fine-grained permissions within roles (e.g., view vs. edit vs. sign-out) so that access is appropriately scoped to job function. | Supports least-privilege access; enables nuanced access models (e.g., resident can draft but not sign). |
| **UN-AUTHZ-004** | The user needs time-bounded role assignments for temporary access scenarios (e.g., coverage, training rotations, locum tenens). | Eliminates stale access from expired assignments; reduces admin burden of manual revocation. |
| **UN-AUTHZ-005** | The administrator needs a reliable, low-error way to grant and revoke access by managing centralized identity provider groups and Starling-local permission mappings (without creating credentials in Starling). | Reduces configuration mistakes and avoids "shadow accounts"; supports controlled access management aligned to institutional identity governance. |
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
| **UN-WL-008** | The user needs to search for cases by accession number, patient name, or MRN from the global navigation bar and navigate directly to a case from the results. | Enables rapid case lookup from anywhere in the application without navigating to the worklist first; reduces time-to-case for pathologists responding to clinical inquiries. |

## 3.7 Case Assignment (CA)

Pathologist assignment to cases is a source-of-truth relationship in the core data model, not merely a worklist display attribute. Assignment drives the worklist ("my cases"), audit attribution (who signed out this case), and downstream reporting (turnaround time by pathologist).

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-CA-001** | The user needs each case to have a designated primary pathologist who is responsible for the final diagnosis and sign-out. | Unambiguous diagnostic responsibility is a patient safety requirement and a regulatory expectation (CAP checklist, CLIA). Without a clear primary, cases may fall through cracks or have conflicting sign-outs. |
| **UN-CA-002** | The user needs to assign additional people to a case with a designation (primary, secondary, or consulting) that indicates their function on the case, independent of their organizational position. Any system user — pathologist, resident, fellow, or other staff — may be assigned. | Academic and community practices routinely involve residents drafting diagnoses for attending review, fellows performing subspecialty consults, and secondary pathologists providing coverage. The case designation (primary/secondary/consulting) is independent of the person's organizational role (pathologist/resident/fellow), which is already captured in their identity record. |
| **UN-CA-003** | The user needs the worklist to reflect pathologist assignments from the authoritative case record, ensuring consistency between the case data model and the work list display. | If the worklist maintains its own separate assignment independent of the case record, drift is inevitable — a pathologist may appear assigned on the worklist but not on the case, or vice versa. A single source of truth eliminates this category of error. |
| **UN-CA-004** | The user needs an audit trail of pathologist assignment changes (who was assigned, by whom, when) for accountability and regulatory compliance. | Assignment changes (e.g., case reassigned from a resident to an attending, or from one pathologist to another due to vacation coverage) must be traceable for quality assurance and accreditation reviews. |

## 3.8 IAM Administration (Admin)

User needs for the institutional administrator managing identities, roles, permissions, IdP mappings, grants, and audit. (Note: the provisioning workflow — user appears in Keycloak → identity syncs to Starling → admin reviews and assigns roles → user gains access — is addressed by UN-AUTHZ-002, UN-AUTHZ-006, and UN-ADMIN-001/002/003 collectively.)

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-ADMIN-001** | The administrator needs to view, search, and filter all user identities in the system to manage access across the institution. | Enables efficient user lookup for access troubleshooting, onboarding verification, and compliance auditing; prevents reliance on ad hoc database queries. |
| **UN-ADMIN-002** | The administrator needs to activate and deactivate user identities without deleting historical records. | Supports offboarding workflows (e.g., departed staff, expired affiliations) while preserving audit trail and case attribution integrity for regulatory defensibility. |
| **UN-ADMIN-003** | The administrator needs to assign and revoke roles for individual users with documented justification. | Supports local role overrides beyond IdP-derived assignments (e.g., temporary coverage, cross-training); justification requirement supports audit trail. |
| **UN-ADMIN-004** | The administrator needs to manage IdP group-to-role mappings to control how institutional directory groups translate to Starling permissions. | Enables centralized identity governance; reduces configuration drift between IdP groups and Starling roles; supports multi-tenant/multi-IdP scenarios. |
| **UN-ADMIN-005** | The administrator needs to view the role-permission matrix to understand what capabilities each role provides. | Supports access review, compliance reporting, and informed decision-making when assigning roles or responding to access requests. |
| **UN-ADMIN-006** | The administrator needs to view, filter, and export audit logs for security investigations and compliance reporting. | Supports HIPAA audit requirements, incident response, and institutional security monitoring without requiring direct database access. |
| **UN-ADMIN-007** | The administrator needs to view and revoke active break-glass and research access grants across all users. | Enables institutional oversight of emergency and research access; supports timely response to grant abuse or policy violations. |
| **UN-ADMIN-008** | The administrator needs to view and revoke trusted devices for any user in response to security incidents (e.g., lost/stolen devices, compromised accounts). | Supports incident response workflows; complements user self-service device management with institutional authority. |
| **UN-ADMIN-009** | The administrator needs a dashboard summarizing system state: active user counts, recent authentication activity, active emergency/research grants, and security-relevant events. | Provides situational awareness for system health and security posture; enables proactive identification of anomalies. |
| **UN-ADMIN-010** | An administrator who manages only user access needs to see only administrative functions; an administrator who also holds a clinical role (e.g., Pathologist) needs seamless access to both clinical and admin views within the same application. | Avoids cognitive overload for admin-only users; eliminates the need for dual-app login for clinician-administrators; supports least-privilege UI presentation. |
| **UN-ADMIN-011** | The administrator needs all administrative actions (identity changes, role assignments, mapping updates, grant revocations, device revocations) to be recorded in the audit log with actor attribution. | Supports non-repudiation of administrative actions; enables investigation of privilege changes; required for regulatory compliance. |

## 3.9 Orchestrator-Viewer Integration (OVI)

The Orchestrator (SvelteKit web client) manages a separate Digital Viewer browser window for slide examination. The two windows communicate via `postMessage` for same-browser coordination and via a WebSocket-based Session Awareness Service for cross-browser/cross-device awareness. The integration must be reliable enough for clinical use where a broken link between windows could contribute to patient safety errors.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-OVI-001** | The pathologist needs the viewer to open reliably from the worklist without manual intervention (no popup blocker failures, no lost windows). | A failed viewer launch interrupts diagnostic workflow and may force the pathologist to fall back to physical glass, causing delay. |
| **UN-OVI-002** | The pathologist needs the viewer to display the correct case immediately when launched from the worklist, without ambiguity about which case is loaded. | Case-image mismatch at launch is a patient safety risk (see RISK-001 in Viewer DHF). The orchestrator is the source of truth for case identity at launch time. |
| **UN-OVI-003** | The pathologist needs case switching from the worklist to propagate to the viewer reliably with explicit confirmation. | Clicking a new case in the worklist while examining another in the viewer is a high-risk moment for mismatch. The transition must be unambiguous. |
| **UN-OVI-004** | The pathologist needs authentication tokens to be provisioned to the viewer transparently and refreshed without interrupting slide examination. | The viewer cannot access the tile server or case APIs without a valid JWT. Token expiry during sign-out is a workflow disruption that may cause loss of unsaved annotations or force a redundant login. |
| **UN-OVI-005** | The pathologist needs the viewer to remain fully functional for its current case if the orchestrator connection is temporarily lost (e.g., orchestrator tab closed, browser crash, network glitch). | A rigid dependency where the viewer stops working the moment the orchestrator disconnects would be unacceptable in clinical use. The pathologist must be able to complete the current case examination. |
| **UN-OVI-006** | The pathologist needs the orchestrator and viewer to resynchronize automatically when communication is restored, without manual intervention. | Requiring the pathologist to close and reopen the viewer or re-navigate to the current case after a momentary disconnection is disruptive and error-prone. |
| **UN-OVI-007** | The pathologist needs clear visual indication in both windows when the cross-window link is degraded or lost. | Silent communication failures are dangerous — the pathologist may assume the orchestrator and viewer are synchronized when they are not. |
| **UN-OVI-008** | The pathologist needs the Session Awareness Service (multi-user focus awareness) to be non-critical — its unavailability must not prevent case viewing or signing out. | The session service provides collaborative awareness ("Dr. Smith is also viewing this case"), which is valuable but not essential for individual diagnostic work. Coupling core clinical workflow to a WebSocket service would create an unacceptable single point of failure. |

## 3.10 Educational WSI Collections (EDU)

Educational slide collections serve teaching, training, competency assessment, and continuing education. They share the clinical slide data model (case → part → block → slide with integrity verification) but have distinct requirements around curation, annotation, de-identification, and access control. The educational collection operates in a separate `wsi_edu` schema to enforce isolation from clinical data while preserving structural consistency.

### 3.10.1 Collection Management

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-EDU-001** | The curator needs to ingest slides into a dedicated educational collection that is structurally identical to the clinical collection (case → part → block → slide) but isolated from clinical data. | Structural consistency enables the same viewer, tile server, and navigation to work for both collections. Isolation prevents accidental mixing of clinical and educational data, which could lead to diagnostic errors or privacy violations. |
| **UN-EDU-002** | The curator needs to transfer a clinical slide to the educational collection in a single operation, preserving the anatomic hierarchy (part, block, slide) while severing all patient-identifying links. | Teaching slides often originate from interesting clinical cases. The transfer must be seamless (low friction encourages faculty to contribute) and safe (de-identification must be automatic, not manual). |
| **UN-EDU-003** | The curator needs the system to assign educational accession numbers (EDU prefix + year + sequential number, e.g., EDU26-00001) automatically on ingestion, independent of any clinical accession. | A consistent, predictable identifier scheme enables teaching faculty to reference cases in syllabi, exam keys, and publications. The year component provides chronological context for the teaching collection. |
| **UN-EDU-004** | The curator needs educational cases to carry the full anatomic hierarchy (part description, block description) with a provenance indicator showing whether the hierarchy was inherited from a clinical case, implied by the system, or curated by a human. | Teaching pathologists need to know where the tissue came from. A resident studying a breast lumpectomy slide needs to know it's from "Part A: Right breast, lumpectomy, 2.1 cm mass" — not just "breast." The provenance indicator (ACCESSIONED, IMPLIED, CURATED) communicates the confidence level. |
| **UN-EDU-005** | The curator needs to upload de-identified slides from external sources (USB drives, conference downloads, TCGA) with the system automatically creating the full case/part/block/slide hierarchy using implied defaults. | Teaching collections grow from multiple sources. External slides arrive without case structure. The system must accommodate them without requiring the curator to manually create database records — a task that would discourage contributions. |
| **UN-EDU-006** | The curator needs educational cases to carry ICD codes, specimen type, and diagnostic metadata identical in structure to clinical cases, enabling search, filtering, and curriculum organization by diagnosis. | "Find all cases of papillary thyroid carcinoma in the teaching collection" is a fundamental query for course preparation. Without structured diagnostic coding, the curator falls back to free-text search, which is unreliable and inconsistent. |

### 3.10.2 Curation and Curator Roles

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-EDU-007** | The curator needs to be recorded as the person responsible for an educational case, analogous to the primary pathologist on a clinical case, with the ability to assign additional curators. | Accountability for teaching content quality. When a resident finds an error in a teaching case annotation, they need to know whom to contact. Multiple curators support team-maintained collections (e.g., a department's dermatopathology teaching set maintained by several faculty). |
| **UN-EDU-008** | The system needs to support curators who are not pathologists (PAs, residents, fellows, educators, research staff) since teaching collection management is not restricted to attending pathologists. | In practice, residents often build study sets, PAs curate grossing teaching collections, and non-physician educators manage histology courses. Restricting curation to pathologists would exclude the people who do much of this work. |
| **UN-EDU-009** | The curator needs to edit the anatomic hierarchy, diagnostic metadata, and case descriptions of educational cases they curate, with changes tracked for provenance. | Teaching cases evolve. Initial implied metadata from a clinical transfer may need correction. A curator might reclassify a case or update the diagnosis after a consensus conference. Changes must be tracked so the collection's provenance history remains intact. |

### 3.10.3 Teaching Annotations

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-EDU-010** | The teaching pathologist needs to create annotations on educational slides with visibility levels that control who sees them and when: personal (study notes), teaching (shown during instruction), and public (always visible to all users with access). | Different annotation audiences serve different purposes. A pathologist preparing for a lecture creates personal markup notes. During the lecture, teaching annotations highlight diagnostic features for students. Public annotations serve as permanent reference for self-study. Mixing these creates confusion and privacy concerns. |
| **UN-EDU-011** | The annotation author needs to be recorded as the provenance of each annotation, separately from the case curator, since the person who annotated a slide may differ from the person who curated the case. | A department chair might curate the teaching collection (selecting cases, organizing curricula) while individual faculty members annotate slides in their subspecialty. Attribution must reflect this — both for academic credit and for resolving conflicting annotations. |
| **UN-EDU-012** | The teaching pathologist needs annotations to support spatial markup (regions, points, arrows, measurements) with labels and descriptive text, using the same geometry model as clinical annotations. | Teaching annotations must be spatially precise — "the area of invasion is here, the mitotic figure is there." Reusing the clinical annotation geometry (GeoJSON in level-0 pixel coordinates) ensures the viewer renders them identically and avoids maintaining two annotation systems. |
| **UN-EDU-013** | The instructor needs to selectively enable or disable teaching annotations during a session so that students can first examine the slide independently before annotations are revealed. | This is the pedagogical "reveal" pattern: students look at the slide, form their own impression, and then the instructor shows the annotated version for comparison. If teaching annotations are always visible, this teaching method is impossible. |

### 3.10.4 Integrity and Security

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-EDU-014** | The system needs to apply the same cryptographic integrity verification (HMAC-SHA256) to educational slides as to clinical slides. | A tampered educational slide used for competency assessment could lead to incorrect training outcomes. A resident who learns from a corrupted image may develop incorrect diagnostic patterns. Integrity verification is not just a clinical concern — it's a training quality concern. |
| **UN-EDU-015** | The system needs to strip patient-identifying metadata from image file headers (patient name, accession number, scan dates, institution name, barcode images) before or during ingestion into the educational collection. | HIPAA Safe Harbor de-identification requires removal of 18 identifier categories. Slide file headers (particularly SVS ImageDescription tags and embedded label images) routinely contain patient names and accession numbers. Failure to strip these creates a privacy violation that could expose the institution to regulatory action. |
| **UN-EDU-016** | The curator needs the educational collection to be accessible independently of clinical system permissions so that teaching faculty and trainees can access teaching slides without requiring clinical case access. | Residents on teaching rotations, visiting students, and continuing education participants need access to teaching slides without being granted clinical system access. Coupling educational access to clinical permissions would either over-grant access (privacy risk) or under-grant it (blocking legitimate educational use). |

### 3.10.5 Collection Organization and Discovery

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-EDU-017** | The teaching pathologist needs to organize educational cases into named collections (e.g., "Breast Pathology Board Review", "Resident Orientation 2026", "Dermatopathology Unknowns") for curriculum delivery. | Teaching is organized by course, not by individual case. A pathologist preparing a tumor board needs to pull together 10 cases from across the collection into a presentation sequence. Without named collections, this requires manual bookkeeping outside the system. |
| **UN-EDU-018** | The user needs to search the educational collection by diagnosis, anatomic site, ICD code, stain type, and free-text across case descriptions and annotation labels. | "Find me a good example of granulomatous inflammation in the lung" is the canonical teaching search. Every dimension — diagnosis, site, stain, text — matters. The search must be fast enough for live use during conferences and teaching sessions. |
| **UN-EDU-019** | The user needs to record the source lineage of an educational case: whether it originated from a clinical case (with source case ID recorded but patient identity severed), an external upload, or a public dataset (e.g., TCGA). | Provenance matters for institutional compliance (can we use this slide in a publication?), for quality (was this slide vetted by our pathologists or downloaded from the internet?), and for re-identification risk management (if we ever need to trace a de-identified slide back to its source, the honest broker can do so via the lineage record). |

## 3.11 Image Ingestion (ING)

Image ingestion is the mechanism by which whole-slide images enter the system and become linked to the correct records in the database so the tile server can serve them. This covers both clinical and educational collections and supports two entry points: an HTTP API for programmatic/UI-driven upload, and a CLI tool for scripted seeding and batch operations. Both entry points call the same core ingestion logic.

| ID | User Need Statement | Rationale/Clinical Justification |
| :--- | :--- | :--- |
| **UN-ING-001** | The operator needs to add whole-slide images into the system without using the UI, via either an API call or a command-line tool. | Scanners produce images asynchronously. Manual UI upload does not scale. Operators and developers need a non-interactive path for loading slides — both for initial data seeding and for production scanner integration. |
| **UN-ING-002** | The operator needs the ingestion process to validate the image file (format, completeness, readability) before registering it in the database. | A corrupt or incompatible image that enters the database without validation will cause tile server errors at view time. Catching bad files at ingestion prevents downstream failures during clinical use. |
| **UN-ING-003** | The operator needs the ingestion process to associate an incoming image with the correct patient, case, part, block, and slide records — creating missing hierarchy records when authorized. | An image without database linkage is invisible to the tile server and the worklist. The hierarchy must exist for the image to be servable. In seed/bootstrap mode, the hierarchy may not yet exist and must be created atomically with the image. |
| **UN-ING-004** | The operator needs the ingestion process to compute and store a cryptographic integrity hash (HMAC-SHA256) at ingestion time so that tampering or corruption can be detected later. | Per SDS-STR-001 §5, integrity verification depends on having a baseline HMAC computed at the moment the file enters managed storage. Without this, the verification sweep has nothing to compare against. |
| **UN-ING-005** | The operator needs the ingestion process to extract image metadata (dimensions, magnification, microns-per-pixel, scanner) automatically from the file, rather than requiring manual entry. | Manual metadata entry is error-prone and impractical at scale. The large_image library can extract this from file headers for all supported formats. |
| **UN-ING-006** | The operator needs to ingest a batch of slides from a manifest file (JSON) in a single operation, with each slide's metadata and hierarchy specified in the manifest. | Seeding 10–50 test cases one slide at a time is tedious and error-prone. A manifest-driven batch mode enables repeatable, auditable initial data loading and supports CI/CD test data provisioning. |
| **UN-ING-007** | The operator needs failed ingestions to leave the system in a clean state: no orphaned files on disk, no partial database records, no corruption of existing data. | A half-ingested slide (file on disk but no DB record, or DB record but no file) creates an inconsistent state that is difficult to diagnose and recover from. Ingestion must be atomic. |
| **UN-ING-008** | The operator needs the ingestion process to reject duplicate slides (same slide_id or same file at the same path) with a clear error rather than silently overwriting existing data. | Duplicate ingestion could overwrite a verified slide with an unverified copy, breaking the integrity chain. Explicit rejection with a clear message is safer than silent replacement. |

# 4. Notes
System requirements are defined in `qms/dhf/02-SRS.md` and verified per `qms/dhf/06-VVP.md`.