# Software Design Specification — Orchestrator-Viewer Integration Architecture

---
document_id: SDS-OVI-001
title: Starling Orchestrator — Digital Viewer Integration Architecture (formerly Okapi)
version: 1.0
status: DRAFT
owner: Engineering
created_date: 2026-02-28
effective_date: TBD
trace_source: SRS-001 (System Requirements SYS-OVI-*)
trace_destination: VVP-001 (Verification Tests), SDS-FDP-001 (Viewer FDP Architecture), SDS-VWR-001 (Viewer Core Architecture)
references:
  - SDS-VWR-001 (Viewer Core Architecture — §5.2 Parent-Child Window Communication)
  - SDS-FDP-001 (Focus Declaration Protocol Architecture — §6.2 Session Awareness)
  - IEC 62304:2006+A1:2015 (Section 5.4 — Software Architectural Design)
---

> **Project rename notice (2026-04-08, v2):** This project was renamed from **Okapi** to **Starling**. An initial cosmetic rename retained structural identifiers; the full rename was completed on this date across Java packages (`com.starling.auth.*`), Spring configuration, database (`starling_auth`), Keycloak realm (`starling`), JWT issuer, protocol field names, seed group names (`Starling_*`), and documentation. Historical traceability of the Okapi name is preserved via git history and `qms/dhf/00-Index.md` revision history; no legacy Okapi identifiers remain.

## 1. Purpose

This document specifies the architecture for the integration between the Starling web client (orchestrator, `:5173`) and the Digital Viewer (`:5174`). The integration uses two independent communication channels: a synchronous `postMessage` bridge for same-browser window coordination, and an asynchronous WebSocket connection to the Session Awareness Service for cross-browser/cross-device awareness.

The architecture must be reliable enough for clinical diagnostic use. The primary design concern is that a communication failure between the orchestrator and viewer must never cause patient safety harm (case-image mismatch, loss of diagnostic work) or abrupt workflow disruption.

## 2. Architectural Overview

### 2.1 System Context

```
                    ┌─────────────────────────────────────────┐
                    │        BROWSER (same origin via nginx)   │
                    │                                         │
                    │  ┌─────────────┐   ┌─────────────────┐  │
                    │  │ Orchestrator│   │ Digital Viewer  │  │
                    │  │ (SvelteKit) │   │ (Svelte 5)      │  │
                    │  │ :5173       │   │ :5174           │  │
                    │  │             │   │                 │  │
                    │  │ ViewerBridge│◀─▶│OrchestratorBridge│ │
                    │  │             │   │                 │  │
                    │  └──────┬──────┘   └────────┬────────┘  │
                    │         │ postMessage        │           │
                    │         └────────┬───────────┘           │
                    └─────────────────┬────────────────────────┘
                                      │
                              ┌───────┴───────┐
                        WSS   │               │  HTTPS
                    ┌─────────┴───┐     ┌─────┴──────┐
                    │ Session     │     │ Auth System │
                    │ Awareness   │     │ :8080       │
                    │ :8765       │     │ (JWT issuer)│
                    └─────────────┘     └────────────┘
```

### 2.2 Two-Channel Design

| Channel | Technology | Scope | Criticality | Failure mode |
|:--------|:-----------|:------|:------------|:-------------|
| **Bridge** (Channel 1) | `window.postMessage` | Same browser, same user, synchronous | **Critical** — carries case context, JWT, lifecycle | Viewer operates standalone with last-known context |
| **Session Service** (Channel 2) | WebSocket (ws/wss) | Cross-browser, cross-device, multi-user | **Non-critical** — provides collaborative awareness | Multi-case warning disabled; all other functions unaffected |

This separation is fundamental to the reliability model. Channel 1 failure degrades to standalone viewer operation. Channel 2 failure degrades to single-user mode. Neither failure prevents clinical work.

## 3. Design Decisions

### 3.1 `postMessage` over SharedWorker or BroadcastChannel

**Decision**: Use `window.postMessage` with a retained `window.open()` reference for the bridge.

**Rationale**:
- `postMessage` with a direct window reference provides the most deterministic delivery model — the sender knows exactly which window receives the message
- `BroadcastChannel` is many-to-many and lacks target specificity; it would require additional filtering logic and cannot distinguish between two viewer windows
- `SharedWorker` adds a third process that can fail independently, increasing the failure surface
- The orchestrator already holds the `WindowProxy` reference from `window.open()`, making `postMessage` the natural choice

**Trade-off**: If the `WindowProxy` reference is lost (e.g., orchestrator page reloads), the bridge cannot be re-established without the viewer detecting the orchestrator window and re-registering. This is handled by the reconnection protocol (§6.4).

