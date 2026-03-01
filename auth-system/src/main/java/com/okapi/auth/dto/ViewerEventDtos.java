package com.okapi.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs for the viewer audit event pipeline.
 *
 * Viewer events flow: viewer window → postMessage → orchestrator → POST /api/viewer-events → audit_event table
 */
public final class ViewerEventDtos {

    private ViewerEventDtos() {}

    /**
     * A single viewer audit event.
     *
     * @param eventType       One of VIEWER_CASE_OPENED, VIEWER_SLIDE_VIEWED, VIEWER_CASE_CLOSED, VIEWER_ANNOTATION_CREATED
     * @param caseId          Case identifier
     * @param slideId         Slide identifier (null for case-level events)
     * @param accessionNumber Accession number of the case
     * @param occurredAt      When the event occurred in the viewer (ISO 8601)
     * @param metadata        Additional event-specific metadata (slide count, duration, etc.)
     */
    public record ViewerEvent(
            String eventType,
            String caseId,
            String slideId,
            String accessionNumber,
            Instant occurredAt,
            Map<String, Object> metadata
    ) {}

    /**
     * Batch submission wrapper — the orchestrator batches events before flushing.
     *
     * @param events List of viewer events to record
     */
    public record ViewerEventBatch(
            List<ViewerEvent> events
    ) {}

    /** Allowed viewer event types for validation */
    public static final List<String> ALLOWED_EVENT_TYPES = List.of(
            "VIEWER_CASE_OPENED",
            "VIEWER_SLIDE_VIEWED",
            "VIEWER_CASE_CLOSED",
            "VIEWER_ANNOTATION_CREATED"
    );
}
