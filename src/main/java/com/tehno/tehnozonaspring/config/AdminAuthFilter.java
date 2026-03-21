package com.tehno.tehnozonaspring.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    private final String adminApiKey;

    public AdminAuthFilter(@Value("${admin.api.key:}") String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            // Generate a random key at startup if not configured - print to log so operator can use it
            this.adminApiKey = java.util.UUID.randomUUID().toString().replace("-", "");
            System.out.println("⚠️  ADMIN_API_KEY not configured. Generated ephemeral key: " + this.adminApiKey);
            System.out.println("   Set ADMIN_API_KEY env variable (or admin.api.key in properties) to make it permanent.");
        } else {
            this.adminApiKey = configuredKey;
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader("X-Admin-Key");
        if (key == null || !key.equals(adminApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
