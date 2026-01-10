# 05b-Hazard-Analysis

---
title: Hazard Analysis (FMEA)
document_id: DHF-05b
version: 1.1
status: DRAFT
owner: Safety Officer
created_date: 2026-01-09
risk_plan_ref: DHF-05a (Risk Management Plan)
---

# 1. Introduction
This document records the specific hazards, failure modes, and risk controls for the Okapi system. The risk acceptability matrix and scoring definitions (e.g., what "Severity 5" means) are defined in **[DHF-05a]**.

# 2. Risk Analysis Matrix

| Risk ID | Hazard / Failure Mode | Cause of Failure | S | P | Risk Level | Risk Control Measure | New S | New P | New Level |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **RISK-001** | Unauthorized access to User Account / PHI | Phishing, weak password, or credential stuffing | 5 | 3 | **15 (High)** | **Requirement SYS-AUTH-003**: System shall enforce MFA. | 5 | 1 | **5 (Low)** |
| **RISK-002** | AI Suggestion Error (Incorrect Diagnosis) | Model bias, training data gap, or edge case pathology | 4 | 3 | **12 (Med)** | **Design Control**: "Clinician-in-the-loop" UI; requirement that all AI suggestions must be explicitly confirmed by a pathologist. | 4 | 1 | **4 (Low)** |
| **RISK-003** | Data Corruption during Write-back to Epic | Network glitch or HL7/FHIR mapping error | 4 | 2 | **8 (Med)** | **Requirement IR-001**: Checksums and ACK/NACK validation for all Epic transactions. | 4 | 1 | **4 (Low)** |
| **RISK-004** | Loss of Connectivity (Hospital ↔ Cloud) | AWS outage or Hospital Firewall changes | 3 | 3 | **9 (Med)** | **Requirement SYS-REL-001**: Local cache of critical pending results; Retry logic for data sync. | 3 | 1 | **3 (Low)** |
| **RISK-005** | Unauthorized Modification of AI Model | Insider threat or compromised CI/CD pipeline | 5 | 2 | **10 (Med)** | **SOP-ChangeControl**: Mandatory code reviews and Signed commits; **SYS-SEC-001**: Read-only model weights in production. | 5 | 1 | **5 (Low)** |
| **RISK-006** | Audit Log Failure / Tampering | Disk full or malicious log deletion | 3 | 2 | **6 (Low)** | **Requirement SYS-AUD-001**: Use of immutable cloud logging (AWS CloudWatch/S3 with Object Lock). | 3 | 1 | **3 (Low)** |
| **RISK-007** | Unreported System Errors / User Frustration | Complex bugs or usability issues are not reported, leading to "Shadow Workarounds" or safety risks. | 4 | 3 | **12 (Med)** | **Requirement SR-UX-01**: Implement "Echo" module for context-aware reporting; **SR-UX-02**: Auto-context capture. | 4 | 1 | **4 (Low)** |

# 3. Conclusion
All identified risks are mitigated to an acceptable level (Low) through the implementation of the specified functional requirements and design controls.