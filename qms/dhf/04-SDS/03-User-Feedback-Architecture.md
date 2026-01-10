# 03-User-Feedback-Architecture

---
title: Okapi Echo Module Architecture
document_id: DHF-04-03
version: 1.0
status: DRAFT
owner: Lead Architect
created_date: 2026-01-10
trace_source: DHF-04
---

> Defines the architecture for the "Okapi Echo" feedback module, enabling context-aware user reporting to mitigate clinical disruption and improve usability.

## 1. Concept: "The Glass Scribe"

The Echo Module is a low-friction linguistic interface designed to capture user intent without breaking their clinical workflow. It decouples "complaining" from "ticket filing."

### 1.1 Core Philosophy
-   **No Forms**: Removes the cognitive load of categorizing issues (Bug vs. Feature vs. User Error).
-   **Context is King**: The user provides the *sentiment*; the system captures the *state*.
-   **Amber Distinction**: Visual cues (Amber color) distinguish system-meta interactions from patient-clinical interactions (Blue).

## 2. Architecture Components

### 2.1 The Anchor (Frontend)
-   **Location**: Fixed in the bottom-left of the Global Navigation Rail.
-   **Visual**: Circular icon (Microphone/Cursor hybrid).
-   **States**:
    -   *Idle*: Gray, unobtrusive.
    -   *Active*: Amber pulse (triggered by user or detected "rage clicks").
-   **Interaction**: Click or Hotkey (`~` Tilde).

### 2.2 The Context Capsule (Data Capture)
Upon activation, the system freezes and captures a state snapshot **before** the user even speaks.
-   **Metadata Payload**:
    -   `timestamp`: ISO-8601.
    -   `user_id`: Current active session.
    -   `viewport_state`: Zoom level, X/Y coordinates, active slide ID.
    -   `layer_state`: AI overlays enabled/disabled, active annotations.
    -   `system_health`: Latency metrics, last error code.

### 2.3 The Linguistic Interface
-   **Modal**: "Glassmorphism" overlay (blurring background context).
-   **Input**: Single input field supporting:
    -   **Voice**: Push-to-talk (Hold `Spacebar` or Click Mic) with streaming transcription.
    -   **Text**: Command-line style entry.
-   **Feedback Loop**: System acknowledges receipt ("Captured") without navigation.

### 2.4 Backend Processing (LLM Router)
*Note: Logical architecture phase.*
1.  **Ingestion**: Receives Text + Context Payload.
2.  **Analysis**: LLM classifies intent (Bug, Feature, Clinical Note).
3.  **Routing**:
    -   *Clinical Note*: To Case Notes (future scope).

### 2.5 Admin Dashboard (The Feedback Loop)
-   **Frustration Heatmap**: Visual representation of the UI highlighting areas generating efficient "rage clicks" or high report volume.
-   **Wishlist Cloud**: LLM-generated clusters of free-text feedback (e.g., "Search Latency", "Dark Mode Requests").

## 3. Deployment Strategy (Evolution)
-   **Phase 1 (Tech Focus)**: "Bug & Wishlist Catcher." The LLM acts as a Junior PM to categorize technical issues.
-   **Phase 2 (Clinical Focus)**: "The Clinical Scribe." As trust builds, the tool pivots to capturing operational/diagnostic notes (e.g., "Remind me to show Dr. Smith").

## 4. Visual Reference (Wireframe)
```text
+------------------------------------------------------------
|  [ Main Pathology Viewer Content Is Visible But Blurred ]
|
|  +-------------------------------------------------------+
|  |  ( ) Auto-Context: Case #1293 | Zoom: 40x | AI: ON    |  <-- Subtle Meta-data
|  +-------------------------------------------------------+
|  |                                                       |
|  |  "The heatmap is obscuring the mitosis..."            |  <-- User Input (Large)
|  |  _________________________________________            |
|  |                                                       |
|  |  [ ||||||||||||||||| ]  Listening...                  |  <-- Audio Waveform
|  +------------------+------------------------------------+
|                     |
|  [ (Amber Icon) ] --+  <-- Anchor Point
|
+------------------------------------------------------------
```

## 5. Risk Mitigation Strategy
-   **Addressed Hazard**: "Unreported Issues" leading to user frustration or hidden safety risks (RISK-USE-01).
-   **Mitigation**: Reduces threshold for reporting. Context capture allows engineering to reproduce bugs that users cannot articulate technically.

## 6. Traceability
-   **Implements**: SR-UX-01 (Feedback Mechanism), SR-UX-02 (Context Capture).
