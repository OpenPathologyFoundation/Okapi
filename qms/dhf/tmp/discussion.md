Below is a write-up you can drop into your product/spec repository. It is intentionally explicit (requirements, behaviors, acceptance criteria) and framed in user needs and clinical/business workflows.

---

# UI Specification: Theme Selection (System / Dark / Light) With Persistent User Preference

## 1. Purpose and Rationale

Okapi is a daily-use clinical/business application used in variable environments (office, reading room, home, different monitors). A single forced theme (dark-only or light-only) creates avoidable friction and can materially impair usability for a subset of users due to ambient-light conditions, visual comfort, and accessibility needs.

This feature provides:

* **User autonomy**: users can choose the display mode that best supports their work context.
* **Consistency**: the user’s preference follows them across devices and sessions.
* **Accessibility**: ensures legibility and operability for keyboard and low-vision users.
* **Operational efficiency**: reduces “UI complaints” and removes a common adoption barrier.

## 2. Scope

Implement:

* Theme modes: **System / Dark / Light**
* Preference persistence: **server-side** user profile + **client cache** for fast first paint
* Toggle location: **User avatar menu (top-right)** and **Settings page**
* Accessibility pass: **contrast**, **focus outlines**, **tooltips** for icon-only nav

Out of scope (for this iteration):

* Per-feature or per-pane theming (e.g., viewer vs worklist different themes)
* Custom theme palettes beyond dark/light
* High-contrast mode (can be added later as an accessibility extension)

## 3. User Needs and Scenarios

### User Need 1: Comfortable visibility across lighting conditions

* **Scenario A (bright environment):** user works in a bright office; dark mode feels low-contrast and “muddy.”
  **Need:** a **Light** option for crisp readability of tables and text.
* **Scenario B (low-light environment):** user works in a reading room; light mode causes glare.
  **Need:** a **Dark** option that reduces perceived glare and supports sustained viewing.

### User Need 2: Consistency across devices and sessions

* **Scenario:** user switches between workstation and laptop; expects the UI to remain consistent.
  **Need:** preference persists in the user profile (server-side) and loads immediately.

### User Need 3: Default behavior aligned with OS preference

* **Scenario:** a new user logs in; expects the app to match their system theme preference.
  **Need:** default mode should be **System** unless user explicitly chooses otherwise.

### User Need 4: Accessibility for keyboard and low-vision workflows

* **Scenario:** user navigates quickly by keyboard; must see focus location clearly.
  **Need:** visible focus outlines and clear selected states in both themes.
* **Scenario:** icon-only nav must be understandable without relying on memory.
  **Need:** tooltips and accessible labels for icon buttons.

## 4. Functional Requirements

### 4.1 Theme Modes

The application shall support three theme modes:

1. **System**

* Follows OS/browser preference using `prefers-color-scheme`.
* If OS preference changes while app is open, UI updates accordingly **only when mode = System**.

2. **Dark**

* Forces dark theme regardless of system setting.

3. **Light**

* Forces light theme regardless of system setting.

**Default:** System.

### 4.2 Theme Toggle Entry Points

#### A) Top-right user avatar menu (primary, immediate access)

* Add menu item: **Appearance** (or **Theme**).
* On selection, present a compact selector:

  * **System**
  * **Dark**
  * **Light**
* The currently active mode must be visibly indicated (checkmark or radio).

#### B) Settings page (secondary access)

* Include the same selector under a section:

  * “Appearance” → “Theme”
* Settings must reflect the current persisted preference.

### 4.3 Persistence and Loading Behavior

**Server-side source of truth**

* Theme preference stored on the user profile (e.g., `ui_theme_mode = system|dark|light`).
* On login/session initialization, backend returns user profile including theme preference.

**Client cache for fast paint**

* Client stores last-selected mode locally (e.g., localStorage) to prevent a “flash” of wrong theme while awaiting profile fetch.
* On app start:

  1. Apply cached theme immediately if present.
  2. Fetch user profile.
  3. If server preference differs from cache, update UI to server preference and update cache.

**Offline / error handling**

* If profile fetch fails, app continues using cached theme or System default.
* Do not block UI on profile load.

### 4.4 UI Application Mechanism

* The theme should be applied via a single root attribute/class (e.g., `<html data-theme="dark">` or `class="theme-dark"`).
* All components must derive colors from design tokens (no hard-coded colors in component logic).

### 4.5 Accessibility Requirements

* **Contrast:** All text and icons must meet WCAG contrast targets appropriate for UI text (at minimum, normal text should meet a standard contrast baseline; headings/large text may have different thresholds).
* **Focus indicators:** Visible focus outlines for all interactive elements using keyboard navigation in both themes.
* **Icon navigation tooltips:**

  * Left-side icon-only nav must have tooltips on hover and on keyboard focus.
  * Each icon button must have an accessible label (ARIA label) matching tooltip text.
* **Selected state clarity:** Selected menu item and selected worklist/nav state must be unambiguous in both themes (not color-only).

## 5. Non-Functional Requirements

* **Performance:** Theme should render correctly on first paint; minimize perceptible theme flicker.
* **Consistency:** Theme tokens applied consistently across pages (worklist, search, case view, settings).
* **Maintainability:** Use centralized design tokens and theming system; avoid duplication.

## 6. Acceptance Criteria (Testable)

### Theme selection and persistence

1. User can change theme from avatar menu in ≤2 clicks.
2. User can change theme from Settings page and see it reflected immediately.
3. Theme preference persists across logout/login and across devices (server-side).
4. On cold start, UI uses cached preference immediately (no obvious flash) and then reconciles with server preference.
5. When mode is System, changing OS theme changes app theme without user interaction.
6. When mode is Dark or Light, changing OS theme does **not** affect app theme.

### Accessibility and usability

7. All interactive elements show a visible focus outline in both themes.
8. Left navigation icons show tooltips on hover and keyboard focus.
9. Tooltip text matches ARIA label for each icon.
10. Primary text and key UI controls are legible in both themes (contrast check passes).
11. Selected states (nav, menu radio/checkmarks) are visually distinct without relying solely on color.

### Resilience

12. If user profile fetch fails, app still renders using cached theme or System default.
13. If local cache is missing, app renders using System until server preference arrives.

## 7. Implementation Notes (Guidance, not requirements)

* Prefer “System” default for first-run users.
* Consider placing the theme toggle under an “Appearance” submenu to avoid clutter.
* Use a single theme provider to avoid per-component divergence.
* Include a small automated UI test (Cypress/Playwright) verifying mode switching and persistence.


