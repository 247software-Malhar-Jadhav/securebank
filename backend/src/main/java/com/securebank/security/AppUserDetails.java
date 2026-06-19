package com.securebank.security;

import com.securebank.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Adapter that exposes our {@link User} entity to Spring Security as a
 * {@link UserDetails} (Adapter pattern).
 *
 * <p>Spring Security speaks {@code UserDetails}; our domain speaks {@code User}.
 * Rather than leak the entity into the security layer, this thin wrapper maps the
 * fields Spring needs: the password hash, the single role rendered as a
 * {@code ROLE_*} authority, and the locked/enabled flags.</p>
 */
@Getter
public class AppUserDetails implements UserDetails {

    private final transient User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring's hasRole("ADMIN") checks for the authority "ROLE_ADMIN", so we
        // prefix the role name here.
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account is "non-locked" unless a lockedUntil instant is set and still in
     * the future. Spring rejects logins for locked accounts automatically.
     */
    @Override
    public boolean isAccountNonLocked() {
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
