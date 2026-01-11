package com.okapi.auth.service;

import com.okapi.auth.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;

@lombok.extern.slf4j.Slf4j
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserRoleMapperTest {

    @org.mockito.Mock
    private com.okapi.auth.repository.IdpGroupMappingRepository mappingRepository;

    @org.mockito.Mock
    private com.okapi.auth.repository.RoleRepository roleRepository;

    @org.mockito.InjectMocks
    private UserRoleMapper userRoleMapper;

    @Test
    void mapRoles_ShouldMapAdminGroup_ToAdminRole() {
        log.info("Testing mapRoles with Admin group...");
        Map<String, Object> attributes = Map.of("groups", List.of("Okapi_Admins"));

        org.mockito.Mockito.when(mappingRepository.findRoleNamesByProviderIdAndGroupNames(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of("ADMIN"));

        Set<Role> roles = userRoleMapper.mapRoles("http://localhost:8180/realms/okapi", attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.contains(Role.ADMIN));
    }

    @Test
    void mapRoles_ShouldMapPathologistGroup_ToPathologistRole() {
        log.info("Testing mapRoles with Pathologist group...");
        Map<String, Object> attributes = Map.of("groups", List.of("Okapi_Pathologists"));

        org.mockito.Mockito.when(mappingRepository.findRoleNamesByProviderIdAndGroupNames(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of("PATHOLOGIST"));

        Set<Role> roles = userRoleMapper.mapRoles("http://localhost:8180/realms/okapi", attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.contains(Role.PATHOLOGIST));
    }

    @Test
    void mapRoles_ShouldReturnEmpty_WhenNoMatchingGroups() {
        log.info("Testing mapRoles with unknown groups...");
        Map<String, Object> attributes = Map.of("groups", List.of("UnknownUserGroup"));

        org.mockito.Mockito.when(mappingRepository.findRoleNamesByProviderIdAndGroupNames(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Collections.emptyList());

        Set<Role> roles = userRoleMapper.mapRoles("http://localhost:8180/realms/okapi", attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void mapRoles_ShouldHandleMissingGroupsAttribute() {
        log.info("Testing mapRoles with empty attributes...");
        Map<String, Object> attributes = Collections.emptyMap();

        Set<Role> roles = userRoleMapper.mapRoles("http://localhost:8180/realms/okapi", attributes);

        log.debug("Mapped roles: {}", roles);
        assertTrue(roles.isEmpty());
    }
}
