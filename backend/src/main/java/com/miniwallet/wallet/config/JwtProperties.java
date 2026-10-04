package com.miniwallet.wallet.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.properties icindeki app.jwt.* ayarlari. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
