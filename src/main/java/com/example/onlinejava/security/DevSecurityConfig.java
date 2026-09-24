package com.example.onlinejava.security;

import com.example.onlinejava.user.AppUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Security configuration active under the {@code dev} profile, allowing all
 * requests without a real login for local development.
 *
 * <p>When the user service is available (i.e. a database is configured),
 * every request is signed in as a fixed "local-dev" user via
 * {@link DevUserAuthenticationFilter}, so login-only features can be exercised
 * locally. Web-layer test slices that don't load JPA simply get no user.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

  /**
   * Builds a permissive security filter chain that permits all requests
   * and disables CSRF protection, for local development only.
   *
   * @param http the security configuration builder
   * @param appUserService user service, absent in web-layer test slices
   * @param serverAddress value of {@code server.address}; must be loopback
   * @return the configured filter chain
   */
  // CSRF is safe to disable here: this bean only activates under
  // @Profile("dev"), and DevUserAuthenticationFilter refuses to start unless
  // the server only listens on loopback, so no other site's page can be
  // served to a victim who is "logged in" here.
  @SuppressWarnings("java:S4502")
  @Bean
  public SecurityFilterChain devSecurityFilterChain(
      final HttpSecurity http,
      final ObjectProvider<AppUserService> appUserService,
      @Value("${server.address:}") final String serverAddress
  ) {

    http
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest()
            .permitAll()
        )
        .csrf(AbstractHttpConfigurer::disable);

    SecurityHeaders.apply(http);

    appUserService.ifAvailable(service -> http.addFilterBefore(
        new DevUserAuthenticationFilter(service, serverAddress),
        AnonymousAuthenticationFilter.class));

    return http.build();
  }
}
