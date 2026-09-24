package com.example.onlinejava;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a {@link Clock} bean so time-dependent code can be tested with a
 * fixed clock instead of {@code Instant.now()}.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  /**
   * Returns the system UTC clock.
   *
   * @return clock used for all persisted timestamps
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
