package com.fooddelivery.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Filter.
 * 
 * This filter intercepts all requests and:
 * 1. Extracts JWT token from Authorization header
 * 2. Validates token (signature, expiration, issuer)
 * 3. Extracts user information (userId, email, role) from token
 * 4. Sets Spring Security authentication context
 * 
 * The filter runs before Spring Security's authentication mechanism,
 * allowing authenticated requests to proceed to controllers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                // Parse and validate token
                Claims claims = jwtTokenProvider.parseToken(token);

                // Extract user information
                String userId = jwtTokenProvider.getUserId(claims);
                String email = jwtTokenProvider.getEmail(claims);
                String role = jwtTokenProvider.getRole(claims);

                log.debug("JWT token validated for user: {} (role: {})", userId, role);

                // Create authentication object
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, // principal (user identifier)
                                null,   // credentials (not needed for JWT)
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in Spring Security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication set for user: {} with role: {}", userId, role);
            } else {
                log.debug("No JWT token found in request");
            }

        } catch (JwtException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            // Clear security context if token is invalid
            SecurityContextHolder.clearContext();
            // Return 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
            return;
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Bearer <token>"
     * 
     * @param request HTTP request
     * @return JWT token string, or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}

