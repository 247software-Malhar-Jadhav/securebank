package com.securebank.service;

import com.securebank.domain.Account;
import com.securebank.domain.Customer;
import com.securebank.domain.enums.AccountStatus;
import com.securebank.domain.enums.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Factory that builds a fully-initialized {@link Account} for a given type
 * (Factory pattern - "account creation by type" from the spec).
 *
 * <p>Different products warrant different defaults; encapsulating that here keeps
 * the service free of {@code switch} statements over account type. Today the
 * defaults are simple (currency, ACTIVE status, a generated number); the factory
 * is the natural home for future per-type rules such as minimum balances or
 * fixed-deposit maturity.</p>
 */
@Component
@RequiredArgsConstructor
public class AccountFactory {

    private final AccountNumberFactory accountNumberFactory;

    /**
     * Create a new (unsaved) account of the requested type for the customer.
     *
     * @param customer       owner
     * @param type           product type
     * @param currency       ISO currency (defaults to INR if null/blank)
     * @param initialBalance opening balance (defaults to zero if null)
     */
    public Account create(Customer customer, AccountType type, String currency,
                          BigDecimal initialBalance) {
        return Account.builder()
                .accountNumber(accountNumberFactory.generate())
                .customer(customer)
                .type(type)
                .currency(currency == null || currency.isBlank() ? "INR" : currency)
                .balance(initialBalance == null ? BigDecimal.ZERO : initialBalance)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
