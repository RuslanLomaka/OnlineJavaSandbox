package com.example.onlinejava;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Provides a throwaway PostgreSQL container for integration tests.
 *
 * <p>{@link ServiceConnection} makes Spring Boot point the datasource at the
 * container automatically, so tests run against the same database engine
 * (and the same Flyway migrations) as production. Import this class with
 * {@code @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /**
   * Starts a PostgreSQL 17 container, matching the version in compose.yaml.
   *
   * @return the container Spring Boot connects to
   */
  @Bean
  @ServiceConnection
  public PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer("postgres:17");
  }
}
