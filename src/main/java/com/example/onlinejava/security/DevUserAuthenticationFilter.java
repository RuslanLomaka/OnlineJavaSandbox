package com.example.onlinejava.security;

import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserPrincipal;
import com.example.onlinejava.user.AppUserService;
import com.example.onlinejava.user.ProviderProfile;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Dev-profile only: signs every request in as a fixed "local-dev" user so
 * login-only features (posting, uploads) work locally without GitHub OAuth.
 *
 * <p>Because this bypasses real authentication, the constructor refuses to
 * create the filter unless the server is bound to a loopback address. That
 * turns "the dev profile accidentally ran in production" into a startup
 * failure instead of an open, pre-authenticated site.
 */
public final class DevUserAuthenticationFilter extends OncePerRequestFilter {

  /** Provider value used for the fake user so it can never clash with a real login. */
  static final String DEV_PROVIDER = "dev";

  private final AppUserService appUserService;

  /** Lazily created on first request, once the database is reachable. */
  private volatile AppUserPrincipal principal;

  /**
   * Creates the filter.
   *
   * @param appUserService used to create/refresh the local-dev user row
   * @param serverAddress value of {@code server.address}
   * @throws IllegalStateException if {@code serverAddress} is not loopback
   */
  public DevUserAuthenticationFilter(
      final AppUserService appUserService,
      final String serverAddress
  ) {
    if (!isLoopback(serverAddress)) {
      throw new IllegalStateException(
          "Dev fake-user login requires server.address to be a loopback address, but was '"
              + serverAddress + "'. Is the dev profile active outside local development?");
    }
    this.appUserService = appUserService;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain chain
  ) throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      final AppUserPrincipal user = devPrincipal();
      final SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(
          new OAuth2AuthenticationToken(user, user.getAuthorities(), DEV_PROVIDER));
      SecurityContextHolder.setContext(context);
    }
    chain.doFilter(request, response);
  }

  private AppUserPrincipal devPrincipal() {
    AppUserPrincipal current = principal;
    if (current == null) {
      synchronized (this) {
        current = principal;
        if (current == null) {
          final AppUser user = appUserService.upsert(new ProviderProfile(
              DEV_PROVIDER, "local-dev", "local-dev", "Local developer", null));
          current = new AppUserPrincipal(user, Map.of("login", "local-dev"),
              AuthorityUtils.createAuthorityList("ROLE_USER"));
          principal = current;
        }
      }
    }
    return current;
  }

  private static boolean isLoopback(final String address) {
    if (address == null || address.isBlank()) {
      return false;
    }
    try {
      return List.of(InetAddress.getAllByName(address)).stream()
          .allMatch(InetAddress::isLoopbackAddress);
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
