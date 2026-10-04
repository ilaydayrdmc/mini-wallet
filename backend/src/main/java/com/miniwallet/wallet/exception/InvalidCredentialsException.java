package com.miniwallet.wallet.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        // Kullanici adinin mi sifrenin mi yanlis oldugunu bilerek soylemeyiz
        super("Kullanici adi veya sifre hatali");
    }
}
