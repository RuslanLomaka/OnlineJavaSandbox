package com.example.onlinejava;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration active under the {@code dev} profile, allowing all
 * requests without authentication for local development.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

  /**
   * Builds a permissive security filter chain that permits all requests
   * and disables CSRF protection, for local development only.
   *
   * @param http the security configuration builder
   * @return the configured filter chain
   */
  // CSRF is safe to disable here: this bean only activates under
  // @Profile("dev"), which permits every request with no authentication
  // at all, so there is no authenticated session for a forged
  // cross-site request to exploit.
  @SuppressWarnings("java:S4502")
  @Bean
  public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) {

    http
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest()
            .permitAll()
        )
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }
}