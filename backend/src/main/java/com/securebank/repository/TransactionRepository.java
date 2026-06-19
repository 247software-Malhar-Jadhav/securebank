package com.securebank.repository;

import com.securebank.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link Transaction} (Repository pattern).
 *
 * <p>Extends {@link JpaSpecificationExecutor} so we can run dynamic, type-safe
 * queries built with the Specification pattern (used for the admin/transaction
 * search filters) instead of hand-writing a query method per filter combination.</p>
 */
public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    /**
     * Sum of completed money LEAVING an account since a given instant.
     *
     * <p>Used by the daily-limit validation handler. A WITHDRAWAL counts in
     * full; a TRANSFER counts when the account is the SOURCE (i.e. it is the
     * {@code account} of the transaction, which by our convention is always the
     * debited side for transfers).</p>
     */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.account.id = :accountId
              and t.status = com.securebank.domain.enums.TransactionStatus.COMPLETED
              and t.type in (com.securebank.domain.enums.TransactionType.WITHDRAWAL,
                             com.securebank.domain.enums.TransactionType.TRANSFER)
              and t.createdAt >= :since
            """)
    BigDecimal sumOutgoingSince(Long accountId, Instant since);

    /** Recent transactions on an account, used by the statistical fraud strategy. */
    List<Transaction> findTop50ByAccountIdOrderByCreatedAtDesc(Long accountId);
}
