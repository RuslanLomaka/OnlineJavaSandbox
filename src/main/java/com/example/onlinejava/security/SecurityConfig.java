package com.example.onlinejava.security;

import com.example.onlinejava.user.AppUserLoginService;
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
   * <p>Every successful login goes through {@link AppUserLoginService},
   * which records the user in the database and makes the principal an
   * {@link com.example.onlinejava.user.AppUserPrincipal}.
   *
   * @param http the security configuration builder
   * @param appUserLoginService records logins and builds the principal
   * @return the configured filter chain
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AppUserLoginService appUserLoginService
  ) {

    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/", "/index.html", "/css/login.css", "/oauth2/**", "/login/**")
            .permitAll()

            .anyRequest()
            .authenticated())

        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo.userService(appUserLoginService))
            .defaultSuccessUrl("/sandbox", true))

        .csrf(csrf -> csrf.ignoringRequestMatchers("/sandbox/run"));

    return http.build();
  }
}
