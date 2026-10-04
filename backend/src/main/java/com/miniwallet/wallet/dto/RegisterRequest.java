package com.miniwallet.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "[A-Za-z0-9_.]+", message = "yalnizca harf, rakam, '_' ve '.' icerebilir")
        String username,

        // BCrypt en fazla 72 bayti dikkate alir; daha uzun sifreleri reddederiz
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {
}
