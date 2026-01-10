package com.okapi.auth.service;

import com.okapi.auth.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class UserRoleMapperTest {

    private final UserRoleMapper userRoleMapper = new UserRoleMapper();

    @Test
    void mapRoles_ShouldMapAdminGroup_ToAdminRole() {
        log.info("Testing mapRoles with Admin group...");
        Map<String, Object> attributes = Map.of("groups", List.of("Okapi_Admins"));

        Set<Role> roles = userRoleMapper.mapRoles(attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.contains(Role.ADMIN));
    }

    @Test
    void mapRoles_ShouldMapPathologistGroup_ToPathologistRole() {
        log.info("Testing mapRoles with Pathologist group...");
        Map<String, Object> attributes = Map.of("groups", List.of("Okapi_Pathologists", "SomeOtherGroup"));

        Set<Role> roles = userRoleMapper.mapRoles(attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.contains(Role.PATHOLOGIST));
        assertFalse(roles.contains(Role.ADMIN));
    }

    @Test
    void mapRoles_ShouldReturnEmpty_WhenNoMatchingGroups() {
        log.info("Testing mapRoles with unknown groups...");
        Map<String, Object> attributes = Map.of("groups", List.of("UnknownUserGroup"));

        Set<Role> roles = userRoleMapper.mapRoles(attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void mapRoles_ShouldHandleMissingGroupsAttribute() {
        log.info("Testing mapRoles with empty attributes...");
        Map<String, Object> attributes = Collections.emptyMap();

        Set<Role> roles = userRoleMapper.mapRoles(attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.isEmpty());
    }
}
