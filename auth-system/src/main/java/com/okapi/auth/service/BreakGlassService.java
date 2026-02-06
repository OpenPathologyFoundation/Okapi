package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.BreakGlassGrantEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.BreakGlassGrantRepository;
import com.okapi.auth.repository.IdentityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BreakGlassService {

    private final BreakGlassGrantRepository breakGlassGrantRepository;
    private final IdentityRepository identityRepository;
    private final AuthzPermissionService authzPermissionService;
    private final AuthAuditService authAuditService;
    private final long defaultTtlHours;

    public BreakGlassService(
            BreakGlassGrantRepository breakGlassGrantRepository,
            IdentityRepository identityRepository,
            AuthzPermissionService authzPermissionService,
            AuthAuditService authAuditService,
            @Value("${okapi.break-glass.ttl-hours:24}") long defaultTtlHours) {
        this.breakGlassGrantRepository = breakGlassGrantRepository;
        this.identityRepository = identityRepository;
        this.authzPermissionService = authzPermissionService;
        this.authAuditService = authAuditService;
        this.defaultTtlHours = defaultTtlHours;
    }

    public BreakGlassGrantEntity createGrant(
            Identity identity,
            UUID scopeEntityId,
        String scopeEntityType,
        String reasonCode,
        String justification,
        OffsetDateTime expiresAt,
        Map<String, Object> metadata) {
        authzPermissionService.requirePermission(
                identity,
                "BREAK_GLASS_INVOKE",
                scopeEntityType,
                scopeEntityId,
                "Break-glass invoke denied");

        UUID identityId = resolveIdentityId(identity);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime effectiveExpiry = expiresAt != null ? expiresAt : now.plusHours(defaultTtlHours);

        BreakGlassGrantEntity grant = BreakGlassGrantEntity.builder()
                .identityId(identityId)
                .scopeEntityId(scopeEntityId)
                .scopeEntityType(scopeEntityType)
                .reasonCode(reasonCode)
                .justification(justification)
                .grantedAt(now)
                .expiresAt(effectiveExpiry)
                .metadata(metadata == null ? Map.of() : metadata)
                .build();

        BreakGlassGrantEntity saved = breakGlassGrantRepository.save(grant);
        authAuditService.recordBreakGlassInvoked(identity, saved.getGrantId(), scopeEntityType, scopeEntityId, reasonCode);
        return saved;
    }

    public BreakGlassGrantEntity revokeGrant(Identity identity, UUID grantId) {
        BreakGlassGrantEntity grant = breakGlassGrantRepository.findById(grantId)
                .orElseThrow(() -> new IllegalArgumentException("Break-glass grant not found"));

        UUID identityId = resolveIdentityId(identity);
        boolean isAdmin = authzPermissionService.hasPermission(identity, "ADMIN_USERS");
        if (!isAdmin && !identityId.equals(grant.getIdentityId())) {
            authAuditService.recordPermissionDenied(
                    identity,
                    "ADMIN_USERS",
                    "BREAK_GLASS_GRANT",
                    grantId,
                    "Break-glass revoke denied");
            throw new org.springframework.security.access.AccessDeniedException("Not permitted to revoke this grant");
        }

        grant.setRevokedAt(OffsetDateTime.now());
        grant.setRevokedByIdentityId(identityId);
        BreakGlassGrantEntity saved = breakGlassGrantRepository.save(grant);
        authAuditService.recordBreakGlassRevoked(identity, grantId);
        return saved;
    }

    public List<BreakGlassGrantEntity> listGrantsForIdentity(Identity identity) {
        UUID identityId = resolveIdentityId(identity);
        return breakGlassGrantRepository.findByIdentityId(identityId);
    }

    private UUID resolveIdentityId(Identity identity) {
        IdentityEntity entity = identityRepository
                .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                .orElseThrow(() -> new IllegalStateException("Identity not found"));
        return entity.getIdentityId();
    }
}
