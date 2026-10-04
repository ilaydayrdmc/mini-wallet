package com.miniwallet.wallet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.dto.TransferRequest;
import com.miniwallet.wallet.security.CurrentUser;
import com.miniwallet.wallet.service.TransferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(CurrentUser.id(jwt), request);
    }
}
