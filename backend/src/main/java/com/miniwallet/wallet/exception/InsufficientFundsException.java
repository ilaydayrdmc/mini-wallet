package com.miniwallet.wallet.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long accountId) {
        super("Yetersiz bakiye. Hesap: " + accountId);
    }
}
