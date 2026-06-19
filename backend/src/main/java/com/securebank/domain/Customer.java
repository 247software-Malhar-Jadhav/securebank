package com.securebank.domain;

import com.securebank.domain.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity for the {@code customers} table - the profile/KYC data behind a
 * {@link User}.
 *
 * <p>We model the link to {@link User} as a lazy {@code @OneToOne} so loading a
 * customer does not eagerly drag the user row along unless we actually navigate
 * to it. The relationship is one-to-one: each login owns exactly one customer
 * profile.</p>
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 32)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 16)
    private KycStatus kycStatus;

    @Column(name = "address_line")
    private String addressLine;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(length = 64)
    private String country;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (kycStatus == null) kycStatus = KycStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
