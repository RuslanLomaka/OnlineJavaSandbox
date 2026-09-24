package com.example.onlinejava.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AppUserLoginService}.
 */
class AppUserLoginServiceTest {

  @SuppressWarnings("unchecked")
  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate =
      mock(OAuth2UserService.class);

  private final AppUserService appUserService = mock(AppUserService.class);

  private final AppUserLoginService service =
      new AppUserLoginService(appUserService, delegate);

  @Test
  void upsertsUserAndReturnsPrincipalCarryingDatabaseId() {
    final Map<String, Object> attributes = Map.of("id", 99, "login", "octocat", "name", "Octo");
    when(delegate.loadUser(any())).thenReturn(new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "login"));
    final AppUser stored = new AppUser("github", "99", "octocat", "Octo", null, null);
    ReflectionTestUtils.setField(stored, "id", 5L);
    when(appUserService.upsert(any())).thenReturn(stored);

    final OAuth2User result = service.loadUser(githubRequest());

    assertThat(result).isInstanceOf(AppUserPrincipal.class);
    final AppUserPrincipal principal = (AppUserPrincipal) result;
    assertThat(principal.getUserId()).isEqualTo(5L);
    assertThat(principal.getLogin()).isEqualTo("octocat");
    assertThat(principal.getDisplayName()).isEqualTo("Octo");
    assertThat(principal.getAttributes()).isEqualTo(attributes);
    assertThat(principal.getAuthorities()).extracting("authority").contains("OAUTH2_USER");
  }

  @Test
  void acceptsRealGitHubProfilesContainingNullFields() {
    // GitHub returns null for every profile field the user left empty.
    // Regression: Map.copyOf rejected those nulls, so every real login
    // failed right after GitHub redirected back.
    final Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 99);
    attributes.put("login", "octocat");
    attributes.put("name", null);
    attributes.put("company", null);
    attributes.put("email", null);
    attributes.put("bio", null);
    when(delegate.loadUser(any())).thenReturn(new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "login"));
    final AppUser stored = new AppUser("github", "99", "octocat", "octocat", null, null);
    ReflectionTestUtils.setField(stored, "id", 5L);
    when(appUserService.upsert(any())).thenReturn(stored);

    final OAuth2User result = service.loadUser(githubRequest());

    assertThat(result.getAttributes()).containsEntry("company", null);
    assertThat(result.getName()).isEqualTo("octocat");
  }

  @Test
  void principalSurvivesSessionSerialization() throws Exception {
    // The principal lives in the HTTP session, so it must serialize,
    // including attribute maps with null values.
    final Map<String, Object> attributes = new HashMap<>();
    attributes.put("login", "octocat");
    attributes.put("bio", null);
    final AppUser user = new AppUser("github", "99", "octocat", "Octo", null, null);
    ReflectionTestUtils.setField(user, "id", 5L);
    final AppUserPrincipal principal = new AppUserPrincipal(user, attributes,
        List.of(new SimpleGrantedAuthority("OAUTH2_USER")));

    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(principal);
    }
    try (ObjectInputStream in = new ObjectInputStream(
        new ByteArrayInputStream(bytes.toByteArray()))) {
      final AppUserPrincipal copy = (AppUserPrincipal) in.readObject();
      assertThat(copy.getUserId()).isEqualTo(5L);
      assertThat(copy.getAttributes()).containsEntry("bio", null);
    }
  }

  private static OAuth2UserRequest githubRequest() {
    final ClientRegistration registration = ClientRegistration.withRegistrationId("github")
        .clientId("id")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost/login/oauth2/code/github")
        .authorizationUri("https://github.com/login/oauth/authorize")
        .tokenUri("https://github.com/login/oauth/access_token")
        .userInfoUri("https://api.github.com/user")
        .userNameAttributeName("login")
        .build();
    final OAuth2AccessToken token = new OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER, "token", null, null);
    return new OAuth2UserRequest(registration, token);
  }
}
