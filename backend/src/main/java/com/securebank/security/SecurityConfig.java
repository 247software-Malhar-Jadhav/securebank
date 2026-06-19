package com.securebank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 configuration - stateless JWT, BCrypt, role-based rules.
 *
 * <p>Key decisions, explained for a reviewer:</p>
 * <ul>
 *   <li><b>Stateless</b> ({@code SessionCreationPolicy.STATELESS}): no HTTP
 *       session, no CSRF token machinery. Every request authenticates itself via
 *       its JWT, which suits a horizontally-scaled API behind a load balancer.</li>
 *   <li><b>BCrypt</b> password hashing - slow-by-design and salted, the standard
 *       for storing passwords.</li>
 *   <li><b>Authorization rules</b> mirror the REST surface: auth + docs + i18n
 *       are public; admin endpoints require ROLE_ADMIN; everything else needs a
 *       valid token.</li>
 *   <li>The {@link JwtAuthenticationFilter} runs before the username/password
 *       filter so a valid token populates the security context first.</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppUserDetailsService userDetailsService;

    /** BCrypt encoder - used to hash on register and verify on login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DAO authentication provider wiring our user lookup + BCrypt encoder. This
     * is what the AuthenticationManager uses to verify username/password at login.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JSON API: disable CSRF (no cookies/sessions to protect)
                // and form login.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---- Public endpoints ----
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/i18n/**").permitAll()
                        // springdoc/swagger and actuator are unauthenticated for ease of demo.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // ---- Role-restricted endpoints ----
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Tellers and admins may also act, but the baseline is any authenticated user.
                        .requestMatchers(HttpMethod.GET, "/accounts/**").authenticated()
                        // ---- Everything else requires authentication ----
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                // Run our JWT filter before the standard credential filter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
