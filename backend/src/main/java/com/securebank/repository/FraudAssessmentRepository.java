package com.securebank.repository;

import com.securebank.domain.FraudAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link FraudAssessment} (Repository pattern).
 */
public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, Long> {

    List<FraudAssessment> findByTransactionId(Long transactionId);
}
