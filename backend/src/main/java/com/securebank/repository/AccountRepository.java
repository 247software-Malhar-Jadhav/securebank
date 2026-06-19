package com.securebank.repository;

import com.securebank.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link Account} (Repository pattern), with the
 * critical PESSIMISTIC locking method used by money movement.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(Long customerId);

    List<Account> findByCustomerUserUsername(String username);

    /**
     * Load an account row taking a PESSIMISTIC_WRITE lock.
     *
     * <p>{@code LockModeType.PESSIMISTIC_WRITE} translates to a
     * {@code SELECT ... FOR UPDATE} in Postgres. While this transaction holds
     * the lock, no other transaction can read-for-update or modify the same row;
     * they block until we commit. This is how we serialize concurrent writers on
     * the same account so balances cannot be corrupted by interleaved
     * read-modify-write cycles.</p>
     *
     * <p>Transfers MUST acquire these locks in a deterministic order (lowest
     * account id first) to avoid deadlocks - see the TransferProcessor.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
