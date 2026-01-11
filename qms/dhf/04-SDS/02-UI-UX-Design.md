# 02-UI-UX-Design

---
title: User Interface & Experience Design Specification
document_id: DHF-04-02
version: 1.0
status: DRAFT
owner: Lead Designer
created_date: 2026-01-10
trace_source: DHF-04
---

> Detailed UI/UX specifications for the Okapi platform, focusing on the "Clinical Threshold" aesthetic.

## 1. Design Philosophy: The "Clinical Threshold"

The user interface represents a transition from the chaotic outside world into a sterilized, high-tech control room.

-   **Theme**: "Clinical Dark Mode"
    -   **Vibe**: Calm, Professional, Low-Glare, Precision.
    -   **Primary Background**: Gunmetal / Deep Charcoal (Not Pitch Black).
    -   **Accents**: Subtle glowing data nodes, muted clinical blues and teals.
    -   **Typography**: Clean Modern Sans-Serif (e.g., Inter, Roboto).

## 2. Artifact 1: Split-Screen Landing & Login

The entry point (Landing Page) serves strictly as the Login Portal.

### 2.1 Layout (Split 50/50)
-   **Left Half (Brand Canvas)**:
    -   **Background**: Abstract "Digital H&E" visualization. Dark tissue section with glowing cell nuclei connected by network lines.
    -   **Content**:
        -   **Logo**: "Okapi" (Top Left, White).
        -   **Headline**: "Orchestrating Precision Diagnostics."
        -   **Sub-headline**: "Unified workflow for Pathology, AI, and Search."
-   **Right Half (Secure Gate)**:
    -   **Background**: Solid Deep Charcoal/Gunmetal.
    -   **Content (Vertically Centered)**:
        -   **Headline**: "Welcome Back."
        -   **Primary Action**: Large "Continue with Institutional ID (SSO)" button (Microsoft/Okta).
        -   **Secondary Action**: "Or sign in with email" (Reveals username/password fields).

## 3. Artifact 2: User Profile & Personalization

Identity is central for audit trails and RBAC.

### 3.1 The Avatar Badge
-   **Location**: Top Right Header.
-   **Visual**: Circular badge.
-   **States**:
    -   **Default**: Two-letter monogram (e.g., "EL") on role-based color (Blue=Pathologist, Teal=Manager).
    -   **Personalized**: User photo.

### 3.2 Profile Dropdown
-   **Header**: Full Name + Role (e.g., "Dr. Elena Li, Attending Surgical Pathologist").
-   **Quick Toggles**:
    -   **Theme**: Dark/Light Mode.
    -   **Focus Mode**: Auto-hide sidebars.
-   **Menu Items**: Settings, Keyboard Shortcuts, Sign Out.

## 4. Artifact 3: Application Shell (The "Zero State")

The structural frame of the application once logged in.

### 4.1 Layout Grid
```
+-----------------------------------------------------------------------+
|  [A] TOP GLOBAL NAVIGATION BAR (Fixed Height: 60px)                   |
+-------+---------------------------------------------------------------+
|       |                                                               |
| [B]   |                                                               |
| LEFT  |                                                               |
| NAV   |                                                               |
| RAIL  |                 [D] MAIN CONTENT AREA (Canvas)                |
| (Slim)|                                                               |
|       |                                                               |
|       |                                                               |
|       |                                                               |
+-------+---------------------------------------------------------------+
```

### 4.2 [A] Top Global Navigation Bar
-   **Left**: Okapi Wordmark (Subtle).
-   **Center**: Global Omni-Search Bar ("Search Cases, Patients, or Archive...").
-   **Right**: User Avatar Badge.

### 4.3 [B] Left Navigation Rail
-   **Style**: Slim, Icon-based (tooltips on hover).
-   **Icons (Top to Bottom)**:
    1.  **Dashboard** (Home plate) - *Default Active*
    2.  **Worklist** (Stacked papers)
    3.  **Archive Search** (Magnifying glass over cylinder)
    4.  **AI/Analytics** (Neural node)
-   **Bottom**: Settings (Gear).

### 4.4 [D] Main Content Area (Zero State)
-   **Visual**: Clean, dark gray canvas.
-   **"Zero State Hero" Component** (Centered):
    -   **Icon**: Large, faint "Waiting" / "Empty Tray" icon.
    -   **Headline**: "Ready to Orchestrate."
    -   **Sub-text**: "Select a worklist from the left, or use the search bar above to begin your session."
    -   **CTA**: "Go to My Daily Worklist".

## 5. Traceability
-   **Requirements**: Implements PURS-UI-001 (Professional UI), PURS-UI-002 (Dark Mode).
-   **Risks**: Mitigates RISK-UI-001 (User Fatigue/Glare) via Dark Mode.

## 6. Theme System Specification

### 6.1 Theme Modes
-   **System**: (Default) Mirrors the OS `prefers-color-scheme`.
-   **Dark**: Forces the "Clinical Dark" palette.
-   **Light**: Forces the "Clinical Light" palette (High contrast, paper-white backgrounds).

### 6.2 Selection & Persistence
-   **Entry Point**: User Avatar Dropdown -> "Appearance".
-   **Persistence**:
    1.  **Server**: Stored in User Profile (`theme_preference`).
    2.  **Local**: Cached in `localStorage` for immediate paint on load.
-   **Reconciliation**: Local cache applies first (anti-flash), then updates if server profile differs.

