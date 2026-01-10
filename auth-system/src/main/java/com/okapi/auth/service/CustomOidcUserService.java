package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRoleMapper userRoleMapper;

    public CustomOidcUserService(UserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        java.util.Set<com.okapi.auth.model.Role> roles = userRoleMapper.mapRoles(oidcUser.getAttributes());
        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>(
                oidcUser.getAuthorities());
        roles.forEach(role -> authorities
                .add(new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getAuthority())));

        return Identity.builder()
                .externalSubject(oidcUser.getSubject())
                .providerId(userRequest.getClientRegistration().getProviderDetails().getIssuerUri()) // Corrected field
                                                                                                     // name
                .displayName(oidcUser.getFullName())
                .email(oidcUser.getEmail())
                .attributes(oidcUser.getAttributes())
                .authorities(authorities)
                .oidcUser(oidcUser)
                .build();
    }
}
