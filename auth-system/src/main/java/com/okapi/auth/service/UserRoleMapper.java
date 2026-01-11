package com.okapi.auth.service;

import com.okapi.auth.model.Role;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@lombok.RequiredArgsConstructor
public class UserRoleMapper {

    private final com.okapi.auth.repository.IdpGroupMappingRepository mappingRepository;
    private final com.okapi.auth.repository.RoleRepository roleRepository;

    /**
     * Maps IdP attributes to internal Roles using Database mappings.
     * Expects a 'groups' (Keycloak) or similar attribute.
     */
    public Set<Role> mapRoles(String providerId, Map<String, Object> attributes) {
        Set<Role> roles = new HashSet<>();

        // Get groups from IdP attributes
        // Keycloak uses "groups"
        Object groupsObj = attributes.get("groups");
        List<String> groupNames = new java.util.ArrayList<>();

        if (groupsObj instanceof List<?>) {
            for (Object g : (List<?>) groupsObj) {
                if (g != null)
                    groupNames.add(g.toString());
            }
        }

        // DB Lookup
        if (providerId != null && !providerId.isBlank() && !groupNames.isEmpty()) {
            List<String> roleNames = mappingRepository.findRoleNamesByProviderIdAndGroupNames(providerId, groupNames);
            for (String roleName : roleNames) {
                try {
                    roles.add(Role.valueOf(roleName));
                } catch (IllegalArgumentException e) {
                    // Role in DB doesn't match Enum
                }
            }
        }

        return roles;
    }
}
