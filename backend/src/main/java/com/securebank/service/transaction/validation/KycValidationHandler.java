package com.securebank.service.transaction.validation;

import com.securebank.domain.Account;
import com.securebank.domain.enums.AccountStatus;
import com.securebank.domain.enums.KycStatus;
import com.securebank.exception.TransactionValidationException;
import com.securebank.service.transaction.TransactionContext;
import org.springframework.stereotype.Component;

/**
 * Chain link #1: KYC + account-status check.
 *
 * <p>Runs first (lowest order) because it is the cheapest, most fundamental gate:
 * no money may move unless the owning customer is KYC-verified and the involved
 * accounts are ACTIVE. Failing here short-circuits the rest of the chain.</p>
 */
@Component
public class KycValidationHandler implements ValidationHandler {

    @Override
    public void validate(TransactionContext ctx) {
        checkAccount(ctx.getPrimaryAccount());
        if (ctx.getCounterpartyAccount() != null) {
            checkAccount(ctx.getCounterpartyAccount());
        }
    }

    private void checkAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransactionValidationException(
                    "error.transaction.accountNotActive", account.getAccountNumber());
        }
        KycStatus kyc = account.getCustomer().getKycStatus();
        if (kyc != KycStatus.VERIFIED) {
            throw new TransactionValidationException("error.transaction.kycNotVerified");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
