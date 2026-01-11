package com.okapi.auth.service.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal Keycloak Admin API client used for dev/demo seeding and validation.
 *
 * This is intentionally scoped:
 * - fetch user by id (OIDC subject)
 * - fetch user groups
 */
@Service
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String adminRealm;
    private final String adminClientId;
    private final String adminUsername;
    private final String adminPassword;

    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

    public KeycloakAdminClient(
            RestClient.Builder restClientBuilder,
            @Value("${okapi.keycloak.admin.base-url:http://localhost:8180}") String baseUrl,
            @Value("${okapi.keycloak.admin.realm:master}") String adminRealm,
            @Value("${okapi.keycloak.admin.client-id:admin-cli}") String adminClientId,
            @Value("${okapi.keycloak.admin.username:admin}") String adminUsername,
            @Value("${okapi.keycloak.admin.password:}") String adminPassword
    ) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.adminRealm = adminRealm;
        this.adminClientId = adminClientId;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public KeycloakUser getUserById(String realm, String userId) {
        String token = getAccessToken();
        return restClient.get()
                .uri(baseUrl + "/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(KeycloakUser.class);
    }

    public List<KeycloakGroup> getUserGroups(String realm, String userId) {
        String token = getAccessToken();
        KeycloakGroup[] groups = restClient.get()
                .uri(baseUrl + "/admin/realms/{realm}/users/{id}/groups", realm, userId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(KeycloakGroup[].class);
        return groups == null ? List.of() : List.of(groups);
    }

    private String getAccessToken() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Missing Keycloak admin password. Set `okapi.keycloak.admin.password` (env var recommended)."
            );
        }

        CachedToken cached = tokenCache.get();
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(15))) {
            return cached.accessToken();
        }

        TokenResponse tokenResponse = restClient.post()
                .uri(baseUrl + "/realms/{realm}/protocol/openid-connect/token", adminRealm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body("grant_type=password"
                        + "&client_id=" + urlEncode(adminClientId)
                        + "&username=" + urlEncode(adminUsername)
                        + "&password=" + urlEncode(adminPassword))
                .retrieve()
                .body(TokenResponse.class);

        if (tokenResponse == null || tokenResponse.access_token == null || tokenResponse.access_token.isBlank()) {
            throw new IllegalStateException("Unable to obtain Keycloak admin access token.");
        }

        Instant expiresAt = now.plusSeconds(Math.max(30, tokenResponse.expires_in == null ? 60 : tokenResponse.expires_in));
        tokenCache.set(new CachedToken(tokenResponse.access_token, expiresAt));
        return tokenResponse.access_token;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class TokenResponse {
        public String access_token;
        public Long expires_in;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeycloakUser(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            Map<String, Object> attributes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeycloakGroup(
            String id,
            String name,
            String path
    ) {
    }
}
