package com.securebank.service;

import com.securebank.config.SecureBankProperties;
import com.securebank.domain.Customer;
import com.securebank.domain.User;
import com.securebank.domain.enums.KycStatus;
import com.securebank.domain.enums.Role;
import com.securebank.dto.AuthDtos.*;
import com.securebank.exception.DuplicateResourceException;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.repository.CustomerRepository;
import com.securebank.repository.UserRepository;
import com.securebank.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Authentication use cases: register, login (with account lockout), and refresh.
 *
 * <p>Lockout policy: after N consecutive failed logins (configured) we set
 * {@code locked_until} to now + lockoutMinutes. While locked, login is refused
 * even with the correct password. A successful login resets the failure counter.
 * This throttles brute-force attacks. We verify the BCrypt hash ourselves here
 * (rather than via the AuthenticationManager) so we can implement the
 * count/lock bookkeeping in the same transaction.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureBankProperties properties;

    /** Register a new CUSTOMER login + profile and return tokens. */
    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new DuplicateResourceException("error.auth.usernameTaken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("error.auth.emailTaken");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .failedAttempts(0)
                .preferredLocale(req.preferredLocale() == null ? "en" : req.preferredLocale())
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .kycStatus(KycStatus.PENDING)
                .build();
        customerRepository.save(customer);

        log.info("Registered new customer username={}", user.getUsername());
        return issueTokens(user);
    }

    /** Verify credentials, apply lockout bookkeeping, and return tokens. */
    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Reject early if currently locked.
        if (isLocked(user)) {
            throw new com.securebank.exception.ApiException(
                    org.springframework.http.HttpStatus.LOCKED, "error.auth.accountLocked") {};
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Disabled account");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Success: clear any failure state.
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return issueTokens(user);
    }

    /** Exchange a valid refresh token for a fresh access (and refresh) token. */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest req) {
        final Claims claims;
        try {
            claims = jwtService.parse(req.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Not a refresh token");
        }
        User user = userRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("error.auth.badCredentials"));
        return issueTokens(user);
    }

    // ---- helpers -----------------------------------------------------------

    private boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    /** Increment the failure counter; lock the account once the threshold is hit. */
    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        int max = properties.getSecurity().getMaxFailedAttempts();
        if (attempts >= max) {
            user.setLockedUntil(Instant.now().plus(
                    properties.getSecurity().getLockoutMinutes(), ChronoUnit.MINUTES));
            log.warn("Account locked due to {} failed attempts: username={}", attempts, user.getUsername());
        }
        userRepository.save(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getUsername());
        return new TokenResponse(access, refresh, "Bearer",
                jwtService.getAccessTokenExpirySeconds(), user.getUsername(), user.getRole().name());
    }
}
