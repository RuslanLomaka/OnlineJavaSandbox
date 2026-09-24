package com.example.onlinejava;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full application context (including Flyway migrations and
 * Hibernate schema validation) against a Testcontainers PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
class OnlineJavaApplicationTests {

  @Test
  void contextLoads() {
  }

}