### 3.2 JWT Lifetime: 30+ Minutes, Not 5 Minutes

**Decision**: The case-scoped JWT provisioned to the viewer shall have a minimum lifetime of 30 minutes, with proactive refresh at 75% of lifetime.

**Rationale**:
- If the bridge is the only JWT refresh mechanism, a short-lived token (5 minutes) creates a tight coupling: any bridge interruption longer than 5 minutes renders the viewer unable to fetch tiles or save annotations
- A 30-minute token gives the pathologist a reasonable window to complete the current case examination even if the bridge drops entirely
- The orchestrator refreshes proactively (at ~22.5 minutes for a 30-minute token), so under normal operation the viewer always has a fresh token
- The security trade-off (longer exposure window if a token is compromised) is mitigated by case-scoping: a stolen token only grants access to the specific case, not the entire system

### 3.3 WebSocket Heartbeat Interval: 30 Seconds

**Decision**: The WebSocket connection to the Session Awareness Service uses a 30-second ping/pong heartbeat.

**Rationale**:
- Hospital network infrastructure frequently includes HTTP proxies, load balancers, and firewalls that terminate idle TCP connections after 60-120 seconds
- A 30-second heartbeat ensures the connection stays alive through aggressive infrastructure timeouts
- This is independent of the `postMessage` bridge heartbeat (15 seconds), which serves a different purpose (detecting whether the other browser window is responsive)

### 3.4 Viewer as Standalone-Capable Application

**Decision**: The viewer must be fully functional as a standalone application (launched directly without an orchestrator).

**Rationale**:
- Ensures the viewer is never a "dead" application when the bridge fails
- Supports educational, demo, and testing scenarios where the orchestrator is not present
- Provides a natural degradation path: if the bridge fails, the viewer simply operates as if it were standalone, with the last-known case context
- Two HTML entry points exist: `index.html` (standalone) and `orchestrated.html` (launched by Starling)

## 4. Component Architecture

### 4.1 Orchestrator Side — ViewerBridge

```
┌──────────────────────────────────────────────────────────────────┐
│                     ViewerBridge (Orchestrator)                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │ Window Manager │  │ Message Router │  │ JWT Provider   │    │
│  │                │  │                │  │                │    │
│  │ • open()       │  │ • type dispatch│  │ • issue()      │    │
│  │ • isAlive()    │  │ • origin check │  │ • refresh()    │    │
│  │ • close()      │  │ • seq tracking │  │ • schedule()   │    │
│  │ • reopen()     │  │ • ack timeout  │  │                │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐                         │
│  │ Heartbeat      │  │ State Machine  │                         │
│  │ Controller     │  │                │                         │
│  │                │  │ DISCONNECTED   │                         │
│  │ • send ping    │  │ CONNECTING     │                         │
│  │ • track acks   │  │ CONNECTED      │                         │
│  │ • detect loss  │  │ DEGRADED       │                         │
│  └────────────────┘  │ RECONNECTING   │                         │
│                      └────────────────┘                         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Location**: `Starling/web-client/src/lib/viewer-bridge.ts`

### 4.2 Viewer Side — OrchestratorBridge

```
┌──────────────────────────────────────────────────────────────────┐
│                   OrchestratorBridge (Viewer)                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │ Message Handler│  │ JWT Store      │  │ Case Context   │    │
│  │                │  │                │  │ Store          │    │
│  │ • onmessage    │  │ • current token│  │                │    │
│  │ • origin check │  │ • expiry time  │  │ • caseId       │    │
│  │ • dispatch     │  │ • isExpired()  │  │ • patientId    │    │
│  │ • ack respond  │  │ • onExpiring() │  │ • slideRefs    │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐                         │
│  │ Heartbeat      │  │ Degradation    │                         │
│  │ Responder      │  │ Manager        │                         │
│  │                │  │                │                         │
│  │ • ack pings    │  │ • bridgeState  │                         │
│  │ • detect loss  │  │ • showBanner() │                         │
│  │ • notify UI    │  │ • standalone() │                         │
│  └────────────────┘  └────────────────┘                         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Location**: `large_image/digital-viewer/packages/viewer-core/src/orchestrator-bridge.ts`

## 5. Message Protocol

### 5.1 Message Envelope

All messages conform to a typed envelope:

