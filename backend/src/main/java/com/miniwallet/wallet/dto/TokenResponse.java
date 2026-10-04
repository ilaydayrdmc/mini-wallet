package com.miniwallet.wallet.dto;

/** expiresIn: tokenin gecerlilik suresi (saniye). */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
