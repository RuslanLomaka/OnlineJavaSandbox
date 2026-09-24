package com.example.onlinejava.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * Unit tests for {@link ProviderProfile#from(String, Map)}.
 */
class ProviderProfileTest {

  @Test
  void mapsGitHubAttributes() {
    final ProviderProfile info = ProviderProfile.from("github", Map.of(
        "id", 12345,
        "login", "octocat",
        "name", "The Octocat",
        "avatar_url", "https://avatars.githubusercontent.com/u/12345"
    ));

    assertThat(info.provider()).isEqualTo("github");
    assertThat(info.providerUserId()).isEqualTo("12345");
    assertThat(info.login()).isEqualTo("octocat");
    assertThat(info.displayName()).isEqualTo("The Octocat");
    assertThat(info.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/12345");
  }

  @Test
  void fallsBackToLoginWhenGitHubNameIsMissing() {
    final Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 7L);
    attributes.put("login", "no-name");
    attributes.put("name", null);

    final ProviderProfile info = ProviderProfile.from("github", attributes);

    assertThat(info.displayName()).isEqualTo("no-name");
    assertThat(info.avatarUrl()).isNull();
  }

  @Test
  void rejectsNonHttpsAvatarUrl() {
    final ProviderProfile info = ProviderProfile.from("github", Map.of(
        "id", 1, "login", "x", "avatar_url", "javascript:alert(1)"));

    assertThat(info.avatarUrl()).isNull();
  }

  @Test
  void rejectsMissingId() {
    assertThatThrownBy(() -> ProviderProfile.from("github", Map.of("login", "x")))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }

  @Test
  void rejectsUnsupportedProvider() {
    assertThatThrownBy(() -> ProviderProfile.from("myspace", Map.of("id", 1)))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }
}
