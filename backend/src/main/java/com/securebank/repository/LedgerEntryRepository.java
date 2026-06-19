package com.securebank.repository;

import com.securebank.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link LedgerEntry} (Repository pattern).
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionId(Long transactionId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
