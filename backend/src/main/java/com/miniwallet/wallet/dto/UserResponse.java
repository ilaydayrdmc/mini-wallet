package com.miniwallet.wallet.dto;

import java.time.Instant;

import com.miniwallet.wallet.entity.User;

/** Sifre (hash dahil) hicbir zaman API yanitina konmaz. */
public record UserResponse(Long id, String username, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
