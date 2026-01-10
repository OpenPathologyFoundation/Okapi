package com.okapi.auth.service;

import com.okapi.auth.model.Role;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserRoleMapper {

    /**
     * Maps IdP attributes to internal Roles.
     * Expects a 'groups' or 'memberOf' attribute.
     */
    public Set<Role> mapRoles(Map<String, Object> attributes) {
        Set<Role> roles = new HashSet<>();

        // Default mapping logic (placeholder)
        // In a real scenario, this might read from a config file or DB
        Object groupsObj = attributes.get("groups");

        if (groupsObj instanceof List<?>) {
            List<?> groups = (List<?>) groupsObj;
            for (Object group : groups) {
                String groupName = group.toString();
                if (groupName.contains("Okapi_Admins")) {
                    roles.add(Role.ADMIN);
                } else if (groupName.contains("Okapi_Pathologists")) {
                    roles.add(Role.PATHOLOGIST);
                } else if (groupName.contains("Okapi_Technicians")) {
                    roles.add(Role.TECHNICIAN);
                }
            }
        }

        // Fallback or default role if needed?
        // roles.add(Role.PATHOLOGIST); // Example default

        return roles;
    }
}
