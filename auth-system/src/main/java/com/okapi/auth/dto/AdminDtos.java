package com.okapi.auth.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    // ── Response Records ───────────────────────────────────────────

    public record IdentitySummary(
            UUID identityId,
            String displayName,
            String displayShort,
            String email,
            String username,
            boolean isActive,
            List<String> roles,
            OffsetDateTime lastSeenAt,
            OffsetDateTime createdAt) {
    }

    public record IdentityDetail(
            UUID identityId,
            String providerId,
            String externalSubject,
            String displayName,
            String displayShort,
            String givenName,
            String familyName,
            String middleName,
            String prefix,
            String suffix,
            String email,
            String username,
            boolean isActive,
            List<RoleSummary> roles,
            Map<String, Object> attributes,
            OffsetDateTime lastSeenAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record RoleSummary(
            UUID roleId,
            String name,
            String description,
            boolean isSystem) {
    }

    public record RolePermissionRow(
            UUID roleId,
            String roleName,
            String roleDescription,
            boolean isSystem,
            List<String> permissions) {
    }

    public record IdpMappingSummary(
            UUID idpGroupId,
            String providerId,
            String groupName,
            String description,
            List<String> roleNames) {
    }

    public record AuditEventSummary(
            UUID eventId,
            OffsetDateTime occurredAt,
            String eventType,
            UUID actorIdentityId,
            String actorProviderId,
            String actorExternalSubject,
            String targetEntityType,
            UUID targetEntityId,
            UUID targetIdentityId,
            String outcome,
            String outcomeReason,
            String details,
            Map<String, Object> metadata) {
    }

    public record BreakGlassGrantSummary(
            UUID grantId,
            UUID identityId,
            String scopeEntityType,
            UUID scopeEntityId,
            String reasonCode,
            String justification,
            OffsetDateTime grantedAt,
            OffsetDateTime expiresAt) {
    }

    public record ResearchGrantSummary(
            UUID grantId,
            UUID identityId,
            String scopeType,
            UUID scopeEntityId,
            String protocolId,
            String reason,
            String phiAccessLevel,
            OffsetDateTime grantedAt,
            OffsetDateTime expiresAt) {
    }

    public record DeviceSummary(
            UUID deviceId,
            UUID identityId,
            String deviceFingerprintHash,
            OffsetDateTime firstSeenAt,
            OffsetDateTime lastSeenAt,
            OffsetDateTime trustedUntil,
            OffsetDateTime revokedAt,
            Map<String, Object> metadata) {
    }

    public record DashboardSummary(
            long totalIdentities,
            long activeIdentities,
            long totalRoles,
            long activeDevices,
            long activeBreakGlassGrants,
            long activeResearchGrants) {
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    // ── Request Records ────────────────────────────────────────────

    public record IdentityStatusRequest(boolean active) {
    }

    public record RoleAssignmentRequest(UUID roleId, String justification) {
    }

    public record IdpMappingCreateRequest(
            String providerId,
            String groupName,
            String description,
            List<UUID> roleIds) {
    }

    public record ResearchGrantRevokeRequest(String reason) {
    }
}
