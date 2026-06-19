package com.securebank.service;

import com.securebank.concurrency.LockManager;
import com.securebank.concurrency.RetryExecutor;
import com.securebank.domain.Transaction;
import com.securebank.domain.enums.TransactionType;
import com.securebank.dto.TransactionDtos.*;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.mapper.TransactionMapper;
import com.securebank.repository.TransactionRepository;
import com.securebank.service.transaction.TransactionContext;
import com.securebank.service.transaction.TransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

/**
 * Public entry point for money movement (deposit / withdraw / transfer).
 *
 * <p>Responsibilities are deliberately thin here - this class composes the
 * concurrency controls around the transactional unit:</p>
 * <ol>
 *   <li>Build a {@link TransactionContext} from the request.</li>
 *   <li>Acquire the DISTRIBUTED lock(s) (Redisson) for the involved account(s),
 *       in ascending account-id order to match the DB-lock order (no deadlock).</li>
 *   <li>Inside the lock, run the unit under {@link RetryExecutor} so an
 *       OPTIMISTIC-lock collision retries with backoff.</li>
 *   <li>The unit itself is {@link TransactionExecutor#execute} - a separate bean
 *       so its {@code @Transactional} proxy actually applies (no self-invocation).
 *       That is where the PESSIMISTIC row locks + double-entry happen (Template
 *       Method).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionExecutor transactionExecutor;
    private final LockManager lockManager;
    private final RetryExecutor retryExecutor;

    // ---- write operations --------------------------------------------------

    public TransactionResponse deposit(String username, DepositRequest req) {
        return run(new TransactionContext(
                TransactionType.DEPOSIT, req.amount(), req.description(), username,
                req.accountId(), null));
    }

    public TransactionResponse withdraw(String username, WithdrawRequest req) {
        return run(new TransactionContext(
                TransactionType.WITHDRAWAL, req.amount(), req.description(), username,
                req.accountId(), null));
    }

    public TransactionResponse transfer(String username, TransferRequest req) {
        return run(new TransactionContext(
                TransactionType.TRANSFER, req.amount(), req.description(), username,
                req.sourceAccountId(), req.destinationAccountId()));
    }

    // ---- read operations ---------------------------------------------------

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        Transaction txn = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("error.transaction.notFound", reference));
        return transactionMapper.toResponse(txn);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listForAccount(Long accountId, int page, int size) {
        Page<Transaction> result = transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size));
        return transactionMapper.toResponseList(result.getContent());
    }

    // ---- orchestration -----------------------------------------------------

    /** Wrap the transactional unit in the distributed lock(s) + optimistic retry. */
    private TransactionResponse run(TransactionContext ctx) {
        List<String> lockKeys = lockKeysFor(ctx);
        Transaction txn = withLocks(lockKeys,
                () -> retryExecutor.executeWithRetry(() -> transactionExecutor.execute(ctx)));
        return transactionMapper.toResponse(txn);
    }

    /** Distributed-lock keys, sorted ascending by account id (deadlock-free order). */
    private List<String> lockKeysFor(TransactionContext ctx) {
        if (ctx.getCounterpartyAccountId() == null) {
            return List.of("account:" + ctx.getPrimaryAccountId());
        }
        long a = ctx.getPrimaryAccountId();
        long b = ctx.getCounterpartyAccountId();
        return List.of("account:" + Math.min(a, b), "account:" + Math.max(a, b));
    }

    /**
     * Acquire the (already ordered) locks by nesting them - the outermost lock is
     * the lowest key - then run the action under all of them.
     */
    private <T> T withLocks(List<String> keys, Supplier<T> action) {
        Supplier<T> wrapped = action;
        for (int i = keys.size() - 1; i >= 0; i--) {
            final String key = keys.get(i);
            final Supplier<T> inner = wrapped;
            wrapped = () -> lockManager.withLock(key, inner);
        }
        return wrapped.get();
    }
}
