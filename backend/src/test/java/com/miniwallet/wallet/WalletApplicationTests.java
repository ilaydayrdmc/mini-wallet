package com.miniwallet.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfig.class)
@SpringBootTest
class WalletApplicationTests {

    @Test
    void contextLoads() {
        // Flyway migration'lari bos bir PostgreSQL'e uygulanir ve Hibernate sema dogrulamasi gecer
    }
}
