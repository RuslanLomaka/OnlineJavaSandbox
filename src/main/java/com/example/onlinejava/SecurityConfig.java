package com.example.onlinejava;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration active when the {@code dev} profile is not set,
 * requiring OAuth2 authentication for all routes except a small public set.
 */
@Configuration
@Profile("!dev")
public class SecurityConfig {

  /**
   * Builds the production security filter chain: permits login-related
   * routes, requires OAuth2 authentication for everything else, and
   * exempts the sandbox execution endpoint from CSRF protection.
   *
   * @param http the security configuration builder
   * @return the configured filter chain
   * @throws Exception if the security configuration cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/", "/index.html", "/css/login.css", "/oauth2/**", "/login/**")
            .permitAll()

            .anyRequest()
            .authenticated())

        .oauth2Login(oauth -> oauth.defaultSuccessUrl("/sandbox", true))

        .csrf(csrf -> csrf.ignoringRequestMatchers("/sandbox/run"));

    return http.build();
  }
}
