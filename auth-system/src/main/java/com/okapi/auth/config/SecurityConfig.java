package com.okapi.auth.config;

import com.okapi.auth.service.AuthAuditService;
import com.okapi.auth.service.CustomOidcUserService;
import com.okapi.auth.model.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;
    private final AuthAuditService authAuditService;
    private final SessionTimeoutFilter sessionTimeoutFilter;

    public SecurityConfig(CustomOidcUserService customOidcUserService, AuthAuditService authAuditService,
                          SessionTimeoutFilter sessionTimeoutFilter) {
        this.customOidcUserService = customOidcUserService;
        this.authAuditService = authAuditService;
        this.sessionTimeoutFilter = sessionTimeoutFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/login", "/error", "/webjars/**").permitAll()
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

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null && authentication.getPrincipal() instanceof Identity identity) {
                authAuditService.recordLogout(identity, request);
            }
            response.sendRedirect("/login");
        };
    }
}
