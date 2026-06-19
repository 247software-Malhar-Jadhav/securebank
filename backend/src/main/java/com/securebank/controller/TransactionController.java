package com.securebank.controller;

import com.securebank.dto.TransactionDtos.*;
import com.securebank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Money-movement endpoints: deposit, withdraw, transfer, and lookup by reference.
 *
 * <p>Each write delegates to {@link TransactionService}, which applies the full
 * locking + validation-chain + double-entry pipeline. The controller stays a thin
 * HTTP layer; it passes the authenticated username so the service can scope and
 * audit the operation.</p>
 */
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Deposit, withdraw, transfer, and view")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Deposit money into an account")
    @PostMapping("/deposit")
    public TransactionResponse deposit(Principal principal,
                                       @Valid @RequestBody DepositRequest request) {
        return transactionService.deposit(principal.getName(), request);
    }

    @Operation(summary = "Withdraw money from an account")
    @PostMapping("/withdraw")
    public TransactionResponse withdraw(Principal principal,
                                        @Valid @RequestBody WithdrawRequest request) {
        return transactionService.withdraw(principal.getName(), request);
    }

    @Operation(summary = "Transfer money between accounts (locked, double-entry)")
    @PostMapping("/transfer")
    public TransactionResponse transfer(Principal principal,
                                        @Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(principal.getName(), request);
    }

    @Operation(summary = "Look up a transaction by its public reference")
    @GetMapping("/{reference}")
    public TransactionResponse byReference(@PathVariable String reference) {
        return transactionService.getByReference(reference);
    }
}
