package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.ResearchAccessGrantEntity;
import com.okapi.auth.service.ResearchGrantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/research-grants")
public class ResearchGrantController {

    private final ResearchGrantService researchGrantService;

    public ResearchGrantController(ResearchGrantService researchGrantService) {
        this.researchGrantService = researchGrantService;
    }

    public record ResearchGrantRequest(
            UUID identityId,
            String scopeType,
            UUID scopeEntityId,
            Map<String, Object> scopeFilter,
            String protocolId,
            String reason,
            String phiAccessLevel,
            OffsetDateTime expiresAt) {
    }

    @PostMapping
    public ResearchAccessGrantEntity createGrant(
            @AuthenticationPrincipal Object principal,
            @RequestBody ResearchGrantRequest request) {
        Identity approver = requireIdentity(principal);
        if (request == null || request.identityId() == null || request.scopeType() == null
                || request.reason() == null || request.phiAccessLevel() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
        return researchGrantService.createGrant(
                approver,
                request.identityId(),
                request.scopeType(),
                request.scopeEntityId(),
                request.scopeFilter(),
                request.protocolId(),
                request.reason(),
                request.phiAccessLevel(),
                request.expiresAt());
    }

    @GetMapping
    public List<ResearchAccessGrantEntity> listGrants(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) UUID identityId) {
        Identity requester = requireIdentity(principal);
        return researchGrantService.listGrants(requester, identityId);
    }

    @DeleteMapping("/{grantId}")
    public Map<String, Object> revokeGrant(
            @AuthenticationPrincipal Object principal,
            @PathVariable UUID grantId,
            @RequestParam(required = false) String reason) {
        Identity approver = requireIdentity(principal);
        researchGrantService.revokeGrant(approver, grantId, reason);
        return Map.of("revoked", true, "grantId", grantId);
    }

    private Identity requireIdentity(Object principal) {
        if (principal instanceof Identity identity) {
            return identity;
        }
        throw new IllegalStateException("unauthenticated");
    }
}
