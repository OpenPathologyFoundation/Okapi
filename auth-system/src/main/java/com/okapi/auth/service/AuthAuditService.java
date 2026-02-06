package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.AuditEventEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.AuditEventRepository;
import com.okapi.auth.repository.IdentityRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthAuditService {

    private final AuditEventRepository auditEventRepository;
    private final IdentityRepository identityRepository;

    public AuthAuditService(
            AuditEventRepository auditEventRepository,
            IdentityRepository identityRepository) {
        this.auditEventRepository = auditEventRepository;
        this.identityRepository = identityRepository;
    }

    public void recordLoginSuccess(Identity identity) {
        AuditEventEntity event = baseEvent(identity, "AUTHN_LOGIN_SUCCESS", "SUCCESS", null);
        auditEventRepository.save(event);
    }

    public void recordLogout(Identity identity, HttpServletRequest request) {
        AuditEventEntity event = baseEvent(identity, "AUTHN_LOGOUT", "SUCCESS", request);
        auditEventRepository.save(event);
    }

    public void recordDeviceTrusted(Identity identity, UUID deviceId) {
        AuditEventEntity event = baseEvent(identity, "AUTHN_DEVICE_TRUSTED", "SUCCESS", null);
        event.setTargetEntityType("SESSION_DEVICE");
        event.setTargetEntityId(deviceId);
        auditEventRepository.save(event);
    }

    public void recordDeviceRevoked(Identity identity, UUID deviceId) {
        AuditEventEntity event = baseEvent(identity, "AUTHN_DEVICE_REVOKED", "SUCCESS", null);
        event.setTargetEntityType("SESSION_DEVICE");
        event.setTargetEntityId(deviceId);
        auditEventRepository.save(event);
    }

    public void recordBreakGlassInvoked(Identity identity, UUID grantId, String entityType, UUID entityId, String reason) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_BREAK_GLASS_INVOKED", "SUCCESS", null);
        event.setTargetEntityType(entityType);
        event.setTargetEntityId(entityId);
        event.setDetails("Break-glass invoked");
        event.setMetadata(Map.of(
                "grant_id", grantId,
                "reason", reason
        ));
        auditEventRepository.save(event);
    }

    public void recordBreakGlassRevoked(Identity identity, UUID grantId) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_BREAK_GLASS_REVOKED", "SUCCESS", null);
        event.setTargetEntityType("BREAK_GLASS_GRANT");
        event.setTargetEntityId(grantId);
        auditEventRepository.save(event);
    }

    public void recordResearchGrantCreated(Identity identity, UUID grantId, String phiAccessLevel, String protocolId) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_RESEARCH_GRANT_CREATED", "SUCCESS", null);
        event.setTargetEntityType("RESEARCH_ACCESS_GRANT");
        event.setTargetEntityId(grantId);
        event.setMetadata(Map.of(
                "phi_access_level", phiAccessLevel,
                "protocol_id", protocolId
        ));
        auditEventRepository.save(event);
    }

    public void recordResearchGrantRevoked(Identity identity, UUID grantId, String reason) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_RESEARCH_GRANT_REVOKED", "SUCCESS", null);
        event.setTargetEntityType("RESEARCH_ACCESS_GRANT");
        event.setTargetEntityId(grantId);
        if (reason != null && !reason.isBlank()) {
            event.setOutcomeReason(reason);
        }
        auditEventRepository.save(event);
    }

    public void recordRoleAssigned(Identity identity, UUID roleId, String roleName, String source) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_ROLE_ASSIGNED", "SUCCESS", null);
        event.setTargetEntityType("ROLE");
        event.setTargetEntityId(roleId);
        event.setDetails("Role assigned");
        event.setMetadata(Map.of(
                "role_name", roleName,
                "assignment_source", source == null ? "UNKNOWN" : source
        ));
        auditEventRepository.save(event);
    }

    public void recordRoleRevoked(Identity identity, UUID roleId, String roleName, String source) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_ROLE_REVOKED", "SUCCESS", null);
        event.setTargetEntityType("ROLE");
        event.setTargetEntityId(roleId);
        event.setDetails("Role revoked");
        event.setMetadata(Map.of(
                "role_name", roleName,
                "assignment_source", source == null ? "UNKNOWN" : source
        ));
        auditEventRepository.save(event);
    }

    public void recordPermissionDenied(
            Identity identity,
            String permission,
            String targetEntityType,
            UUID targetEntityId,
            String detail) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_PERMISSION_DENIED", "DENY", null);
        event.setTargetEntityType(targetEntityType);
        event.setTargetEntityId(targetEntityId);
        event.setDetails(detail == null ? "Permission denied" : detail);
        event.setMetadata(Map.of(
                "permission", permission == null ? "UNKNOWN" : permission
        ));
        auditEventRepository.save(event);
    }

    public void recordAccessDenied(Identity identity, HttpServletRequest request, String reason) {
        AuditEventEntity event = baseEvent(identity, "AUTHZ_PERMISSION_DENIED", "DENY", request);
        event.setDetails(reason == null ? "Access denied" : reason);
        if (request != null) {
            event.setMetadata(Map.of(
                    "path", request.getRequestURI(),
                    "method", request.getMethod()
            ));
        }
        auditEventRepository.save(event);
    }

    public void recordIdentityActivated(Identity actor, UUID targetIdentityId) {
        AuditEventEntity event = baseEvent(actor, "ADMIN_IDENTITY_ACTIVATED", "SUCCESS", null);
        event.setTargetEntityType("IDENTITY");
        event.setTargetIdentityId(targetIdentityId);
        auditEventRepository.save(event);
    }

    public void recordIdentityDeactivated(Identity actor, UUID targetIdentityId) {
        AuditEventEntity event = baseEvent(actor, "ADMIN_IDENTITY_DEACTIVATED", "SUCCESS", null);
        event.setTargetEntityType("IDENTITY");
        event.setTargetIdentityId(targetIdentityId);
        auditEventRepository.save(event);
    }

    public void recordIdpMappingCreated(Identity actor, UUID mappingId, String groupName, List<String> roleNames) {
        AuditEventEntity event = baseEvent(actor, "ADMIN_IDP_MAPPING_CREATED", "SUCCESS", null);
        event.setTargetEntityType("IDP_GROUP");
        event.setTargetEntityId(mappingId);
        event.setDetails("IdP mapping created: " + groupName);
        event.setMetadata(Map.of(
                "group_name", groupName,
                "role_names", String.join(",", roleNames)
        ));
        auditEventRepository.save(event);
    }

    public void recordIdpMappingDeleted(Identity actor, UUID mappingId, String groupName) {
        AuditEventEntity event = baseEvent(actor, "ADMIN_IDP_MAPPING_DELETED", "SUCCESS", null);
        event.setTargetEntityType("IDP_GROUP");
        event.setTargetEntityId(mappingId);
        event.setDetails("IdP mapping deleted: " + groupName);
        auditEventRepository.save(event);
    }

    private AuditEventEntity baseEvent(Identity identity, String eventType, String outcome, HttpServletRequest request) {
        Optional<IdentityEntity> storedIdentity = identityRepository
                .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject());

        UUID actorIdentityId = storedIdentity.map(IdentityEntity::getIdentityId).orElse(null);
        String ipAddress = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        return AuditEventEntity.builder()
                .eventType(eventType)
                .actorIdentityId(actorIdentityId)
                .actorProviderId(identity.getProviderId())
                .actorExternalSubject(identity.getExternalSubject())
                .outcome(outcome)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(Map.of("source", "auth-system"))
                .build();
    }
}
