package com.fooddelivery.config;

import com.fooddelivery.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Customer Service.
 * 
 * Step 1: Basic Spring Security setup
 * - All requests pass through Spring Security filter chain
 * - Only health endpoints (/actuator/**) are allowed without authentication
 * - All other requests require authentication (will be blocked until JWT is added in Step 2)
 * - Stateless session (no session cookies - ready for JWT tokens)
 * - CSRF disabled (REST API uses JWT tokens, not session cookies)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize annotations
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * BCrypt strength configuration (4-31, default 10).
     * Higher strength = more secure but slower.
     * Recommended: 10-12 for production.
     */
    @Value("${security.bcrypt.strength:10}")
    private int bcryptStrength;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * PasswordEncoder bean for hashing passwords.
     * Uses BCrypt with configurable strength.
     * This bean is used by CustomerService for password hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API (we use JWT tokens instead)
            .csrf(csrf -> csrf.disable())
            
            // Stateless session - no session cookies (we use JWT tokens)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add JWT authentication filter before Spring Security's default authentication filter
            // JWT filter validates tokens and sets authentication in SecurityContext
            // UsernamePasswordAuthenticationFilter runs after but does nothing (authentication already set)
            // Note: UsernamePasswordAuthenticationFilter cannot be removed (part of Spring Security chain),
            //       but it's effectively bypassed since JWT filter sets authentication first
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure request authorization
            .authorizeHttpRequests(auth -> auth
                // Allow actuator endpoints (health checks) - no authentication required
                .requestMatchers("/actuator/**").permitAll()
                
                // Public endpoint: Create customer (registration) - no authentication required
                .requestMatchers(HttpMethod.POST, "/customers").permitAll()
                
                // Admin-only endpoints
                .requestMatchers(HttpMethod.GET, "/customers").hasRole("ADMIN") // Get all customers
                .requestMatchers(HttpMethod.DELETE, "/customers/**").hasRole("ADMIN") // Delete customer
                .requestMatchers(HttpMethod.PUT, "/customers/**/role").hasRole("ADMIN") // Update role
                
                // Customer and Admin endpoints (method-level security will check ownership)
                // GET /customers/{id} - allowed to ADMIN and CUSTOMER (method checks ownership)
                // GET /customers/email/{email} - allowed to ADMIN and CUSTOMER (method checks ownership)
                // PUT /customers/{id} - allowed to ADMIN and CUSTOMER (method checks ownership)
                .requestMatchers(HttpMethod.GET, "/customers/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/customers/**").hasAnyRole("ADMIN", "CUSTOMER")
                
                // All other requests require authentication (valid JWT token)
                .anyRequest().authenticated()
            );

        return http.build();
    }
}

