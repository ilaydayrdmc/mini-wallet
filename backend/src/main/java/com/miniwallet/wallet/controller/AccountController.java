package com.miniwallet.wallet.controller;

import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniwallet.wallet.dto.AccountResponse;
import com.miniwallet.wallet.dto.AmountRequest;
import com.miniwallet.wallet.dto.CreateAccountRequest;
import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.security.CurrentUser;
import com.miniwallet.wallet.service.AccountService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<AccountResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return accountService.findAll(CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody AmountRequest request) {
        return accountService.deposit(CurrentUser.id(jwt), id, request);
    }

    @GetMapping("/{id}/transactions")
    public PagedModel<TransactionResponse> transactions(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return accountService.getTransactions(CurrentUser.id(jwt), id, page, size);
    }

    @PostMapping("/{id}/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody AmountRequest request) {
        return accountService.withdraw(CurrentUser.id(jwt), id, request);
    }
}
