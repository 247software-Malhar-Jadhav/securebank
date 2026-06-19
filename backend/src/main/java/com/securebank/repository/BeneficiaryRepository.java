package com.securebank.repository;

import com.securebank.domain.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link Beneficiary} (Repository pattern).
 */
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByCustomerId(Long customerId);

    List<Beneficiary> findByCustomerUserUsername(String username);
}
