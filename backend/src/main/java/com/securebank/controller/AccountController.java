package com.securebank.controller;

import com.securebank.dto.AccountDtos.AccountResponse;
import com.securebank.dto.AccountDtos.OpenAccountRequest;
import com.securebank.dto.TransactionDtos.TransactionResponse;
import com.securebank.service.AccountService;
import com.securebank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Account endpoints: list my accounts, open an account, get one by id, and list
 * an account's transactions.
 *
 * <p>The caller's identity comes from the {@link Principal} that the JWT filter
 * placed in the security context - we never trust a username from the request
 * body. Reads of a single account go through the {@code @Cacheable} service
 * method so they can be served from Redis.</p>
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Open and view accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @Operation(summary = "List the authenticated customer's accounts")
    @GetMapping
    public List<AccountResponse> myAccounts(Principal principal) {
        return accountService.listMyAccounts(principal.getName());
    }

    @Operation(summary = "Open a new account")
    @PostMapping
    public ResponseEntity<AccountResponse> open(Principal principal,
                                                @Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.openAccount(principal.getName(), request));
    }

    @Operation(summary = "Get an account by id (cached)")
    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @Operation(summary = "List an account's transactions (most recent first)")
    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> transactions(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return transactionService.listForAccount(id, page, size);
    }
}