```typescript
interface BridgeMessage {
  type: BridgeMessageType;
  seq: number;               // Monotonically increasing per sender
  timestamp: number;         // Date.now() at send time
  payload: Record<string, unknown>;
}

type BridgeMessageType =
  | 'VIEWER_OPEN'           // Orchestrator → Viewer: initial case context + JWT
  | 'VIEWER_READY'          // Viewer → Orchestrator: viewer initialized, ready for interaction
  | 'CASE_SWITCH'           // Orchestrator → Viewer: new case context + JWT
  | 'CASE_SWITCH_ACK'       // Viewer → Orchestrator: case switch accepted
  | 'CASE_SWITCH_REJECT'    // Viewer → Orchestrator: user declined case switch
  | 'JWT_REFRESH'           // Orchestrator → Viewer: fresh JWT for current case
  | 'JWT_REFRESH_ACK'       // Viewer → Orchestrator: JWT accepted
  | 'HEARTBEAT'             // Orchestrator → Viewer: liveness check
  | 'HEARTBEAT_ACK'         // Viewer → Orchestrator: liveness response
  | 'BRIDGE_RECONNECT'      // Orchestrator → Viewer: re-establish after disconnection
  | 'BRIDGE_RECONNECT_ACK'  // Viewer → Orchestrator: reconnection accepted
  | 'VIEWER_CLOSE'          // Orchestrator → Viewer: request to close
  | 'AUDIT_EVENT'           // Viewer → Orchestrator: forward audit event for server persistence
  | 'SLIDE_CHANGED'         // Viewer → Orchestrator: user navigated to different slide
  ;
```

### 5.2 Message Payloads

```typescript
// VIEWER_OPEN payload
interface ViewerOpenPayload {
  caseId: string;
  patientIdentifier: string;
  patientDOB?: string;
  jwt: string;
  jwtExpiresAt: number;       // Unix epoch ms
  diagnosticMode: boolean;
  slideId?: string;            // Optional: open specific slide
}

// CASE_SWITCH payload
interface CaseSwitchPayload {
  newCaseId: string;
  newPatientIdentifier: string;
  newPatientDOB?: string;
  jwt: string;
  jwtExpiresAt: number;
  diagnosticMode: boolean;
}

// BRIDGE_RECONNECT payload
interface BridgeReconnectPayload {
  caseId: string;
  patientIdentifier: string;
  jwt: string;
  jwtExpiresAt: number;
  orchestratorSeq: number;    // Last known viewer seq, for gap detection
}

// AUDIT_EVENT payload
interface AuditEventPayload {
  eventType: string;
  caseId: string;
  slideId?: string;
  metadata: Record<string, unknown>;
}
```

### 5.3 Security Constraints

- **Origin validation**: Both sides validate `event.origin` against the expected origin (derived from nginx proxy configuration). Messages from unexpected origins are silently discarded.
- **No secrets in messages**: The JWT is the only sensitive payload. It is held in memory on both sides and never persisted to `localStorage`, `sessionStorage`, or cookies.
- **No cross-origin**: Both the orchestrator and viewer are served behind the same nginx reverse proxy on `:8443`, so `postMessage` operates within a single effective origin. This eliminates CORS complications.

## 6. Behavioral Specifications

### 6.1 Viewer Launch Sequence

```
Orchestrator                                     Viewer
    │                                               │
    │  window.open(viewerUrl)                       │
    │ ─────────────────────────────────────────────▶│
    │                                               │
    │  [3s timeout: detect popup blocker]           │
    │                                               │
    │              ◀── VIEWER_READY (windowId) ─────│
    │                                               │
    │  VIEWER_OPEN (caseId, jwt, dxMode) ──────────▶│
    │                                               │
    │  Start heartbeat timer (15s interval)         │
    │                                               │
    │              ◀── SLIDE_CHANGED (slideId) ─────│
    │                                               │
    ▼                                               ▼
```

**Popup blocker detection**: After calling `window.open()`, the orchestrator checks the returned `WindowProxy`. If it is `null`, or if the window's `closed` property is `true` within 3 seconds, the orchestrator concludes that a popup blocker intervened and displays a user-facing message with instructions.

### 6.2 Heartbeat and Degradation

```
                 CONNECTED
                    │
                    │ 3 consecutive missed HEARTBEAT_ACK
                    ▼
                 DEGRADED
                    │
             ┌──────┴──────┐
             │              │
             ▼              ▼
    HEARTBEAT_ACK      window.closed
    received           detected
             │              │
             ▼              ▼
         RECONNECTING   DISCONNECTED
             │
             │ BRIDGE_RECONNECT + ACK
             ▼
         CONNECTED
```

