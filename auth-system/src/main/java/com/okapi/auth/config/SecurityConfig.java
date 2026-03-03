package com.okapi.auth.config;

import com.okapi.auth.service.AuthAuditService;
import com.okapi.auth.service.CustomOidcUserService;
import com.okapi.auth.model.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;
    private final AuthAuditService authAuditService;
    private final SessionTimeoutFilter sessionTimeoutFilter;

    @Value("${okapi.viewer.allowed-origins:}")
    private String viewerAllowedOrigins;

    @Value("${okapi.oidc.end-session-uri}")
    private String endSessionUri;

    public SecurityConfig(CustomOidcUserService customOidcUserService, AuthAuditService authAuditService,
                          SessionTimeoutFilter sessionTimeoutFilter) {
        this.customOidcUserService = customOidcUserService;
        this.authAuditService = authAuditService;
        this.sessionTimeoutFilter = sessionTimeoutFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF: Use cookie-based CSRF token so the SPA can read the XSRF-TOKEN
        // cookie and send it back as the X-XSRF-TOKEN header on POST/PUT/DELETE.
        // CsrfTokenRequestAttributeHandler (non-deferred) is required so Spring
        // sets the XSRF-TOKEN cookie on every response, not just after the first
        // form submission.
        CookieCsrfTokenRepository csrfTokenRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null); // force eager loading of CSRF token

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepo)
                        .csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/login", "/logged-out", "/error", "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/app", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)))
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler()))
                .addFilterAfter(sessionTimeoutFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            if (authentication != null && authentication.getPrincipal() instanceof Identity identity) {
                                authAuditService.recordAccessDenied(identity, request, accessDeniedException.getMessage());
                            }
                            response.setStatus(403);
                        }));
        // .saml2Login(Customizer.withDefaults()); // Enable when metadata is ready

        return http.build();
    }

    /**
     * CORS configuration for viewer subdomain access.
     *
     * When deploying behind the nginx reverse proxy, everything is same-origin
     * and CORS is not needed. This configuration is a safety net for development
     * or deployments where the viewer is on a subdomain (e.g., viewer.okapi.local).
     *
     * Configure via: OKAPI_VIEWER_ALLOWED_ORIGINS=http://viewer.okapi.local,http://localhost:5174
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        if (viewerAllowedOrigins != null && !viewerAllowedOrigins.isBlank()) {
            List<String> origins = Arrays.asList(viewerAllowedOrigins.split(","));
            config.setAllowedOrigins(origins);
        }
        // If no origins configured, CORS will reject all cross-origin requests (secure default)

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Only apply CORS to API endpoints the viewer might call
        source.registerCorsConfiguration("/auth/token", config);
        source.registerCorsConfiguration("/api/viewer-events", config);
        source.registerCorsConfiguration("/api/cases/**", config);
        source.registerCorsConfiguration("/api/edu/**", config);
        return source;
    }

    /**
     * OIDC-aware logout: records the audit event, then redirects to Keycloak's
     * end-session endpoint so the IdP session is also terminated.  Keycloak will
     * redirect back to {@code /logged-out} (registered as a post-logout URI on
     * the client).
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            if (authentication != null && authentication.getPrincipal() instanceof Identity identity) {
                authAuditService.recordLogout(identity, request);
            }

            // Build the Keycloak end-session URL with a post_logout_redirect_uri
            // so the user lands back on our app after Keycloak clears its session.
            String postLogoutRedirect = UriComponentsBuilder
                    .fromHttpUrl(getBaseUrl(request))
                    .path("/logged-out")
                    .toUriString();

            String logoutUrl = UriComponentsBuilder
                    .fromHttpUrl(endSessionUri)
                    .queryParam("client_id", "okapi-client")
                    .queryParam("post_logout_redirect_uri", postLogoutRedirect)
                    .toUriString();

            response.sendRedirect(logoutUrl);
        };
    }

    /**
     * Reconstruct the public-facing base URL from forwarded headers (set by
     * Caddy/nginx) so that the post-logout redirect URI matches what Keycloak
     * expects (e.g. https://okapi.openpathology.tech).
     */
    private String getBaseUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        String host  = request.getHeader("X-Forwarded-Host");
        if (proto != null && host != null) {
            return proto + "://" + host;
        }
        // Fallback: use the request's own URL
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                   ? "" : ":" + request.getServerPort());
    }
}
