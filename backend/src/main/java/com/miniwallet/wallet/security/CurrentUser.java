package com.miniwallet.wallet.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Dogrulanmis JWT'den, istegi yapan kullanicinin id'sini (sub alani) okur. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
