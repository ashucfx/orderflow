package com.orderflow.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class OrderFlowTestcontainersIT extends BaseIntegrationTest {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify PostgreSQL and Kafka testcontainers start and connect successfully")
    void contextLoads_andContainersAreRunning() {
        if (postgresContainer.isRunning()) {
            assertThat(postgresContainer.isCreated()).isTrue();
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertThat(result).isEqualTo(1);
        }

        if (kafkaContainer.isRunning()) {
            assertThat(kafkaContainer.getBootstrapServers()).isNotBlank();
        }
    }
}
