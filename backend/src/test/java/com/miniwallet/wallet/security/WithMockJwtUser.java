package com.miniwallet.wallet.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Teste, giris yapmis bir kullanici (JWT'nin sub alani = userId) verir.
 * Controller'larin @AuthenticationPrincipal Jwt parametresi bu kimlikle dolar.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = WithMockJwtUser.Factory.class)
public @interface WithMockJwtUser {

    long userId() default 7L;

    class Factory implements WithSecurityContextFactory<WithMockJwtUser> {

        @Override
        public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
            Jwt jwt = new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(3600),
                    Map.of("alg", "HS256"), Map.of("sub", String.valueOf(annotation.userId())));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            // Yetki listesi verilmezse token "dogrulanmamis" sayilir (403 alinir)
            context.setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
            return context;
        }
    }
}
