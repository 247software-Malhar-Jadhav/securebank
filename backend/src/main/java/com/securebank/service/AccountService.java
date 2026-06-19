package com.securebank.service;

import com.securebank.config.Audited;
import com.securebank.config.CacheConfig;
import com.securebank.domain.Account;
import com.securebank.domain.Customer;
import com.securebank.dto.AccountDtos.AccountResponse;
import com.securebank.dto.AccountDtos.OpenAccountRequest;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.mapper.AccountMapper;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Account use cases: open an account, list my accounts, get one by id.
 *
 * <p><b>Caching decorator (Redis):</b> {@code getById} is annotated
 * {@code @Cacheable}. Spring's caching abstraction wraps the method so a hit in
 * the {@code accounts} Redis cache returns immediately without touching the
 * database - the cache "decorates" the read transparently. On any write that
 * changes a balance/status (open here; deposits/withdraws/transfers in the
 * transaction service) we {@code @CacheEvict} that account's entry so a cached
 * balance can never lag the real one.</p>
 *
 * <p>The account-number generation goes through {@link AccountFactory} (Factory),
 * and opening an account is {@link Audited} so it lands in the audit log via the
 * AOP aspect.</p>
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountFactory accountFactory;
    private final AccountMapper accountMapper;

    /** Open a new account for the authenticated customer. */
    @Transactional
    @Audited(action = "ACCOUNT_OPENED", entityType = "ACCOUNT")
    public AccountResponse openAccount(String username, OpenAccountRequest req) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("error.customer.notFound"));

        BigDecimal initial = req.initialDeposit() == null ? BigDecimal.ZERO : req.initialDeposit();
        Account account = accountFactory.create(customer, req.type(), req.currency(), initial);
        account = accountRepository.save(account);
        return accountMapper.toResponse(account);
    }

    /** List the authenticated customer's accounts. */
    @Transactional(readOnly = true)
    public List<AccountResponse> listMyAccounts(String username) {
        return accountMapper.toResponseList(
                accountRepository.findByCustomerUserUsername(username));
    }

    /**
     * Get an account by id, served from the Redis cache when possible.
     *
     * <p>{@code @Cacheable} key is the account id; only non-null results are
     * cached (configured in CacheConfig). This is the read decorated by Redis.</p>
     */
    @Cacheable(cacheNames = CacheConfig.ACCOUNTS_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public AccountResponse getById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return accountMapper.toResponse(account);
    }

    /**
     * Evict an account's cached entry. Called by the transaction service after a
     * balance change so the next read reflects the new balance. Public so other
     * services can invalidate; the annotation does the actual eviction.
     */
    @CacheEvict(cacheNames = CacheConfig.ACCOUNTS_CACHE, key = "#id")
    public void evictCache(Long id) {
        // Body intentionally empty: the @CacheEvict annotation is the behaviour.
    }
}
