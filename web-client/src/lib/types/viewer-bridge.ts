/**
 * Viewer Bridge Message Protocol
 *
 * Defines the typed postMessage protocol between the orchestrator
 * (Starling web-client) and the viewer (digital-viewer) windows.
 *
 * Security: All messages are validated by origin before processing.
 */

// ============ Orchestrator → Viewer Messages ============

/** Viewer mode — determines DX features, case source, and visual context */
export type ViewerMode = 'clinical' | 'educational';

/** Initialization payload sent after viewer signals ready */
export interface InitPayload {
	/** JWT access token for tile server authentication */
	token: string;
	/** Case identifier to load */
	caseId: string;
	/** Accession number */
	accession: string;
	/** Tile server base URL (e.g., '/tiles' behind proxy, or full URL) */
	tileServerUrl: string;
	/** Session awareness WebSocket URL (e.g., '/ws' behind proxy) */
	sessionServiceUrl: string;
	/** Authenticated user identifier */
	userId: string;
	/** Orchestrator's origin for reply validation */
	orchestratorOrigin: string;
	/** Viewer mode — clinical enables DX mode, educational disables it */
	mode?: ViewerMode;
}

/** Audit event reported by the viewer */
export interface ViewerAuditEvent {
	/** Event type */
	eventType: 'VIEWER_CASE_OPENED' | 'VIEWER_SLIDE_VIEWED' | 'VIEWER_CASE_CLOSED' | 'VIEWER_ANNOTATION_CREATED';
	/** Case identifier */
	caseId: string;
	/** Slide identifier (null for case-level events) */
	slideId?: string;
	/** Accession number */
	accessionNumber: string;
	/** When the event occurred (ISO 8601) */
	occurredAt: string;
	/** Additional event-specific metadata */
	metadata?: Record<string, unknown>;
}

/** Messages sent from orchestrator to viewer */
export type OrchestratorMessage =
	| { type: 'orchestrator:init'; payload: InitPayload }
	| { type: 'orchestrator:token-refresh'; payload: { token: string } }
	| { type: 'orchestrator:case-change'; payload: { caseId: string; accession: string } }
	| { type: 'orchestrator:focus'; payload: { state: 'active' | 'blurred' } }
	| { type: 'orchestrator:logout'; payload: Record<string, never> }
	| { type: 'orchestrator:heartbeat'; payload: { timestamp: number } };

/** Messages sent from viewer to orchestrator */
export type ViewerMessage =
	| { type: 'viewer:ready'; payload: Record<string, never> }
	| { type: 'viewer:heartbeat-ack'; payload: { timestamp: number } }
	| { type: 'viewer:case-loaded'; payload: { caseId: string; slideCount: number } }
	| { type: 'viewer:error'; payload: { code: string; message: string } }
	| { type: 'viewer:audit-event'; payload: ViewerAuditEvent };

/** All message types (for type guards) */
export type BridgeMessage = OrchestratorMessage | ViewerMessage;

/** Extract the payload type for a given message type */
export type PayloadOf<T extends BridgeMessage['type']> = Extract<BridgeMessage, { type: T }>['payload'];

// ============ Future Extensions (not yet handled) ============

/** Future orchestrator messages for collaboration and search */
// | { type: 'orchestrator:search-results'; payload: { cases: unknown[] } }
// | { type: 'orchestrator:collaboration-join'; payload: { sessionId: string; peers: string[] } }
// | { type: 'orchestrator:annotation-sync'; payload: { annotations: unknown[] } }

/** Future viewer messages for annotations and viewport sync */
// | { type: 'viewer:annotation-saved'; payload: { annotationId: string } }
// | { type: 'viewer:viewport-sync'; payload: { center: { x: number; y: number }; zoom: number; rotation: number } }
// | { type: 'viewer:annotation-deleted'; payload: { annotationId: string } }

/** Configuration for launching the viewer window */
export interface ViewerLaunchConfig {
	/** JWT access token */
	token: string;
	/** Case identifier */
	caseId: string;
	/** Accession number */
	accession: string;
	/** Tile server URL */
	tileServerUrl: string;
	/** Session service WebSocket URL */
	sessionServiceUrl: string;
	/** User ID */
	userId: string;
	/** URL of the viewer application */
	viewerUrl: string;
	/** Allowed origin of the viewer (for postMessage validation) */
	viewerOrigin: string;
	/** Viewer mode — clinical (default) or educational */
	mode?: ViewerMode;
}
