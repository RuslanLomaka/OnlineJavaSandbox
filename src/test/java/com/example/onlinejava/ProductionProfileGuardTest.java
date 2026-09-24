package com.example.onlinejava;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Guards against the production app silently running the {@code dev} profile.
 *
 * <p>Regression test for an outage: the default profile was switched to
 * {@code dev} and production (which set no profile) started with dev
 * settings, bound to localhost inside its container, and served nothing.
 */
class ProductionProfileGuardTest {

  @Test
  void defaultProfileIsProductionSoNothingFallsBackToDevSecurity() throws IOException {
    final Properties properties = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
      properties.load(in);
    }

    assertThat(properties.getProperty("spring.profiles.default")).isEqualTo("prod");
  }

  @Test
  void composeSetsProductionProfileExplicitly() throws IOException {
    final String compose = Files.readString(Path.of("compose.yaml"));

    assertThat(compose).containsPattern("SPRING_PROFILES_ACTIVE:\\s*prod");
  }
}
