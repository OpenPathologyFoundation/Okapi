package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import org.springframework.beans.factory.annotation.Value;
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
    private final String providerIdFallback;

    public CustomOidcUserService(UserRoleMapper userRoleMapper,
            com.okapi.auth.repository.IdentityRepository identityRepository,
            com.okapi.auth.repository.RoleRepository roleRepository,
            @Value("${okapi.oidc.provider-id:local-oidc}") String providerIdFallback) {
        this.userRoleMapper = userRoleMapper;
        this.identityRepository = identityRepository;
        this.roleRepository = roleRepository;
        this.providerIdFallback = providerIdFallback;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String providerId = userRequest.getClientRegistration().getProviderDetails().getIssuerUri();
        if (providerId == null || providerId.isBlank()) {
            providerId = providerIdFallback;
        }

        // 1. Calculate Roles based on IdP Groups (DB Lookup via Mapper)
        java.util.Set<com.okapi.auth.model.Role> calculatedRoles = userRoleMapper.mapRoles(providerId,
                oidcUser.getAttributes());

        // 2. Persist/Update User Identity in DB
        String email = oidcUser.getEmail();
        String subject = oidcUser.getSubject();

        com.okapi.auth.model.db.IdentityEntity entity = identityRepository
                .findByProviderIdAndExternalSubject(providerId, subject)
                .orElseGet(() -> {
                    // Backward-compatibility for pre-migration data keyed by email
                    if (email != null && !email.isBlank()) {
                        return identityRepository.findByEmail(email)
                                .orElseGet(() -> com.okapi.auth.model.db.IdentityEntity.builder().build());
                    }
                    return com.okapi.auth.model.db.IdentityEntity.builder().build();
                });

        if (entity.getProviderId() == null) {
            entity.setProviderId(providerId);
        }
        if (entity.getExternalSubject() == null) {
            entity.setExternalSubject(subject);
        }
        if (entity.getEmail() == null && email != null && !email.isBlank()) {
            entity.setEmail(email);
        }

        // Update basic fields
        entity.setDisplayName(oidcUser.getFullName());

        // Optional structured name fields (if IdP provides them)
        String givenName = oidcUser.getAttribute("given_name");
        String familyName = oidcUser.getAttribute("family_name");
        String middleName = oidcUser.getAttribute("middle_name");
        String middleInitial = oidcUser.getAttribute("middle_initial");
        String nickname = oidcUser.getAttribute("nickname");
        String prefix = oidcUser.getAttribute("prefix");
        String suffix = oidcUser.getAttribute("suffix");
        String displayShort = oidcUser.getAttribute("display_short");

        if (givenName != null && !givenName.isBlank()) {
            entity.setGivenName(givenName);
        }
        if (familyName != null && !familyName.isBlank()) {
            entity.setFamilyName(familyName);
        }
        if (middleName != null && !middleName.isBlank()) {
            entity.setMiddleName(middleName);
        }
        if (middleInitial != null && !middleInitial.isBlank()) {
            entity.setMiddleInitial(middleInitial);
        }
        if (nickname != null && !nickname.isBlank()) {
            entity.setNickname(nickname);
        }
        if (prefix != null && !prefix.isBlank()) {
            entity.setPrefix(prefix);
        }
        if (suffix != null && !suffix.isBlank()) {
            entity.setSuffix(suffix);
        }
        if (displayShort != null && !displayShort.isBlank()) {
            entity.setDisplayShort(displayShort);
        }

        entity.setProviderId(providerId);
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
                .providerId(providerId)
                .displayName(entity.getDisplayName())
                .displayShort(entity.getDisplayShort())
                .givenName(entity.getGivenName())
                .familyName(entity.getFamilyName())
                .middleName(entity.getMiddleName())
                .middleInitial(entity.getMiddleInitial())
                .nickname(entity.getNickname())
                .prefix(entity.getPrefix())
                .suffix(entity.getSuffix())
                .email(oidcUser.getEmail())
                .attributes(oidcUser.getAttributes())
                .authorities(authorities)
                .oidcUser(oidcUser)
                .build();
    }
}
