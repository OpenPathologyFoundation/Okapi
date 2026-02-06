package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.IdentityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthzTokenService {

    private final IdentityRepository identityRepository;
    private final AuthzPermissionService authzPermissionService;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String authzVersion;
    private final long ttlSeconds;

    public AuthzTokenService(
            IdentityRepository identityRepository,
            AuthzPermissionService authzPermissionService,
            JwtEncoder jwtEncoder,
            @Value("${okapi.jwt.issuer:okapi}") String issuer,
            @Value("${okapi.authz.version:2026.01.26+dev}") String authzVersion,
            @Value("${okapi.jwt.ttl-seconds:600}") long ttlSeconds) {
        this.identityRepository = identityRepository;
        this.authzPermissionService = authzPermissionService;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.authzVersion = authzVersion;
        this.ttlSeconds = ttlSeconds;
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String okapiAuthzVersion) {
    }

    public TokenResponse mintToken(Identity identity) {
        IdentityEntity entity = identityRepository
                .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                .orElseThrow(() -> new IllegalStateException("Identity not found for token issuance"));

        List<String> roleNames = authzPermissionService.resolveRoleNames(entity.getIdentityId());
        List<String> permissions = authzPermissionService.resolvePermissionsForRoles(roleNames);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(identity.getExternalSubject())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", roleNames)
                .claim("permissions", permissions)
                .claim("okapi_authz_version", authzVersion)
                .build();

        Jwt jwt = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claims));

        return new TokenResponse(jwt.getTokenValue(), "Bearer", ttlSeconds, authzVersion);
    }
}
