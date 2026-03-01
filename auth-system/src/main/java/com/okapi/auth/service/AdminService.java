package com.okapi.auth.service;

import com.okapi.auth.dto.AdminDtos.*;
import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.*;
import com.okapi.auth.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final IdentityRepository identityRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final IdpGroupMappingRepository idpGroupMappingRepository;
    private final AuditEventRepository auditEventRepository;
    private final BreakGlassGrantRepository breakGlassGrantRepository;
    private final ResearchAccessGrantRepository researchAccessGrantRepository;
    private final SessionDeviceRepository sessionDeviceRepository;
    private final AuthzPermissionService authzPermissionService;
    private final AuthAuditService authAuditService;

    public AdminService(
            IdentityRepository identityRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            IdpGroupMappingRepository idpGroupMappingRepository,
            AuditEventRepository auditEventRepository,
            BreakGlassGrantRepository breakGlassGrantRepository,
            ResearchAccessGrantRepository researchAccessGrantRepository,
            SessionDeviceRepository sessionDeviceRepository,
            AuthzPermissionService authzPermissionService,
            AuthAuditService authAuditService) {
        this.identityRepository = identityRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.idpGroupMappingRepository = idpGroupMappingRepository;
        this.auditEventRepository = auditEventRepository;
        this.breakGlassGrantRepository = breakGlassGrantRepository;
        this.researchAccessGrantRepository = researchAccessGrantRepository;
        this.sessionDeviceRepository = sessionDeviceRepository;
        this.authzPermissionService = authzPermissionService;
        this.authAuditService = authAuditService;
    }

    // ── Identities ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<IdentitySummary> listIdentities(Identity actor, String search, Boolean active, int page, int size) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "IDENTITY", null, "listIdentities");
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayName").ascending());
        Page<IdentityEntity> result;

        if (search != null && !search.isBlank()) {
            result = identityRepository.searchByTerm(search.trim(), pageable);
        } else if (active != null) {
            result = identityRepository.findByIsActive(active, pageable);
        } else {
            result = identityRepository.findAll(pageable);
        }

        List<IdentitySummary> content = result.getContent().stream()
                .map(this::toIdentitySummary)
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public IdentityDetail getIdentity(Identity actor, UUID identityId) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "IDENTITY", identityId, "getIdentity");
        IdentityEntity entity = identityRepository.findById(identityId)
                .orElseThrow(() -> new IllegalStateException("Identity not found: " + identityId));
        return toIdentityDetail(entity);
    }

    @Transactional
    public void setIdentityActive(Identity actor, UUID identityId, boolean active) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "IDENTITY", identityId, "setIdentityActive");
        IdentityEntity entity = identityRepository.findById(identityId)
                .orElseThrow(() -> new IllegalStateException("Identity not found: " + identityId));
        entity.setActive(active);
        entity.setUpdatedAt(OffsetDateTime.now());
        identityRepository.save(entity);

        if (active) {
            authAuditService.recordIdentityActivated(actor, identityId);
        } else {
            authAuditService.recordIdentityDeactivated(actor, identityId);
        }
    }

    // ── Role Assignment ────────────────────────────────────────────

    @Transactional
    public void assignRole(Identity actor, UUID identityId, UUID roleId, String justification) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "IDENTITY", identityId, "assignRole");
        IdentityEntity identity = identityRepository.findById(identityId)
                .orElseThrow(() -> new IllegalStateException("Identity not found: " + identityId));
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleId));

        identity.getRoles().add(role);
        identity.setUpdatedAt(OffsetDateTime.now());
        identityRepository.save(identity);

        authAuditService.recordRoleAssigned(actor, roleId, role.getName(), "LOCAL_ADMIN");
    }

    @Transactional
    public void revokeRole(Identity actor, UUID identityId, UUID roleId, String justification) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "IDENTITY", identityId, "revokeRole");
        IdentityEntity identity = identityRepository.findById(identityId)
                .orElseThrow(() -> new IllegalStateException("Identity not found: " + identityId));
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleId));

        identity.getRoles().removeIf(r -> r.getRoleId().equals(roleId));
        identity.setUpdatedAt(OffsetDateTime.now());
        identityRepository.save(identity);

        authAuditService.recordRoleRevoked(actor, roleId, role.getName(), "LOCAL_ADMIN");
    }

    // ── Roles ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RoleSummary> listRoles(Identity actor) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "ROLE", null, "listRoles");
        return roleRepository.findAll(Sort.by("name")).stream()
                .map(r -> new RoleSummary(r.getRoleId(), r.getName(), r.getDescription(), r.isSystem()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolePermissionRow> getRolePermissionMatrix(Identity actor) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "ROLE", null, "getRolePermissionMatrix");
        List<RoleEntity> roles = roleRepository.findAll(Sort.by("name"));
        return roles.stream().map(role -> {
            List<String> permNames = permissionRepository
                    .findPermissionNamesByRoleNames(List.of(role.getName()))
                    .stream().sorted().toList();
            return new RolePermissionRow(
                    role.getRoleId(), role.getName(), role.getDescription(),
                    role.isSystem(), permNames);
        }).toList();
    }

    // ── IdP Mappings ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IdpMappingSummary> listIdpMappings(Identity actor) {
        authzPermissionService.requirePermission(actor, "ADMIN_SYSTEM", "IDP_MAPPING", null, "listIdpMappings");
        List<Object[]> rows = idpGroupMappingRepository.findAllWithRoleNames();
        return rows.stream().map(row -> {
            UUID id = (UUID) row[0];
            String providerId = (String) row[1];
            String groupName = (String) row[2];
            String description = (String) row[3];
            String roleNamesStr = (String) row[4];
            List<String> roleNames = roleNamesStr != null && !roleNamesStr.isBlank()
                    ? Arrays.asList(roleNamesStr.split(","))
                    : List.of();
            return new IdpMappingSummary(id, providerId, groupName, description, roleNames);
        }).toList();
    }

    @Transactional
    public IdpMappingSummary createIdpMapping(Identity actor, IdpMappingCreateRequest request) {
        authzPermissionService.requirePermission(actor, "ADMIN_SYSTEM", "IDP_MAPPING", null, "createIdpMapping");

        IdpGroupMappingEntity entity = IdpGroupMappingEntity.builder()
                .providerId(request.providerId())
                .groupName(request.groupName())
                .description(request.description())
                .build();

        // Resolve role entities and associate them before saving
        List<String> roleNames = new ArrayList<>();
        if (request.roleIds() != null) {
            Set<RoleEntity> roleEntities = new HashSet<>();
            for (UUID roleId : request.roleIds()) {
                roleRepository.findById(roleId).ifPresent(r -> {
                    roleEntities.add(r);
                    roleNames.add(r.getName());
                });
            }
            entity.setRoles(roleEntities);
        }

        entity = idpGroupMappingRepository.save(entity);

        authAuditService.recordIdpMappingCreated(actor, entity.getIdpGroupId(),
                request.groupName(), roleNames);

        return new IdpMappingSummary(entity.getIdpGroupId(), entity.getProviderId(),
                entity.getGroupName(), entity.getDescription(), roleNames);
    }

    @Transactional
    public void deleteIdpMapping(Identity actor, UUID mappingId) {
        authzPermissionService.requirePermission(actor, "ADMIN_SYSTEM", "IDP_MAPPING", mappingId, "deleteIdpMapping");
        IdpGroupMappingEntity entity = idpGroupMappingRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalStateException("IdP mapping not found: " + mappingId));
        idpGroupMappingRepository.delete(entity);
        authAuditService.recordIdpMappingDeleted(actor, mappingId, entity.getGroupName());
    }

    // ── Audit Events ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AuditEventSummary> listAuditEvents(
            Identity actor, String eventType, UUID actorId, String targetType, String outcome,
            OffsetDateTime from, OffsetDateTime to, int page, int size) {
        authzPermissionService.requirePermission(actor, "ADMIN_AUDIT", "AUDIT_EVENT", null, "listAuditEvents");
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);

        Specification<AuditEventEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorIdentityId"), actorId));
            }
            if (targetType != null && !targetType.isBlank()) {
                predicates.add(cb.equal(root.get("targetEntityType"), targetType));
            }
            if (outcome != null && !outcome.isBlank()) {
                predicates.add(cb.equal(root.get("outcome"), outcome));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        Page<AuditEventEntity> result = auditEventRepository.findAll(spec, pageable);

        List<AuditEventSummary> content = result.getContent().stream()
                .map(this::toAuditEventSummary)
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    // ── Break-Glass Grants ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BreakGlassGrantSummary> listActiveBreakGlassGrants(Identity actor) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "BREAK_GLASS_GRANT", null, "listActiveBreakGlassGrants");
        return breakGlassGrantRepository.findByRevokedAtIsNullAndExpiresAtAfter(OffsetDateTime.now())
                .stream()
                .map(g -> new BreakGlassGrantSummary(
                        g.getGrantId(), g.getIdentityId(), g.getScopeEntityType(),
                        g.getScopeEntityId(), g.getReasonCode(), g.getJustification(),
                        g.getGrantedAt(), g.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void revokeBreakGlassGrant(Identity actor, UUID grantId) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "BREAK_GLASS_GRANT", grantId, "revokeBreakGlassGrant");
        BreakGlassGrantEntity grant = breakGlassGrantRepository.findById(grantId)
                .orElseThrow(() -> new IllegalStateException("Break-glass grant not found: " + grantId));
        var stored = identityRepository.findByProviderIdAndExternalSubject(
                actor.getProviderId(), actor.getExternalSubject());
        grant.setRevokedAt(OffsetDateTime.now());
        grant.setRevokedByIdentityId(stored.map(IdentityEntity::getIdentityId).orElse(null));
        breakGlassGrantRepository.save(grant);
        authAuditService.recordBreakGlassRevoked(actor, grantId);
    }

    // ── Research Grants ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResearchGrantSummary> listActiveResearchGrants(Identity actor) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "RESEARCH_GRANT", null, "listActiveResearchGrants");
        return researchAccessGrantRepository.findByRevokedAtIsNullAndExpiresAtAfter(OffsetDateTime.now())
                .stream()
                .map(g -> new ResearchGrantSummary(
                        g.getGrantId(), g.getIdentityId(), g.getScopeType(),
                        g.getScopeEntityId(), g.getProtocolId(), g.getReason(),
                        g.getPhiAccessLevel(), g.getGrantedAt(), g.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void revokeResearchGrant(Identity actor, UUID grantId, String reason) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "RESEARCH_GRANT", grantId, "revokeResearchGrant");
        ResearchAccessGrantEntity grant = researchAccessGrantRepository.findById(grantId)
                .orElseThrow(() -> new IllegalStateException("Research grant not found: " + grantId));
        var stored = identityRepository.findByProviderIdAndExternalSubject(
                actor.getProviderId(), actor.getExternalSubject());
        grant.setRevokedAt(OffsetDateTime.now());
        grant.setRevokedByIdentityId(stored.map(IdentityEntity::getIdentityId).orElse(null));
        if (reason != null && !reason.isBlank()) {
            grant.setRevocationReason(reason);
        }
        researchAccessGrantRepository.save(grant);
        authAuditService.recordResearchGrantRevoked(actor, grantId, reason);
    }

    // ── Devices ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeviceSummary> listDevicesForIdentity(Identity actor, UUID identityId) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "DEVICE", null, "listDevicesForIdentity");
        return sessionDeviceRepository.findByIdentityId(identityId).stream()
                .map(d -> new DeviceSummary(
                        d.getDeviceId(), d.getIdentityId(), d.getDeviceFingerprintHash(),
                        d.getFirstSeenAt(), d.getLastSeenAt(), d.getTrustedUntil(),
                        d.getRevokedAt(), d.getMetadata()))
                .toList();
    }

    @Transactional
    public void revokeDevice(Identity actor, UUID deviceId) {
        authzPermissionService.requirePermission(actor, "ADMIN_USERS", "DEVICE", deviceId, "revokeDevice");
        SessionDeviceEntity device = sessionDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalStateException("Device not found: " + deviceId));
        var stored = identityRepository.findByProviderIdAndExternalSubject(
                actor.getProviderId(), actor.getExternalSubject());
        device.setRevokedAt(OffsetDateTime.now());
        device.setRevokedByIdentityId(stored.map(IdentityEntity::getIdentityId).orElse(null));
        sessionDeviceRepository.save(device);
        authAuditService.recordDeviceRevoked(actor, deviceId);
    }

    // ── Dashboard ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        long totalIdentities = identityRepository.count();
        long activeIdentities = identityRepository.findByIsActive(true, PageRequest.of(0, 1)).getTotalElements();
        long totalRoles = roleRepository.count();
        long activeDevices = sessionDeviceRepository.countByRevokedAtIsNull();
        long activeBG = breakGlassGrantRepository
                .findByRevokedAtIsNullAndExpiresAtAfter(OffsetDateTime.now()).size();
        long activeResearch = researchAccessGrantRepository
                .findByRevokedAtIsNullAndExpiresAtAfter(OffsetDateTime.now()).size();

        return new DashboardSummary(
                totalIdentities, activeIdentities, totalRoles,
                activeDevices, activeBG, activeResearch);
    }

    // ── Mappers ────────────────────────────────────────────────────

    private IdentitySummary toIdentitySummary(IdentityEntity entity) {
        List<String> roleNames = entity.getRoles() == null ? List.of()
                : entity.getRoles().stream()
                        .map(RoleEntity::getName)
                        .filter(n -> n != null && !n.isBlank())
                        .sorted()
                        .toList();
        return new IdentitySummary(
                entity.getIdentityId(), entity.getDisplayName(), entity.getDisplayShort(),
                entity.getEmail(), entity.getUsername(), entity.isActive(),
                roleNames, entity.getLastSeenAt(), entity.getCreatedAt());
    }

    private IdentityDetail toIdentityDetail(IdentityEntity entity) {
        List<RoleSummary> roles = entity.getRoles() == null ? List.of()
                : entity.getRoles().stream()
                        .map(r -> new RoleSummary(r.getRoleId(), r.getName(), r.getDescription(), r.isSystem()))
                        .sorted(Comparator.comparing(RoleSummary::name))
                        .toList();
        return new IdentityDetail(
                entity.getIdentityId(), entity.getProviderId(), entity.getExternalSubject(),
                entity.getDisplayName(), entity.getDisplayShort(),
                entity.getGivenName(), entity.getFamilyName(), entity.getMiddleName(),
                entity.getPrefix(), entity.getSuffix(),
                entity.getEmail(), entity.getUsername(), entity.isActive(),
                roles, entity.getAttributes(),
                entity.getLastSeenAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private AuditEventSummary toAuditEventSummary(AuditEventEntity entity) {
        return new AuditEventSummary(
                entity.getEventId(), entity.getOccurredAt(), entity.getEventType(),
                entity.getActorIdentityId(), entity.getActorProviderId(),
                entity.getActorExternalSubject(), entity.getTargetEntityType(),
                entity.getTargetEntityId(), entity.getTargetIdentityId(),
                entity.getOutcome(), entity.getOutcomeReason(),
                entity.getDetails(), entity.getMetadata());
    }
}