**Heartbeat parameters**:
- Interval: 15 seconds (configurable via `VITE_BRIDGE_HEARTBEAT_INTERVAL`)
- Timeout: 3 consecutive missed ACKs = 45 seconds to degraded state
- The viewer mirrors this: if it receives no `HEARTBEAT` for 45 seconds, it enters its own degraded state

**Degradation behavior**:
- Both windows display an amber connection-status indicator
- The viewer continues all local functions (tile rendering, annotations, measurements)
- The orchestrator disables case-switch-to-viewer functionality until the bridge recovers
- JWT refresh is suspended (the viewer operates on its current token)

### 6.3 Case Switching

```
Orchestrator                                     Viewer
    │                                               │
    │  User clicks different case in worklist       │
    │                                               │
    │  [Confirmation dialog: "Switch to            │
    │   new slides? Current case: X"]              │
    │                                               │
    │  User confirms                                │
    │                                               │
    │  CASE_SWITCH (newCaseId, jwt) ───────────────▶│
    │                                               │
    │  [5s timeout]                                 │
    │                                               │
    │              ◀── CASE_SWITCH_ACK ─────────────│
    │                                               │
    │  Update worklist state                        │
    │                                               │
    ▼                                               ▼
```

If the viewer does not ACK within 5 seconds, the orchestrator retries once. On second failure, the orchestrator displays a warning: "Viewer may be out of sync. Consider reopening."

### 6.4 Reconnection After Orchestrator Reload

When the orchestrator page reloads (user refresh, SvelteKit hot-module reload, navigation), the `WindowProxy` reference is lost. Reconnection proceeds as follows:

```
Orchestrator (reloaded)                          Viewer (still open)
    │                                               │
    │  On mount: check sessionStorage for           │
    │  viewer window name                           │
    │                                               │
    │  window.open('', viewerWindowName)            │
    │  (re-acquire reference to named window)       │
    │ ─────────────────────────────────────────────▶│
    │                                               │
    │  BRIDGE_RECONNECT (caseId, jwt, lastSeq) ────▶│
    │                                               │
    │              ◀── BRIDGE_RECONNECT_ACK ────────│
    │                  (viewerCaseId, viewerSeq)    │
    │                                               │
    │  [Compare caseId: match? → CONNECTED          │
    │   mismatch? → prompt user]                    │
    │                                               │
    ▼                                               ▼
```

The viewer window is opened with a deterministic `windowName` (e.g., `starling-viewer-{sessionId}`). On orchestrator reload, `window.open('', windowName)` returns the existing window reference without navigating it.

### 6.5 JWT Refresh Lifecycle

```
Time ──────────────────────────────────────────────────────▶

Token issued (T=0)                  Refresh (T=22.5m)    Expiry (T=30m)
     │                                    │                   │
     │  Normal operation                  │                   │
     │  Viewer uses current JWT           │                   │
     │                                    │                   │
     │                          JWT_REFRESH ──▶               │
     │                          ◀── JWT_REFRESH_ACK           │
     │                                    │                   │
     │                          Viewer swaps to new token     │
     │                                    │                   │
     │                                    │    [If bridge     │
     │                                    │     was down]     │
     │                                    │         │         │
     │                                    │    Token expires  │
     │                                    │    Viewer shows   │
     │                                    │    expiry banner  │
```

If the bridge is down at refresh time, the orchestrator queues the refresh and sends it immediately upon reconnection. If the token expires before reconnection, the viewer displays a non-blocking banner but does not close — tiles already cached in the OpenSeadragon tile cache remain viewable.

## 7. Session Awareness Service Integration

### 7.1 Role in the Architecture

The Session Awareness Service (`:8765`) is a Node.js WebSocket hub that provides cross-browser, cross-device awareness of which cases a user has open. It is entirely optional (Layer 2 of the FDP specification).

```
                   Orchestrator A              Orchestrator B
                   (Browser 1)                 (Browser 2)
                        │                           │
                        │ WSS                       │ WSS
                        ▼                           ▼
                   ┌─────────────────────────────────────┐
                   │      Session Awareness Service       │
                   │                                     │
                   │  Session Registry (in-memory/Redis) │
                   │  Multi-Case Detector                │
                   │  Warning Broadcaster                │
                   └─────────────────────────────────────┘
```

### 7.2 WebSocket Resilience

| Concern | Design |
|:--------|:-------|
| Connection drop | Automatic reconnect with exponential backoff: 1s → 2s → 4s → ... → 30s max, with ±20% jitter |
| Hospital proxy timeout | 30s ping/pong heartbeat keeps connection alive through 60-120s proxy timeouts |
| Service restart | Client re-registers current session state on reconnect |
| Service unavailable at startup | Client starts without session awareness; retries connection in background |
| Message ordering | Each message carries a server-assigned sequence; client detects gaps |

