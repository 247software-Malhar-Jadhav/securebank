package com.securebank.domain;

import com.securebank.domain.enums.FraudDecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * JPA entity for the {@code fraud_assessments} table - the persisted result of
 * scoring a transaction for fraud.
 *
 * <p>Produced by {@code FraudScoringService}, which combines several scoring
 * {@code FraudStrategy} implementations. {@code reasons} is a JSONB list of
 * human-readable explanations (e.g. "amount above 95th percentile") so an
 * analyst can understand why a transaction was flagged.</p>
 */
@Entity
@Table(name = "fraud_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FraudDecision decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> reasons;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
