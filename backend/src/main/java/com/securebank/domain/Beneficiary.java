package com.securebank.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for the {@code beneficiaries} table - a payee a customer has saved
 * for repeated transfers. Stores just enough to address a payment (name,
 * account number, bank); it does not hold money itself.
 */
@Entity
@Table(name = "beneficiaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "account_number", nullable = false, length = 34)
    private String accountNumber;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
