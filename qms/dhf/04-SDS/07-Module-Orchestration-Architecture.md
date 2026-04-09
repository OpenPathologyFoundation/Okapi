---
title: Module Orchestration Architecture
document_id: DHF-04-07
version: 1.0
status: DRAFT
owner: Lead Architect
created_date: 2026-04-04
trace_source: DHF-04, STARLING-MIS-001
---

> **Project rename notice (2026-04-08, v2):** This project was renamed from **Okapi** to **Starling**. An initial cosmetic rename retained structural identifiers; the full rename was completed on this date across Java packages (`com.starling.auth.*`), Spring configuration, database (`starling_auth`), Keycloak realm (`starling`), JWT issuer, protocol field names, seed group names (`Starling_*`), and documentation. Historical traceability of the Okapi name is preserved via git history and `qms/dhf/00-Index.md` revision history; no legacy Okapi identifiers remain.

# Software Design Specification — Module Orchestration Architecture

| Field | Value |
|---|---|
| **Document ID** | DHF-04-07 |
| **Version** | 1.0 DRAFT |
| **Date** | April 4, 2026 |
| **IEC 62304 Reference** | §5.3 — Software Architectural Design |
| **Status** | DRAFT |
| **Parent** | STARLING-MIS-001 (Module Integration Specification) |

---

## 1. Purpose

This document describes the implementation of the module orchestration layer within the Starling web-client. It covers:

- The `ModuleBridge` base class and how module-specific bridges extend it
- The `activityStore` that tracks all open activities across the platform
- The Activity Registry and how modules are discovered and launched
- Cross-module message forwarding
- Migration of the existing `ViewerBridge` to the new architecture

This document implements the orchestrator-side responsibilities defined in STARLING-MIS-001 §4.

---

## 2. Scope

**In scope:** Orchestrator web-client code only — the bridge classes, stores, registry, and cross-module routing that live in `Starling/web-client/src/lib/`.

