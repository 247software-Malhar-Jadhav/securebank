package com.securebank.repository;

import com.securebank.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for {@link Customer} (Repository pattern).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** Look up the customer profile owned by a given login. */
    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByUserUsername(String username);
}
