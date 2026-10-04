package com.miniwallet.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniwallet.wallet.config.JwtConfig;
import com.miniwallet.wallet.config.JwtProperties;
import com.miniwallet.wallet.dto.TokenResponse;
import com.miniwallet.wallet.entity.User;

/** Spring baglami kurmadan, gercek JwtConfig ile uretilen encoder/decoder uzerinde. */
class TokenServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123";

    private final JwtConfig config = new JwtConfig();

    private final User user = userWithId(42L, "ayse");

    private static User userWithId(Long id, String username) {
        User user = new User(username, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TokenService serviceWith(String secret, Duration expiration) {
        JwtProperties properties = new JwtProperties(secret, expiration);
        SecretKey key = config.jwtSecretKey(properties);
        return new TokenService(config.jwtEncoder(key), properties);
    }

    private JwtDecoder decoderFor(String secret) {
        return config.jwtDecoder(config.jwtSecretKey(new JwtProperties(secret, Duration.ofHours(1))));
    }

    @Test
    void generatedToken_carriesUserIdentityAndExpiry() {
        TokenResponse response = serviceWith(SECRET, Duration.ofHours(1)).generateToken(user);

        Jwt jwt = decoderFor(SECRET).decode(response.accessToken());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("ayse");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("mini-wallet");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void tamperedToken_isRejected() {
        String token = serviceWith(SECRET, Duration.ofHours(1)).generateToken(user).accessToken();
        String[] parts = token.split("\\.");
        // Govdeyi (payload) degistir, imzayi oldugu gibi birak
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"username\":\"admin\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> decoderFor(SECRET).decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String otherSecret = "baska-bir-sir-baska-bir-sir-baska-bir-sir";
        String token = serviceWith(otherSecret, Duration.ofHours(1)).generateToken(user).accessToken();

        assertThatThrownBy(() -> decoderFor(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        // TokenService gecmis tarihli token uretmez; dogrudan encoder ile uretiyoruz.
        // 5 dk once dolmus: decoder'in varsayilan 60 sn saat toleransinin de disinda.
        JwtEncoder encoder = config.jwtEncoder(
                config.jwtSecretKey(new JwtProperties(SECRET, Duration.ofHours(1))));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(now.minus(Duration.ofMinutes(65)))
                .expiresAt(now.minus(Duration.ofMinutes(5)))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> decoderFor(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecret_failsFast() {
        assertThatThrownBy(() -> serviceWith("kisa-sir", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
