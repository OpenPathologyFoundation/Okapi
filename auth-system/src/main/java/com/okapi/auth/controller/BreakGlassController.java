package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.BreakGlassGrantEntity;
import com.okapi.auth.service.BreakGlassService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/break-glass")
public class BreakGlassController {

    private final BreakGlassService breakGlassService;

    public BreakGlassController(BreakGlassService breakGlassService) {
        this.breakGlassService = breakGlassService;
    }

    public record BreakGlassRequest(
            UUID scopeEntityId,
            String scopeEntityType,
            String reasonCode,
            String justification,
            OffsetDateTime expiresAt,
            Map<String, Object> metadata) {
    }

    @PostMapping
    public BreakGlassGrantEntity createGrant(
            @AuthenticationPrincipal Object principal,
            @RequestBody BreakGlassRequest request) {
        Identity identity = requireIdentity(principal);
        if (request == null || request.scopeEntityId() == null || request.scopeEntityType() == null
                || request.reasonCode() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
        return breakGlassService.createGrant(
                identity,
                request.scopeEntityId(),
                request.scopeEntityType(),
                request.reasonCode(),
                request.justification(),
                request.expiresAt(),
                request.metadata());
    }

    @GetMapping
    public List<BreakGlassGrantEntity> listMyGrants(@AuthenticationPrincipal Object principal) {
        Identity identity = requireIdentity(principal);
        return breakGlassService.listGrantsForIdentity(identity);
    }

    @DeleteMapping("/{grantId}")
    public Map<String, Object> revokeGrant(
            @AuthenticationPrincipal Object principal,
            @PathVariable UUID grantId) {
        Identity identity = requireIdentity(principal);
        breakGlassService.revokeGrant(identity, grantId);
        return Map.of("revoked", true, "grantId", grantId);
    }

    private Identity requireIdentity(Object principal) {
        if (principal instanceof Identity identity) {
            return identity;
        }
        throw new IllegalStateException("unauthenticated");
    }
}
