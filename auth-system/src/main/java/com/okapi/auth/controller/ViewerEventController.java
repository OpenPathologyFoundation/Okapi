package com.okapi.auth.controller;

import com.okapi.auth.dto.ViewerEventDtos;
import com.okapi.auth.dto.ViewerEventDtos.ViewerEvent;
import com.okapi.auth.dto.ViewerEventDtos.ViewerEventBatch;
import com.okapi.auth.model.Identity;
import com.okapi.auth.service.AuthAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives batched viewer audit events from the orchestrator.
 *
 * The viewer window sends events (case opened, slide viewed, case closed) via
 * postMessage to the orchestrator. The orchestrator's ViewerBridge batches them
 * and flushes here every 5 seconds.
 *
 * Authentication: session cookie (same as all Okapi endpoints).
 * The actor is always the authenticated session principal, not a field in the request.
 */
@RestController
@RequestMapping("/api/viewer-events")
public class ViewerEventController {

    private static final Logger log = LoggerFactory.getLogger(ViewerEventController.class);

    private final AuthAuditService auditService;

    public ViewerEventController(AuthAuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<Void> recordViewerEvents(
            @AuthenticationPrincipal Identity identity,
            @RequestBody ViewerEventBatch batch,
            HttpServletRequest request) {

        if (identity == null) {
            return ResponseEntity.status(401).build();
        }

        if (batch.events() == null || batch.events().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        int accepted = 0;
        for (ViewerEvent event : batch.events()) {
            // Validate event type
            if (event.eventType() == null || !ViewerEventDtos.ALLOWED_EVENT_TYPES.contains(event.eventType())) {
                log.warn("[ViewerEvent] Rejected unknown event type: {}", event.eventType());
                continue;
            }
            if (event.caseId() == null || event.caseId().isBlank()) {
                log.warn("[ViewerEvent] Rejected event with missing caseId");
                continue;
            }

            auditService.recordViewerEvent(identity, event, request);
            accepted++;
        }

        log.debug("[ViewerEvent] Accepted {}/{} events from user {}",
                accepted, batch.events().size(), identity.getExternalSubject());

        return ResponseEntity.noContent().build();
    }
}
