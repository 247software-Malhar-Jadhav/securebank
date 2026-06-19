package com.securebank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Per-request filter that authenticates a caller from their JWT access token.
 *
 * <p>For every request it:</p>
 * <ol>
 *   <li>Reads the {@code Authorization: Bearer <token>} header (if any).</li>
 *   <li>Verifies the signature/expiry and confirms it is an ACCESS token.</li>
 *   <li>Builds a Spring {@code Authentication} with the user's role authority and
 *       places it in the {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p>If anything is missing or invalid the filter simply does nothing - the
 * request continues unauthenticated and the security rules reject it downstream
 * with a 401/403. We extend {@link OncePerRequestFilter} so it runs exactly once
 * per request even with forwards/includes.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                // Only access tokens authenticate API calls; a refresh token
                // presented here must NOT be accepted as a credential.
                if (jwtService.isAccessToken(claims)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    var authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired token: leave the context empty so the request
                // is treated as anonymous. No exception is propagated here.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
