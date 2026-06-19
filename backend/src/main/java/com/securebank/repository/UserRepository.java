package com.securebank.repository;

import com.securebank.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for {@link User} (Repository pattern).
 *
 * <p>Spring generates the implementation at runtime from these method
 * signatures - we never write the SQL or the boilerplate DAO. Derived query
 * methods like {@code findByUsername} are translated to JPQL automatically.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
