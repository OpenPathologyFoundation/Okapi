package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    public record MeResponse(
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
            List<String> authorities
    ) {
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

            return new MeResponse(
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
                    authorities);
        }

        // Fallback for debugging, if it's not our Identity object yet
        return Map.of("principal", principal);
    }
}
