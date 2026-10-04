package com.miniwallet.wallet.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Hesap bulunamadi: " + id);
    }
}
