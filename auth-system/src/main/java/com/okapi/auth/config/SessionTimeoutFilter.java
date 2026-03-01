package com.okapi.auth.config;

import com.okapi.auth.service.SessionTimeoutService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SessionTimeoutFilter extends OncePerRequestFilter {

    private final SessionTimeoutService sessionTimeoutService;

    public SessionTimeoutFilter(SessionTimeoutService sessionTimeoutService) {
        this.sessionTimeoutService = sessionTimeoutService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                int timeoutMinutes = sessionTimeoutService.getIdleTimeoutMinutes();
                session.setMaxInactiveInterval(timeoutMinutes * 60);
            }
        }

        filterChain.doFilter(request, response);
    }
}
