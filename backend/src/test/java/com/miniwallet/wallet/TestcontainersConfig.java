package com.miniwallet.wallet;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Testler icin gecici bir PostgreSQL container'i baslatir. @ServiceConnection sayesinde
 * Spring, datasource ayarlarini container'dan otomatik alir (application.properties'teki
 * localhost ayari kullanilmaz).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }
}
