package com.miniwallet.wallet.exception;

public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Bu kullanici adi zaten alinmis: " + username);
    }
}