### 7.3 Graceful Degradation

When the Session Awareness Service is unavailable:

| Feature | Behavior |
|:--------|:---------|
| Multi-case warning | Disabled — no cross-browser detection possible |
| "Dr. X is also viewing" indicator | Disabled |
| Focus Declaration Protocol (Layer 1) | Unaffected — runs entirely in the browser |
| Viewer launch and case switching | Unaffected — uses postMessage bridge (Channel 1) |
| JWT provisioning and refresh | Unaffected |
| Annotations, measurements, review state | Unaffected |
| Tile rendering | Unaffected |

## 8. Error Scenarios and Recovery

| Scenario | Detection | Orchestrator Response | Viewer Response |
|:---------|:----------|:---------------------|:----------------|
| Popup blocker prevents viewer | `window.open()` returns null | Display instruction banner; retry button | N/A (not launched) |
| Viewer window closed by user | `windowRef.closed === true` | "Viewer closed" indicator + reopen button | N/A (closed) |
| Viewer window navigated away | Heartbeat timeout (no ACK) | Amber indicator; disable case switch to viewer | N/A (different page) |
| Orchestrator tab closed | Heartbeat timeout (no HEARTBEAT received) | N/A (closed) | Amber indicator; continue with current JWT |
| Orchestrator page reload | Heartbeat gap → reconnect | Re-acquire window ref; send `BRIDGE_RECONNECT` | ACK reconnect; resync case context |
| JWT expires during bridge outage | Token expiry check (local) | Queue refresh for reconnection | Expiry banner; cached tiles still viewable |
| Network partition (both windows alive) | Heartbeat timeout | Amber indicator | Amber indicator; standalone mode |
| Session Awareness Service down | WebSocket error/close | Disable multi-case warning | FDP Layer 1 continues; Layer 2 disabled |
| OS moves viewer to different virtual desktop | No detection (window still alive) | No effect (heartbeat continues) | FDP focus announcement on window.focus |

## 9. Security Controls

| Control | Implementation | Requirement |
|:--------|:---------------|:------------|
| Origin validation | Both sides check `event.origin` against configured origin | SYS-OVI-006 |
| JWT in memory only | No `localStorage`, no cookies, no `sessionStorage` | SYS-OVI-014 |
| Case-scoped JWT | Token grants access to specific case only | SYS-OVI-012 |
| Typed messages | Discriminated union prevents malformed message processing | SYS-OVI-005 |
| Sequence numbers | Detect replays, gaps, and out-of-order delivery | SYS-OVI-005 |
| HTTPS/WSS only | All communication over TLS via nginx proxy | SYS-OVI-006 |

## 10. Traceability

| Design Element | System Requirement | Risk Control |
|:---------------|:-------------------|:-------------|
| ViewerBridge.open() + popup detection | SYS-OVI-001, SYS-OVI-002 | — |
| Window liveness polling | SYS-OVI-003, SYS-OVI-004 | — |
| Typed message protocol + seq numbers | SYS-OVI-005, SYS-OVI-008 | — |
| Origin validation | SYS-OVI-006 | RC-OVI-001 |
| Heartbeat protocol | SYS-OVI-007 | RC-OVI-002 |
| Case switch confirmation | SYS-OVI-009, SYS-OVI-010, SYS-OVI-011 | RC-001-C (Viewer FMEA) |
| JWT 30-min lifetime | SYS-OVI-012 | — |
| Proactive JWT refresh | SYS-OVI-013 | — |
| JWT in memory only | SYS-OVI-014 | SC-001 (Viewer Cybersecurity) |
| Expiry banner (no abrupt close) | SYS-OVI-015 | — |
| Degradation indicators | SYS-OVI-016 | RC-OVI-002 |
| Standalone viewer capability | SYS-OVI-017, SYS-OVI-020 | RC-OVI-003 |
| Reconnect handshake | SYS-OVI-018, SYS-OVI-019 | RC-OVI-002 |
| Session Service optional | SYS-OVI-021, SYS-OVI-024 | — |
| WebSocket reconnect with backoff | SYS-OVI-022 | — |
| WebSocket heartbeat (30s) | SYS-OVI-023 | — |

## 11. Revision History

| Version | Date | Author | Description |
|:--------|:-----|:-------|:------------|
| 1.0 | 2026-02-28 | Engineering | Initial OVI architecture |

---

**Document Control**: This is a controlled document. Changes require review and approval per SOP-DHF-Management.
