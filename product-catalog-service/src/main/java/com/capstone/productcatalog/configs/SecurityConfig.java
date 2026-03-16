package com.capstone.productcatalog.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    /*
     * Reads the X-User-Name and X-User-Role headers injected by the API Gateway's
     * JwtAuthFilter and populates the Spring SecurityContext.
     * This allows @PreAuthorize annotations to work without re-validating the JWT.
     */

    @Bean
    public OncePerRequestFilter headerAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NotNull HttpServletRequest request,
                                            @NotNull HttpServletResponse response,
                                            @NotNull FilterChain chain)
                throws ServletException, IOException {

                String username = request.getHeader("X-User-Name");
                String role = request.getHeader("X-User-Role");

                if(username != null && role != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var auth =  new UsernamePasswordAuthenticationToken(
                            username, null,
                            List.of(new SimpleGrantedAuthority(role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(headerAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
