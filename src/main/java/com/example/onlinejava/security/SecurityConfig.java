package com.example.onlinejava.security;

import com.example.onlinejava.user.AppUserLoginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Security configuration active when the {@code dev} profile is not set,
 * requiring OAuth2 authentication for all routes except a small public set.
 */
@Configuration
@Profile("!dev")
public class SecurityConfig {

  /** Where unauthenticated page requests are sent to start the GitHub login. */
  private static final String GITHUB_LOGIN_URL = "/oauth2/authorization/github";

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
            .requestMatchers("/", "/index.html", "/css/login.css", "/manifest.json",
                "/icons/**", "/oauth2/**", "/login/**")
            .permitAll()

            .anyRequest()
            .authenticated())

        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo.userService(appUserLoginService))
            .defaultSuccessUrl("/sandbox", true))

        // JSON API calls get a plain 401 instead of a redirect to GitHub, so
        // the page's JavaScript can show a "please sign in" message. Pages
        // keep redirecting to the GitHub login. Registering any entry point
        // here replaces oauth2Login's default, so both must be listed.
        .exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                PathPatternRequestMatcher.pathPattern("/api/**"))
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(GITHUB_LOGIN_URL),
                AnyRequestMatcher.INSTANCE))

        .csrf(csrf -> csrf.ignoringRequestMatchers("/sandbox/run"));

    SecurityHeaders.apply(http);

    return http.build();
  }
}
