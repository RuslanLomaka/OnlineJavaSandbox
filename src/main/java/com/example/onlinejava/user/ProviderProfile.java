package com.example.onlinejava.user;

import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Provider-independent view of the profile an OAuth2 provider returns.
 *
 * <p>Each provider names its attributes differently (GitHub uses
 * {@code id}/{@code login}/{@code avatar_url}, OIDC providers use
 * {@code sub}/{@code name}/{@code picture}); {@link #from(String, Map)} is the
 * single place that knows those differences. Adding a provider means adding
 * one case there.
 *
 * @param provider OAuth2 registration id, e.g. {@code github}
 * @param providerUserId the provider's stable id for the user
 * @param login username/handle
 * @param displayName human-readable name, never blank
 * @param avatarUrl HTTPS avatar URL, or {@code null}
 */
public record ProviderProfile(
    String provider,
    String providerUserId,
    String login,
    String displayName,
    String avatarUrl
) {

  /**
   * Extracts user info from a provider's attribute map.
   *
   * @param provider OAuth2 registration id
   * @param attributes attributes returned by the provider's user-info endpoint
   * @return normalized user info
   * @throws OAuth2AuthenticationException if the provider is unsupported or
   *     a required attribute is missing
   */
  public static ProviderProfile from(final String provider, final Map<String, Object> attributes) {
    if (!"github".equals(provider)) {
      throw failure("Unsupported OAuth2 provider: " + provider);
    }
    final Object id = attributes.get("id");
    final Object login = attributes.get("login");
    if (id == null || login == null) {
      throw failure("GitHub user info is missing 'id' or 'login'");
    }
    final Object name = attributes.get("name");
    final String displayName =
        name instanceof String s && !s.isBlank() ? s : login.toString();
    return new ProviderProfile(
        provider,
        id.toString(),
        login.toString(),
        displayName,
        httpsOrNull(attributes.get("avatar_url"))
    );
  }

  private static String httpsOrNull(final Object url) {
    // Avatar URLs end up in <img src>, so only plain HTTPS URLs are kept.
    return url instanceof String s && s.startsWith("https://") ? s : null;
  }

  private static OAuth2AuthenticationException failure(final String message) {
    return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), message);
  }
}
