package com.securebank.security;

import com.securebank.config.SecureBankProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates JSON Web Tokens (jjwt) for stateless authentication.
 *
 * <p>We mint two token types:</p>
 * <ul>
 *   <li><b>access</b> - short-lived (minutes), carries the username + role, sent
 *       on every API call in the {@code Authorization: Bearer} header.</li>
 *   <li><b>refresh</b> - longer-lived (days), used only at
 *       {@code POST /auth/refresh} to obtain a fresh access token without
 *       re-entering credentials.</li>
 * </ul>
 *
 * <p>Tokens are signed (HS256) with a secret from {@link SecureBankProperties}.
 * Because they are signed and self-contained, the server keeps NO session state -
 * it just verifies the signature and expiry on each request. Constructor
 * injection only, per the conventions.</p>
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessExpiryMinutes;
    private final long refreshExpiryDays;

    public JwtService(SecureBankProperties props) {
        SecureBankProperties.Jwt jwt = props.getJwt();
        // HS256 needs a >=256-bit key; the configured secret is read as UTF-8 bytes.
        this.signingKey = Keys.hmacShaKeyFor(jwt.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = jwt.getIssuer();
        this.accessExpiryMinutes = jwt.getAccessTokenExpiryMinutes();
        this.refreshExpiryDays = jwt.getRefreshTokenExpiryDays();
    }

    /** Build a signed access token for the given user. */
    public String generateAccessToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claims(Map.of(CLAIM_ROLE, role, CLAIM_TYPE, TYPE_ACCESS))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpiryMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    /** Build a signed refresh token (longer lived, type=refresh). */
    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claims(Map.of(CLAIM_TYPE, TYPE_REFRESH))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpiryDays * 24 * 3600)))
                .signWith(signingKey)
                .compact();
    }

    /** Parse + verify a token, returning its claims. Throws if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** Convenience: minutes until an access token expires (for the login response). */
    public long getAccessTokenExpirySeconds() {
        return accessExpiryMinutes * 60;
    }
}