**Out of scope:** Module-internal architecture (see each module's own SDS), session service protocol (see FDP documentation), auth-system API design (see DHF-04-01/02), nginx configuration (see STARLING-MIS-001 §6.1).

---

## 3. Component Architecture

### 3.1 File Organization

```
web-client/src/lib/
├── bridges/
│   ├── module-bridge.ts              # Base bridge class (extracted from viewer-bridge.ts)
│   ├── viewer-bridge.ts              # Pelican-specific bridge (extends ModuleBridge)
│   ├── willet-bridge.ts              # WILLET-specific bridge (extends ModuleBridge)
│   └── index.ts                      # Re-exports
├── stores/
│   ├── activity.svelte.ts            # ActivityStore — tracks all open modules
│   ├── viewer.svelte.ts              # ViewerStore (updated to delegate to ActivityStore)
│   ├── willet.svelte.ts              # WilletStore (new)
│   ├── worklist.svelte.ts            # (unchanged)
│   ├── auth.svelte.ts                # (unchanged)
│   └── ...
├── types/
│   ├── bridge-protocol.ts            # Base protocol types (STARLING-MIS-001 §3.4.2)
│   ├── viewer-bridge.ts              # Viewer-specific message types (existing, extended)
│   ├── willet-bridge.ts              # WILLET-specific message types (new)
│   ├── activity.ts                   # Activity registry and state types
│   └── ...
└── registry/
    └── activities.ts                 # Static Activity Registry definitions
```

### 3.2 Dependency Graph

```
┌────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Routes / Components)                  │
│                                                                        │
│   /app/case/[accession]     /app/worklist     Sidebar    Case Header   │
│          │                       │               │            │        │
│          ▼                       ▼               ▼            ▼        │
│   ┌──────────────────────────────────────────────────────────────┐     │
│   │                      activityStore                           │     │
│   │  (reactive — all components derive state from here)          │     │
│   └───────┬──────────────────────┬───────────────────────────────┘     │
│           │                      │                                     │
│           ▼                      ▼                                     │
│   ┌──────────────┐       ┌──────────────┐                              │
│   │ ViewerBridge │       │ WilletBridge │                              │
│   │ (extends     │       │ (extends     │                              │
│   │  ModuleBridge│       │  ModuleBridge│                              │
│   └──────┬───────┘       └──────┬───────┘                              │
│          │                      │                                      │
│          ▼                      ▼                                      │
│   ┌─────────────────────────────────────┐                              │
│   │          ModuleBridge (base)        │                              │
│   │  • window.open / postMessage        │                              │
│   │  • heartbeat                        │                              │
│   │  • token refresh                    │                              │
│   │  • audit event batching             │                              │
│   │  • origin + source validation       │                              │
│   └─────────────────────────────────────┘                              │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. ModuleBridge Base Class

### 4.1 Extraction from ViewerBridge

The existing `ViewerBridge` (375 lines) contains generic orchestration logic mixed with viewer-specific handling. The refactoring extracts the generic logic into `ModuleBridge`:

**Moves to `ModuleBridge` (generic):**
- `launch()` — opens window, sets up message listener, starts window-closed check
- `sendToModule()` — postMessage with origin validation (renamed from `sendToViewer`)
- `handleMessage()` — origin/source validation, type-guard dispatch
- `startHeartbeat()` / `stopHeartbeat()` — 5-second heartbeat loop
- `scheduleTokenRefresh()` / `refreshToken()` — JWT pre-expiry refresh
- `handleAuditEvent()` / `flushAuditEvents()` — audit event batching and POST
- `cleanup()` — timer teardown, listener removal
- `on()` — typed event subscription
- State management: `setState()`, `getState()`, `isAlive()`

**Stays in `ViewerBridge` (viewer-specific):**
- Handler for `viewer:case-loaded` → emit `caseLoaded`
- Handler for `viewer:error` → emit `viewerError`
- Handler for `viewer:audit-event` → delegate to base `handleAuditEvent`
- `sendFocusState()` / `sendCaseChange()` — convenience methods

### 4.2 ModuleBridge API

```typescript
// src/lib/bridges/module-bridge.ts

export type ModuleBridgeState = 'idle' | 'launching' | 'connected' | 'error' | 'closed';

export interface ModuleBridgeConfig {
  /** Module identifier (matches Activity Registry) */
  moduleId: string;

  /** URL to open */
  url: string;

  /** Expected origin for postMessage validation */
  origin: string;

  /** Initialization payload to send after module:ready */
  initPayload: ModuleInitPayload;

  /** Mount strategy */
  strategy: 'window' | 'iframe';

  /** Window features (for strategy: 'window') */
  windowFeatures?: string;

  /** Callback to mint a fresh JWT */
  mintToken: () => Promise<string | null>;

  /** Callback when a module-specific message arrives */
  onModuleMessage?: (message: unknown) => void;
}

export interface ModuleBridgeEvents {
  stateChange: (state: ModuleBridgeState) => void;
  initialized: (data: { moduleId: string; version: string; capabilities: string[] }) => void;
  error: (data: { code: string; message: string; recoverable: boolean }) => void;
  stateUpdate: (data: { key: string; value: unknown }) => void;
  auditEvent: (event: AuditEvent) => void;
}

export class ModuleBridge {
  protected moduleWindow: Window | null = null;
  protected moduleOrigin: string = '';
  protected config: ModuleBridgeConfig | null = null;
  protected state: ModuleBridgeState = 'idle';

  // ... timers, listeners, audit batch (same as current ViewerBridge)

  /** Launch the module */
  launch(config: ModuleBridgeConfig): void;

  /** Send the base focus state message */
  sendFocusState(state: 'active' | 'blurred'): void;

  /** Send a context update to the module */
  sendContextUpdate(key: string, value: unknown): void;

  /** Send logout signal and close */
  close(): void;

  /** Check if the module window is alive */
  isAlive(): boolean;

  /** Subscribe to events */
  on<K extends keyof ModuleBridgeEvents>(event: K, handler: ModuleBridgeEvents[K]): () => void;

  /** Destroy the bridge and all resources */
  destroy(): void;

  // ---- Protected methods for subclass override ----

  /** Process a message after base validation. Override to handle module-specific types. */
  protected handleModuleMessage(message: { type: string; payload: unknown }): void;

  /** Build the init payload. Override to add module-specific fields. */
  protected buildInitPayload(): ModuleInitPayload;
}
```

### 4.3 ViewerBridge (Updated)

```typescript
// src/lib/bridges/viewer-bridge.ts

import { ModuleBridge } from './module-bridge';
import type { ViewerMessage } from '../types/viewer-bridge';

export class ViewerBridge extends ModuleBridge {
  // Viewer-specific events (extend base events)
  private viewerListeners = {
    caseLoaded: new Set<(data: { caseId: string; slideCount: number }) => void>(),
    viewerError: new Set<(data: { code: string; message: string }) => void>(),
  };

  /** Send a case change to the already-open viewer */
  sendCaseChange(caseId: string, accession: string): void {
    if (this.state !== 'connected') return;
    this.sendToModule({
      type: 'orchestrator:case-change',
      payload: { caseId, accession },
    });
  }

  protected handleModuleMessage(message: ViewerMessage): void {
    switch (message.type) {
      case 'viewer:case-loaded':
        this.viewerListeners.caseLoaded.forEach(fn => fn(message.payload));
        break;
      case 'viewer:error':
        this.viewerListeners.viewerError.forEach(fn => fn(message.payload));
        break;
      case 'viewer:audit-event':
        // Delegate to base class audit handling
        this.handleAuditEvent(message.payload);
        break;
    }
  }
}
```

### 4.4 WilletBridge

```typescript
// src/lib/bridges/willet-bridge.ts

import { ModuleBridge } from './module-bridge';
import type { WilletMessage } from '../types/willet-bridge';

export interface WilletBridgeEvents {
  reportFinalized: (data: { caseId: string; accession: string; reportId: string }) => void;
  lockAcquired: (data: { caseId: string; lockId: string }) => void;
  lockReleased: (data: { caseId: string }) => void;
  navigateToSlide: (data: { caseId: string; partLabel: string; slideId?: string }) => void;
  draftStatus: (data: { caseId: string; isDirty: boolean; clauseCount: number }) => void;
}

export class WilletBridge extends ModuleBridge {
  private willetListeners: Map<string, Set<Function>> = new Map();

  protected handleModuleMessage(message: WilletMessage): void {
    switch (message.type) {
      case 'willet:report-finalized':
        this.emit('reportFinalized', message.payload);
        break;
      case 'willet:lock-acquired':
        this.emit('lockAcquired', message.payload);
        break;
      case 'willet:lock-released':
        this.emit('lockReleased', message.payload);
        break;
      case 'willet:navigate-to-slide':
        this.emit('navigateToSlide', message.payload);
        break;
      case 'willet:draft-status':
        this.emit('draftStatus', message.payload);
        break;
    }
  }
}
```

---

## 5. Activity Store

### 5.1 Design

The `ActivityStore` is a Svelte 5 reactive class that tracks all open activities. It replaces the pattern where `viewerStore` was the only tracked external module.

```typescript
// src/lib/stores/activity.svelte.ts

import type { ActivityDefinition, ActivityState } from '$lib/types/activity';
import { ACTIVITY_REGISTRY } from '$lib/registry/activities';

class ActivityStore {
  /** All currently open activities (keyed by activityId) */
  activities: Map<string, ActivityState> = $state(new Map());

  /** The currently focused activity (receives 'active' focus state) */
  focusedActivityId: string | null = $state(null);

  // ---- Derived state ----

  /** All open external modules */
  openModules = $derived.by(() => {
    const result: ActivityState[] = [];
    for (const activity of this.activities.values()) {
      if (activity.status === 'connected' || activity.status === 'launching') {
        result.push(activity);
      }
    }
    return result;
  });

  /** Whether any module has unsaved changes for the given case */
  hasDirtyModules = (caseId: string) => $derived.by(() => {
    for (const activity of this.activities.values()) {
      if (activity.caseId === caseId && activity.moduleState?.isDirty) {
        return true;
      }
    }
    return false;
  });

  /** Get the activity state for a specific module */
  getActivity(activityId: string): ActivityState | undefined {
    return this.activities.get(activityId);
  }

  // ---- Lifecycle methods ----

  /** Register a new activity (called when launching a module) */
  register(activityId: string, state: Partial<ActivityState>): void {
    this.activities.set(activityId, {
      activityId,
      status: 'launching',
      caseId: null,
      accession: null,
      moduleState: {},
      bridge: null,
      lastHeartbeat: Date.now(),
      ...state,
    });
  }

  /** Update an activity's state (called by bridge event handlers) */
  update(activityId: string, patch: Partial<ActivityState>): void {
    const current = this.activities.get(activityId);
    if (current) {
      this.activities.set(activityId, { ...current, ...patch });
    }
  }

  /** Remove an activity (called when a module closes) */
  unregister(activityId: string): void {
    const activity = this.activities.get(activityId);
    if (activity?.bridge) {
      activity.bridge.destroy();
    }
    this.activities.delete(activityId);
  }

  /** Update focus — sends 'active' to focused module, 'blurred' to all others */
  setFocus(activityId: string | null): void {
    this.focusedActivityId = activityId;
    for (const [id, activity] of this.activities) {
      if (activity.bridge && activity.status === 'connected') {
        activity.bridge.sendFocusState(id === activityId ? 'active' : 'blurred');
      }
    }
  }

  // ---- Cross-module communication ----

  /**
   * Forward a message from one module to another.
   * The orchestrator translates/enriches the message before forwarding.
   */
  forwardToModule(targetActivityId: string, message: { type: string; payload: unknown }): void {
    const target = this.activities.get(targetActivityId);
    if (target?.bridge && target.status === 'connected') {
      target.bridge.sendToModule(message);
    }
  }

  // ---- Transition guards ----

  /**
   * Check if it's safe to switch away from a case.
   * Returns a list of modules with unsaved changes.
   */
  getDirtyModulesForCase(caseId: string): ActivityState[] {
    const dirty: ActivityState[] = [];
    for (const activity of this.activities.values()) {
      if (activity.caseId === caseId && activity.moduleState?.isDirty) {
        dirty.push(activity);
      }
    }
    return dirty;
  }

  /** Shut down all modules (called on logout) */
  destroyAll(): void {
    for (const activity of this.activities.values()) {
      if (activity.bridge) {
        activity.bridge.close();
        activity.bridge.destroy();
      }
    }
    this.activities.clear();
    this.focusedActivityId = null;
  }
}

export const activityStore = new ActivityStore();
```

### 5.2 Integration with Existing Stores

The `viewerStore` and `willetStore` become thin facades over the `activityStore`:

```typescript
// src/lib/stores/viewer.svelte.ts (updated)

class ViewerStore {
  // Derived from activityStore
  get state(): ViewerBridgeState {
    return (activityStore.getActivity('pelican-viewer')?.status ?? 'idle') as ViewerBridgeState;
  }

  get isOpen(): boolean {
    const activity = activityStore.getActivity('pelican-viewer');
    return activity?.status === 'connected' || activity?.status === 'launching';
  }

  get currentCase(): string | null {
    return activityStore.getActivity('pelican-viewer')?.accession ?? null;
  }

  // ... launchViewer(), closeViewer() delegate to activityStore
}
```

This preserves the existing API that the case page, header, and worklist already use, while centralizing the tracking in `activityStore`.

---

## 6. Activity Registry

### 6.1 Static Definitions

```typescript
// src/lib/registry/activities.ts

import type { ActivityDefinition } from '$lib/types/activity';

export const ACTIVITY_REGISTRY: Record<string, ActivityDefinition> = {
  'worklist': {
    id: 'worklist',
    label: 'Worklist',
    type: 'internal',
    path: '/app/worklist',
    caseScoped: false,
  },

  'case-detail': {
    id: 'case-detail',
    label: 'Case',
    type: 'internal',
    path: '/app/case',
    caseScoped: true,
  },

  'pelican-viewer': {
    id: 'pelican-viewer',
    label: 'Digital Viewer',
    type: 'external',
    path: '/viewer/orchestrated.html',
    caseScoped: true,
    mount: {
      strategy: 'window',
      windowFeatures: 'width=1400,height=900,menubar=no,toolbar=no,location=no,status=no',
      protocolVersion: '1.0',
    },
    initPayloadExtensions: ['caseId', 'accession', 'mode'],
  },

  'willet-report': {
    id: 'willet-report',
    label: 'Report Authoring',
    type: 'external',
    path: '/report/orchestrated.html',
    caseScoped: true,
    mount: {
      strategy: 'window',
      windowFeatures: 'width=1200,height=900,menubar=no,toolbar=no,location=no,status=no',
      protocolVersion: '1.0',
    },
    requiredPermissions: ['REPORT_AUTHOR'],
    initPayloadExtensions: ['caseId', 'accession', 'role'],
  },

  'activity-portal': {
    id: 'activity-portal',
    label: 'My Activity',
    type: 'internal',
    path: '/app/portal',
    caseScoped: false,
  },
};
```

### 6.2 Launch Flow

When the user triggers a module launch (e.g., clicks "Write Report" on the case page):

```typescript
async function launchActivity(activityId: string, context: Record<string, unknown>): Promise<void> {
  const definition = ACTIVITY_REGISTRY[activityId];
  if (!definition) throw new Error(`Unknown activity: ${activityId}`);

  // 1. Check permissions
  if (definition.requiredPermissions) {
    for (const perm of definition.requiredPermissions) {
      if (!authStore.hasPermission(perm)) {
        throw new Error(`Missing permission: ${perm}`);
      }
    }
  }

  // 2. For internal activities, navigate via SvelteKit router
  if (definition.type === 'internal') {
    goto(definition.caseScoped ? `${definition.path}/${context.accession}` : definition.path);
    return;
  }

  // 3. For external modules, check if already open
  const existing = activityStore.getActivity(activityId);
  if (existing?.status === 'connected' && existing.caseId === context.caseId) {
    // Already open for this case — just focus it
    activityStore.setFocus(activityId);
    return;
  }
  if (existing?.status === 'connected' && existing.caseId !== context.caseId) {
    // Open for a different case — send case-change
    existing.bridge?.sendContextUpdate('case', context);
    activityStore.update(activityId, { caseId: context.caseId as string });
    return;
  }

  // 4. Mint token and build init payload
  const token = await mintToken();
  if (!token) return;

  const initPayload = buildInitPayload(definition, token, context);

  // 5. Create the module-specific bridge
  const bridge = createBridgeForActivity(activityId, initPayload);

  // 6. Register in activity store
  activityStore.register(activityId, {
    caseId: context.caseId as string,
    accession: context.accession as string,
    bridge,
  });

  // 7. Wire up bridge events → activity store updates
  bridge.on('stateChange', (state) => {
    activityStore.update(activityId, { status: state });
  });
  bridge.on('stateUpdate', (data) => {
    const current = activityStore.getActivity(activityId);
    if (current) {
      activityStore.update(activityId, {
        moduleState: { ...current.moduleState, [data.key]: data.value },
      });
    }
  });

  // 8. Launch
  bridge.launch({
    moduleId: activityId,
    url: `${window.location.origin}${definition.path}`,
    origin: window.location.origin,
    initPayload,
    strategy: definition.mount!.strategy,
    windowFeatures: definition.mount!.windowFeatures,
    mintToken: () => mintToken(),
  });
}
```

---

## 7. Cross-Module Message Routing

### 7.1 Routing Table

The orchestrator maintains a message routing table for cross-module forwarding:

| Source Message | Source Module | Action | Target Module | Target Message |
|---|---|---|---|---|
| `willet:navigate-to-slide` | WILLET | Translate: look up slideId from partLabel via case data | Pelican Digital Viewer | `orchestrator:navigate-slide` |
| `willet:report-finalized` | WILLET | Update worklist item status | (Internal) | `worklistStore.updateCaseStatus()` |
| `willet:report-finalized` | WILLET | Emit audit event | (Audit pipeline) | `POST /api/audit/events` |
| `viewer:annotation-created` | Pelican Digital Viewer | Notify WILLET of new annotation | WILLET | `orchestrator:context-update` |

### 7.2 Implementation

```typescript
// In the orchestrator's message handling setup for WilletBridge:

willetBridge.on('navigateToSlide', async (data) => {
  // Translate part label → slide ID using case data
  const slideId = await resolveSlideForPart(data.caseId, data.partLabel);
  if (!slideId) return;

  // Forward to the viewer
  activityStore.forwardToModule('pelican-viewer', {
    type: 'orchestrator:navigate-slide',
    payload: { slideId, caseId: data.caseId },
  });
});

willetBridge.on('reportFinalized', async (data) => {
  // Update worklist
  await worklistStore.updateCaseStatus(data.accession, 'SIGNED_OUT');

  // Close WILLET window (optional — could also keep it open for corrections)
  // activityStore.unregister('willet-report');
});
```

---

## 8. App Shell Updates

### 8.1 Focus Management (Updated)

The app layout (`/app/+layout.svelte`) currently manages focus for the viewer only. It needs to generalize to all external modules:

```svelte
<!-- /app/+layout.svelte (updated focus management) -->
<script>
  import { page } from '$app/stores';
  import { activityStore } from '$lib/stores/activity.svelte';

  // Determine which activity should be "focused" based on current route
  $effect(() => {
    const path = $page.url.pathname;

    if (path.startsWith('/app/case/')) {
      // On a case page — the case-scoped modules for this case are "active"
      const accession = path.split('/')[3];
      for (const [id, activity] of activityStore.activities) {
        if (activity.accession === accession) {
          activityStore.setFocus(id);
        }
      }
    } else {
      // Not on a case page — blur all external modules
      activityStore.setFocus(null);
    }
  });
</script>
```

### 8.2 Transition Guards

Before navigating away from a case, check for dirty modules:

```svelte
<script>
  import { beforeNavigate } from '$app/navigation';
  import { activityStore } from '$lib/stores/activity.svelte';

  beforeNavigate(({ cancel, to }) => {
    const currentAccession = $page.params.accession;
    if (!currentAccession) return;

    const dirty = activityStore.getDirtyModulesForCase(currentAccession);
    if (dirty.length > 0) {
      // Show confirmation dialog
      const confirmed = confirm(
        `You have unsaved changes in ${dirty.map(d => d.activityId).join(', ')}. Continue?`
      );
      if (!confirmed) cancel();
    }
  });
</script>
```

### 8.3 Logout Coordination

On logout, all modules must be notified and closed:

```typescript
async function handleLogout(): Promise<void> {
  // 1. Send logout to all connected modules
  activityStore.destroyAll();

  // 2. Call the auth-system logout endpoint
  await fetch('/logout', { method: 'POST', credentials: 'include', headers: csrfHeaders() });

  // 3. Navigate to logged-out page
  goto('/logged-out');
}
```

---

## 9. Case Page Updates

The case page (`/app/case/[accession]/+page.svelte`) gains additional launch buttons:

```svelte
<!-- Existing: Launch Viewer -->
<button onclick={() => launchActivity('pelican-viewer', { caseId, accession, mode: 'clinical' })}>
  Launch Viewer
</button>

<!-- New: Write Report -->
{#if authStore.hasPermission('REPORT_AUTHOR')}
  <button onclick={() => launchActivity('willet-report', { caseId, accession, role: authStore.reportRole })}>
    Write Report
  </button>
{/if}

<!-- Status indicators from activityStore -->
{#if activityStore.getActivity('pelican-viewer')?.status === 'connected'}
  <span class="badge">Viewer Open — {activityStore.getActivity('pelican-viewer')?.moduleState?.slideCount} slides</span>
{/if}

{#if activityStore.getActivity('willet-report')?.status === 'connected'}
  <span class="badge" class:dirty={activityStore.getActivity('willet-report')?.moduleState?.isDirty}>
    Report {activityStore.getActivity('willet-report')?.moduleState?.isDirty ? '(unsaved)' : ''}
  </span>
{/if}
```

---

## 10. Testing Strategy

### 10.1 Unit Tests

| Component | Test Strategy |
|---|---|
| `ModuleBridge` | Mock `window.open`, `postMessage`, `MessageEvent`; verify handshake, heartbeat, token refresh, audit batching |
| `ActivityStore` | Verify registration, unregistration, focus management, dirty-module detection, destroyAll |
| `Activity Registry` | Verify all entries are valid, permissions are defined, paths are consistent |
| `ViewerBridge` (updated) | Verify backward compatibility — existing viewer tests pass unchanged |
| `WilletBridge` | Verify WILLET-specific message handling, event emission |

### 10.2 Integration Tests (Playwright)

| Scenario | Steps |
|---|---|
| Full handshake | Launch orchestrator → open viewer → verify init → heartbeat → token refresh → close |
| Case switch | Open viewer for case A → navigate to case B → verify case-change message |
| Cross-module forward | Open viewer + WILLET → WILLET emits navigate-to-slide → verify viewer receives |
| Dirty guard | Open WILLET → make changes → navigate away → verify confirmation dialog |
| Logout coordination | Open viewer + WILLET → logout → verify both windows close |
| Mode switch | Open viewer in clinical → open atlas case (educational) → verify viewer closes and reopens |

---

## 11. Migration Plan

### Phase 1: Extract ModuleBridge (Non-Breaking)

1. Create `src/lib/bridges/module-bridge.ts` by extracting generic logic from `viewer-bridge.ts`
2. Update `viewer-bridge.ts` to extend `ModuleBridge`
3. **Run all existing viewer tests** — they must pass without modification
4. Create `src/lib/stores/activity.svelte.ts` wrapping the existing `viewerStore` state
5. Update app layout to use `activityStore.setFocus()` instead of `viewerStore.setViewerFocus()`

**Exit criteria:** All existing functionality works identically. No user-visible changes.

### Phase 2: WILLET Integration

1. Create `willet-bridge.ts` extending `ModuleBridge`
2. Create `willet.svelte.ts` store
3. Add `willet-report` to Activity Registry
4. Add `/report/` location block to nginx
5. Add "Write Report" button to case page
6. Wire up cross-module forwarding (navigate-to-slide)
7. Wire up report-finalized → worklist status update

**Exit criteria:** WILLET launches from case page, communicates via bridge, report finalization updates worklist.

### Phase 3: Activity Portal

1. Create `/app/portal/+page.svelte` route
2. Implement audit event timeline (read from `GET /api/audit/events?userId=...&limit=50`)
3. Add portal link to sidebar
4. Show recent cases, recent actions, file downloads

**Exit criteria:** Portal shows unified activity history from all modules.

---

## 12. Traceability

| Section | Traces To |
|---|---|
| §3 Component Architecture | STARLING-MIS-001 §4 (Orchestrator Responsibilities) |
| §4 ModuleBridge | STARLING-MIS-001 §3.4 (Message Protocol), existing `viewer-bridge.ts` |
| §5 ActivityStore | STARLING-MIS-001 §4.2 (Activity State Store) |
| §6 Activity Registry | STARLING-MIS-001 §4.1 (Activity Registry) |
| §7 Cross-Module Routing | STARLING-MIS-001 §4.3 (Cross-Module Communication) |
| §8 App Shell Updates | Starling web-client `routes/app/+layout.svelte`, DHF-04-SDS/05-Worklist-Architecture |
| §9 Case Page Updates | Starling web-client `routes/app/case/[accession]/+page.svelte` |
| §11 Migration Plan | STARLING-MIS-001 §9 (Migration Path) |

---

## 13. Revision History

| Version | Date | Changes |
|---|---|---|
| 1.0 | 2026-04-04 | Initial specification: ModuleBridge extraction, ActivityStore, Activity Registry, cross-module routing, app shell updates, migration plan. |
