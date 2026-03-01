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
    private final AuthAuditService authAuditService;
    private final String providerIdFallback;

    public CustomOidcUserService(UserRoleMapper userRoleMapper,
            com.okapi.auth.repository.IdentityRepository identityRepository,
            com.okapi.auth.repository.RoleRepository roleRepository,
            AuthAuditService authAuditService,
            @Value("${okapi.oidc.provider-id:local-oidc}") String providerIdFallback) {
        this.userRoleMapper = userRoleMapper;
        this.identityRepository = identityRepository;
        this.roleRepository = roleRepository;
        this.authAuditService = authAuditService;
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
        String prefix = oidcUser.getAttribute("prefix");
        String suffix = oidcUser.getAttribute("suffix");
        String displayShort = oidcUser.getAttribute("display_short");
        String username = oidcUser.getAttribute("preferred_username");

        if (givenName != null && !givenName.isBlank()) {
            entity.setGivenName(givenName);
        }
        if (familyName != null && !familyName.isBlank()) {
            entity.setFamilyName(familyName);
        }
        if (middleName != null && !middleName.isBlank()) {
            entity.setMiddleName(middleName);
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
        if (username != null && !username.isBlank()) {
            entity.setUsername(username);
        } else if (email != null && !email.isBlank()) {
            entity.setUsername(email);
        }

        entity.setProviderId(providerId);
        entity.setLastSeenAt(java.time.OffsetDateTime.now());
        java.util.Map<String, Object> mergedAttributes = new java.util.HashMap<>();
        if (entity.getAttributes() != null) {
            mergedAttributes.putAll(entity.getAttributes());
        }
        mergedAttributes.putAll(sanitizeAttributes(oidcUser.getAttributes()));
        entity.setAttributes(mergedAttributes);

        // Update Roles in DB
        // Fetch RoleEntities matching our calculated Roles
        java.util.Set<com.okapi.auth.model.db.RoleEntity> existingRoles = new java.util.HashSet<>();
        if (entity.getRoles() != null) {
            existingRoles.addAll(entity.getRoles());
        }
        java.util.Set<com.okapi.auth.model.db.RoleEntity> dbRoles = new java.util.HashSet<>();
        for (com.okapi.auth.model.Role roleEnum : calculatedRoles) {
            roleRepository.findByName(roleEnum.name()).ifPresent(dbRoles::add);
        }
        entity.setRoles(dbRoles);

        Identity auditIdentity = Identity.builder()
                .externalSubject(oidcUser.getSubject())
                .providerId(providerId)
                .displayName(entity.getDisplayName())
                .email(oidcUser.getEmail())
                .attributes(oidcUser.getAttributes())
                .build();

        java.util.Set<String> existingRoleNames = existingRoles.stream()
                .map(com.okapi.auth.model.db.RoleEntity::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> newRoleNames = dbRoles.stream()
                .map(com.okapi.auth.model.db.RoleEntity::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        if (!existingRoleNames.equals(newRoleNames)) {
            for (com.okapi.auth.model.db.RoleEntity role : dbRoles) {
                if (role != null && role.getName() != null && !existingRoleNames.contains(role.getName())) {
                    authAuditService.recordRoleAssigned(auditIdentity, role.getRoleId(), role.getName(), "IDP_GROUP");
                }
            }
            for (com.okapi.auth.model.db.RoleEntity role : existingRoles) {
                if (role != null && role.getName() != null && !newRoleNames.contains(role.getName())) {
                    authAuditService.recordRoleRevoked(auditIdentity, role.getRoleId(), role.getName(), "IDP_GROUP");
                }
            }
        }

        identityRepository.save(entity);
        log.info("Persisted identity for user: {}", email);
        authAuditService.recordLoginSuccess(auditIdentity);

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
                .middleInitial(oidcUser.getAttribute("middle_initial"))
                .nickname(oidcUser.getAttribute("nickname"))
                .prefix(entity.getPrefix())
                .suffix(entity.getSuffix())
                .email(oidcUser.getEmail())
                .attributes(oidcUser.getAttributes())
                .authorities(authorities)
                .oidcUser(oidcUser)
                .build();
    }

    /**
     * Converts OIDC attribute values to JSON-safe types.
     * OIDC claims may contain Instant, URL, or other types that Hibernate's
     * internal Jackson ObjectMapper cannot serialize to JSONB.
     */
    private java.util.Map<String, Object> sanitizeAttributes(java.util.Map<String, Object> attributes) {
        if (attributes == null) return java.util.Collections.emptyMap();
        java.util.Map<String, Object> safe = new java.util.HashMap<>();
        for (var entry : attributes.entrySet()) {
            safe.put(entry.getKey(), toJsonSafe(entry.getValue()));
        }
        return safe;
    }

    private Object toJsonSafe(Object value) {
        if (value == null) return null;
        if (value instanceof String || value instanceof Boolean || value instanceof Number) return value;
        if (value instanceof java.time.Instant instant) return instant.toString();
        if (value instanceof java.net.URL url) return url.toString();
        if (value instanceof java.net.URI uri) return uri.toString();
        if (value instanceof java.util.Collection<?> collection) {
            return collection.stream().map(this::toJsonSafe).toList();
        }
        if (value instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> safeMap = new java.util.HashMap<>();
            for (var entry : map.entrySet()) {
                safeMap.put(String.valueOf(entry.getKey()), toJsonSafe(entry.getValue()));
            }
            return safeMap;
        }
        return value.toString();
    }
}
