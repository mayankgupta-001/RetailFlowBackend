package com.creator.RetailFlow.config;

import com.creator.RetailFlow.auth.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String DEFAULT_ALLOWED_ORIGIN =
            "http://localhost:5173";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /*
     * The inline default matters: src/test/resources/application.properties
     * shadows the main properties file on the test classpath, so without
     * it the context would fail to start in tests.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /*
     * Optional wildcard origins, for hosts that mint a new subdomain per
     * deployment (e.g. Vercel preview URLs). Empty by default: exact
     * origins are the safer default.
     *
     * Example: https://retailflow-frontend-*.vercel.app
     */
    @Value("${app.cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/products/*/barcode-image").permitAll()
                        // Without this, any unhandled server error is re-dispatched
                        // to /error, denied as anonymous, and shows up as a
                        // misleading 403 instead of the real status.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    /*
     * Comma-separated env values are easy to get subtly wrong, so blank
     * entries are dropped and trailing slashes stripped - the browser
     * never sends an origin with a trailing slash, and an exact match
     * failure there is invisible except as a CORS error.
     */
    private static List<String> parseOrigins(String value) {

        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(origin -> origin.trim().replaceAll("/+$", ""))
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = parseOrigins(allowedOrigins);

        configuration.setAllowedOrigins(
                origins.isEmpty() ? List.of(DEFAULT_ALLOWED_ORIGIN) : origins
        );

        /*
         * Patterns stack on top of the exact origins above: both lists
         * are consulted, so production can stay exact while preview
         * deployments are matched by wildcard.
         */
        List<String> patterns = parseOrigins(allowedOriginPatterns);

        if (!patterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(patterns);
        }

        // PATCH is load-bearing: /api/discounts/{id}/status uses it.
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(List.of("*"));

        // Cache the preflight result instead of one OPTIONS round-trip
        // per request that carries an Authorization header.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
