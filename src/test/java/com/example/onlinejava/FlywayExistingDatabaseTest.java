package com.example.onlinejava;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Regression test for the first production deploy of Flyway: the live
 * database already existed (with tables, but no Flyway history), and Flyway
 * refused to start, taking the whole site down. The app must baseline such a
 * database and still apply every migration.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(FlywayExistingDatabaseTest.PreExistingSchema.class)
class FlywayExistingDatabaseTest {

  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void migratesDatabaseThatAlreadyHasTables() {
    assertThat(jdbc.queryForObject(
        "select count(*) from flyway_schema_history where success and version is not null "
            + "and version <> '0'", Integer.class))
        .isGreaterThanOrEqualTo(3);
    assertThat(jdbc.queryForObject(
        "select count(*) from information_schema.tables where table_name in "
            + "('app_user', 'discussion_post', 'attachment', 'legacy_table')", Integer.class))
        .isEqualTo(4);
  }

  /**
   * A PostgreSQL that already contains an unrelated table, like production did.
   */
  @TestConfiguration(proxyBeanMethods = false)
  static class PreExistingSchema {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresWithExistingTable() {
      return new PostgreSQLContainer("postgres:17")
          .withInitScript("db/pre-existing-schema.sql");
    }
  }
}
