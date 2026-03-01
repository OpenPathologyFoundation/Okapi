package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.model.db.ResearchAccessGrantEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.ResearchAccessGrantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResearchGrantService {

    private final ResearchAccessGrantRepository researchAccessGrantRepository;
    private final IdentityRepository identityRepository;
    private final AuthzPermissionService authzPermissionService;
    private final AuthAuditService authAuditService;
    private final long defaultTtlDays;

    public ResearchGrantService(
            ResearchAccessGrantRepository researchAccessGrantRepository,
            IdentityRepository identityRepository,
            AuthzPermissionService authzPermissionService,
            AuthAuditService authAuditService,
            @Value("${okapi.research.grant.ttl-days:90}") long defaultTtlDays) {
        this.researchAccessGrantRepository = researchAccessGrantRepository;
        this.identityRepository = identityRepository;
        this.authzPermissionService = authzPermissionService;
        this.authAuditService = authAuditService;
        this.defaultTtlDays = defaultTtlDays;
    }

    public ResearchAccessGrantEntity createGrant(
            Identity approver,
            UUID granteeIdentityId,
            String scopeType,
            UUID scopeEntityId,
            Map<String, Object> scopeFilter,
            String protocolId,
        String reason,
        String phiAccessLevel,
        OffsetDateTime expiresAt) {

        authzPermissionService.requirePermission(
                approver,
                "RESEARCH_APPROVE",
                "RESEARCH_ACCESS_GRANT",
                scopeEntityId,
                "Research grant approval denied");

        UUID approverId = resolveIdentityId(approver);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime effectiveExpiry = expiresAt != null ? expiresAt : now.plusDays(defaultTtlDays);

        ResearchAccessGrantEntity grant = ResearchAccessGrantEntity.builder()
                .identityId(granteeIdentityId)
                .scopeType(scopeType)
                .scopeEntityId(scopeEntityId)
                .scopeFilter(scopeFilter)
                .protocolId(protocolId)
                .reason(reason)
                .approvedByIdentityId(approverId)
                .phiAccessLevel(phiAccessLevel)
                .grantedAt(now)
                .expiresAt(effectiveExpiry)
                .build();

        ResearchAccessGrantEntity saved = researchAccessGrantRepository.save(grant);
        authAuditService.recordResearchGrantCreated(approver, saved.getGrantId(), phiAccessLevel, protocolId);
        return saved;
    }

    public ResearchAccessGrantEntity revokeGrant(Identity approver, UUID grantId, String reason) {
        authzPermissionService.requirePermission(
                approver,
                "RESEARCH_APPROVE",
                "RESEARCH_ACCESS_GRANT",
                grantId,
                "Research grant revocation denied");

        ResearchAccessGrantEntity grant = researchAccessGrantRepository.findById(grantId)
                .orElseThrow(() -> new IllegalArgumentException("Research grant not found"));

        UUID approverId = resolveIdentityId(approver);
        grant.setRevokedAt(OffsetDateTime.now());
        grant.setRevokedByIdentityId(approverId);
        grant.setRevocationReason(reason);
        ResearchAccessGrantEntity saved = researchAccessGrantRepository.save(grant);
        authAuditService.recordResearchGrantRevoked(approver, grantId, reason);
        return saved;
    }

    public List<ResearchAccessGrantEntity> listGrants(Identity requester, UUID granteeIdentityId) {
        authzPermissionService.requirePermission(
                requester,
                "RESEARCH_APPROVE",
                "RESEARCH_ACCESS_GRANT",
                granteeIdentityId,
                "Research grant listing denied");
        if (granteeIdentityId == null) {
            return researchAccessGrantRepository.findAll();
        }
        return researchAccessGrantRepository.findByIdentityId(granteeIdentityId);
    }

    private UUID resolveIdentityId(Identity identity) {
        IdentityEntity entity = identityRepository
                .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                .orElseThrow(() -> new IllegalStateException("Identity not found"));
        return entity.getIdentityId();
    }
}
