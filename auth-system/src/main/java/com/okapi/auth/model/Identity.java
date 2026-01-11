package com.okapi.auth.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

@Data
@Builder
public class Identity implements OidcUser, UserDetails {
    private String externalSubject;
    private String providerId; // Renamed from issuer to avoid conflict
    private String displayName;
    private String displayShort;
    private String givenName;
    private String familyName;
    private String middleName;
    private String middleInitial;
    private String nickname;
    private String prefix;
    private String suffix;
    private String email;

    private Map<String, Object> attributes;
    private Collection<? extends GrantedAuthority> authorities;
    private OidcUser oidcUser; // Delegation pattern for OidcUser methods if needed

    @Override
    public Map<String, Object> getClaims() {
        return this.attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return this.displayName;
    }

    // OidcUser specific methods
    @Override
    public org.springframework.security.oauth2.core.oidc.OidcUserInfo getUserInfo() {
        return oidcUser != null ? oidcUser.getUserInfo() : null;
    }

    @Override
    public org.springframework.security.oauth2.core.oidc.OidcIdToken getIdToken() {
        return oidcUser != null ? oidcUser.getIdToken() : null;
    }

    @Override
    public URL getIssuer() {
        return oidcUser != null ? oidcUser.getIssuer() : null;
    }
}
