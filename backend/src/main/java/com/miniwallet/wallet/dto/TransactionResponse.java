package com.miniwallet.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.miniwallet.wallet.entity.Transaction;
import com.miniwallet.wallet.entity.TransactionType;

public record TransactionResponse(Long id, Long accountId, TransactionType type,
        BigDecimal amount, BigDecimal balanceAfter, Instant createdAt) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(tx.getId(), tx.getAccount().getId(), tx.getType(),
                tx.getAmount(), tx.getBalanceAfter(), tx.getCreatedAt());
    }
}
