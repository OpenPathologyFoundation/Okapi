package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthzPermissionService {

    private final IdentityRepository identityRepository;
    private final PermissionRepository permissionRepository;
    private final AuthAuditService authAuditService;

    public AuthzPermissionService(
            IdentityRepository identityRepository,
            PermissionRepository permissionRepository,
            AuthAuditService authAuditService) {
        this.identityRepository = identityRepository;
        this.permissionRepository = permissionRepository;
        this.authAuditService = authAuditService;
    }

    public List<String> resolveRoleNames(UUID identityId) {
        IdentityEntity entity = identityRepository.findById(identityId)
                .orElseThrow(() -> new IllegalStateException("Identity not found"));
        return entity.getRoles() == null
                ? List.of()
                : entity.getRoles().stream()
                        .map(role -> role == null ? null : role.getName())
                        .filter(name -> name != null && !name.isBlank())
                        .distinct()
                        .sorted()
                        .toList();
    }

    public List<String> resolvePermissionsForRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findPermissionNamesByRoleNames(roleNames).stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public boolean hasPermission(Identity identity, String permission) {
        UUID identityId = identityRepository
                .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                .map(IdentityEntity::getIdentityId)
                .orElseThrow(() -> new IllegalStateException("Identity not found"));
        List<String> roles = resolveRoleNames(identityId);
        List<String> permissions = resolvePermissionsForRoles(roles);
        return permissions.contains(permission);
    }

    public void requirePermission(
            Identity identity,
            String permission,
            String targetEntityType,
            UUID targetEntityId,
            String detail) {
        if (hasPermission(identity, permission)) {
            return;
        }
        authAuditService.recordPermissionDenied(identity, permission, targetEntityType, targetEntityId, detail);
        throw new org.springframework.security.access.AccessDeniedException("Missing permission: " + permission);
    }
}
