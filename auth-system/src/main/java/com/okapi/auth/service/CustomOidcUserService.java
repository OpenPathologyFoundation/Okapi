package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@lombok.extern.slf4j.Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRoleMapper userRoleMapper;
    private final com.okapi.auth.repository.IdentityRepository identityRepository;
    private final com.okapi.auth.repository.RoleRepository roleRepository;

    public CustomOidcUserService(UserRoleMapper userRoleMapper,
            com.okapi.auth.repository.IdentityRepository identityRepository,
            com.okapi.auth.repository.RoleRepository roleRepository) {
        this.userRoleMapper = userRoleMapper;
        this.identityRepository = identityRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // 1. Calculate Roles based on IdP Groups (DB Lookup via Mapper)
        java.util.Set<com.okapi.auth.model.Role> calculatedRoles = userRoleMapper.mapRoles(oidcUser.getAttributes());

        // 2. Persist/Update User Identity in DB
        String email = oidcUser.getEmail();
        String subject = oidcUser.getSubject();

        com.okapi.auth.model.db.IdentityEntity entity = identityRepository.findByEmail(email)
                .orElseGet(() -> com.okapi.auth.model.db.IdentityEntity.builder()
                        .email(email)
                        .externalSubject(subject)
                        .build());

        // Update basic fields
        entity.setDisplayName(oidcUser.getFullName());
        entity.setProviderId(userRequest.getClientRegistration().getProviderDetails().getIssuerUri());
        entity.setLastLoginAt(java.time.LocalDateTime.now());

        // Update Roles in DB
        // Fetch RoleEntities matching our calculated Roles
        java.util.Set<com.okapi.auth.model.db.RoleEntity> dbRoles = new java.util.HashSet<>();
        for (com.okapi.auth.model.Role roleEnum : calculatedRoles) {
            roleRepository.findByName(roleEnum.name()).ifPresent(dbRoles::add);
        }
        entity.setRoles(dbRoles);

        identityRepository.save(entity);
        log.info("Persisted identity for user: {}", email);

        // 3. Construct Internal UserDetails (Session Object)
        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>(
                oidcUser.getAuthorities());
        calculatedRoles.forEach(role -> authorities
                .add(new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getAuthority())));

        return Identity.builder()
                .externalSubject(oidcUser.getSubject())
                .providerId(userRequest.getClientRegistration().getProviderDetails().getIssuerUri())
                .displayName(oidcUser.getFullName())
                .email(oidcUser.getEmail())
                .attributes(oidcUser.getAttributes())
                .authorities(authorities)
                .oidcUser(oidcUser)
                .build();
    }
}
