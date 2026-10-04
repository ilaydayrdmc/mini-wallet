package com.miniwallet.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.miniwallet.wallet.entity.Account;

public record AccountResponse(Long id, String ownerName, BigDecimal balance, Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerName(),
                account.getBalance(), account.getCreatedAt());
    }
}
