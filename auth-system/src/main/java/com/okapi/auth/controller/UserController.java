package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.service.AuthAuditService;
import com.okapi.auth.service.AuthzPermissionService;
import com.okapi.auth.service.AuthzTokenService;
import com.okapi.auth.service.SessionTimeoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class UserController {

    public record MeResponse(
            UUID identityId,
            String providerId,
            String externalSubject,
            String email,
            String displayName,
            String displayShort,
            String givenName,
            String familyName,
            String middleName,
            String middleInitial,
            String prefix,
            String suffix,
            List<String> roles,
            List<String> permissions,
            boolean isAdmin,
            List<String> authorities
    ) {
    }

    private final AuthzTokenService authzTokenService;
    private final IdentityRepository identityRepository;
    private final AuthzPermissionService authzPermissionService;
    private final SessionTimeoutService sessionTimeoutService;
    private final AuthAuditService authAuditService;

    @Value("${okapi.oidc.end-session-uri:}")
    private String endSessionUri;

    public UserController(
            AuthzTokenService authzTokenService,
            IdentityRepository identityRepository,
            AuthzPermissionService authzPermissionService,
            SessionTimeoutService sessionTimeoutService,
            AuthAuditService authAuditService) {
        this.authzTokenService = authzTokenService;
        this.identityRepository = identityRepository;
        this.authzPermissionService = authzPermissionService;
        this.sessionTimeoutService = sessionTimeoutService;
        this.authAuditService = authAuditService;
    }

    @GetMapping("/auth/me")
    public Object getCurrentUserAuthMe(@AuthenticationPrincipal Object principal) {
        return toMeResponse(principal);
    }

    /**
     * Post-login landing endpoint.
     *
     * Spring Security OAuth2 login defaults to redirecting to `/` after a successful login
     * when no saved request exists. Provide a stable mapping to avoid a 404 Whitelabel page.
     */
    @GetMapping("/")
    public org.springframework.web.servlet.view.RedirectView root() {
        return new org.springframework.web.servlet.view.RedirectView("/auth/me");
    }

    @GetMapping("/me")
    public Object getCurrentUser(@AuthenticationPrincipal Object principal) {
        return toMeResponse(principal);
    }

    @GetMapping("/auth/session-info")
    public Map<String, Integer> getSessionInfo() {
        return Map.of("idleTimeoutMinutes", sessionTimeoutService.getIdleTimeoutMinutes());
    }

    @GetMapping("/auth/logout")
    public void logout(@AuthenticationPrincipal Object principal,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        String idTokenValue = null;

        // Record audit and extract id_token before destroying the session
        if (principal instanceof Identity identity) {
            authAuditService.recordLogout(identity, request);
            if (identity.getIdToken() != null) {
                idTokenValue = identity.getIdToken().getTokenValue();
            }
        }

        // Invalidate Spring session and clear security context
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        // Redirect to Keycloak's end_session_endpoint to terminate the IdP session
        if (endSessionUri != null && !endSessionUri.isBlank()) {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort();

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionUri)
                    .queryParam("client_id", "okapi-client")
                    .queryParam("post_logout_redirect_uri", baseUrl + "/logged-out");

            if (idTokenValue != null) {
                builder.queryParam("id_token_hint", idTokenValue);
            }

            response.sendRedirect(builder.toUriString());
        } else {
            response.sendRedirect("/login");
        }
    }

    @PostMapping("/auth/token")
    public Object mintToken(@AuthenticationPrincipal Object principal) {
        if (principal instanceof Identity identity) {
            return authzTokenService.mintToken(identity);
        }
        return Map.of("error", "unauthenticated");
    }

    private Object toMeResponse(Object principal) {
        if (principal instanceof Identity identity) {
            List<String> authorities = identity.getAuthorities() == null
                    ? List.of()
                    : identity.getAuthorities().stream()
                            .map(a -> a == null ? null : a.getAuthority())
                            .filter(a -> a != null && !a.isBlank())
                            .distinct()
                            .sorted()
                            .toList();

            UUID identityId = null;
            List<String> roles = List.of();
            List<String> permissions = List.of();
            boolean isAdmin = false;

            var stored = identityRepository.findByProviderIdAndExternalSubject(
                    identity.getProviderId(), identity.getExternalSubject());
            if (stored.isPresent()) {
                IdentityEntity entity = stored.get();
                identityId = entity.getIdentityId();
                roles = authzPermissionService.resolveRoleNames(identityId);
                permissions = authzPermissionService.resolvePermissionsForRoles(roles);
                isAdmin = roles.contains("ADMIN");
            }

            return new MeResponse(
                    identityId,
                    identity.getProviderId(),
                    identity.getExternalSubject(),
                    identity.getEmail(),
                    identity.getDisplayName(),
                    identity.getDisplayShort(),
                    identity.getGivenName(),
                    identity.getFamilyName(),
                    identity.getMiddleName(),
                    identity.getMiddleInitial(),
                    identity.getPrefix(),
                    identity.getSuffix(),
                    roles,
                    permissions,
                    isAdmin,
                    authorities);
        }

        // Fallback for debugging, if it's not our Identity object yet
        return Map.of("principal", principal);
    }
}
