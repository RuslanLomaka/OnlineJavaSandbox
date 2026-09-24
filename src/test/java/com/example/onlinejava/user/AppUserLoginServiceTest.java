package com.example.onlinejava.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
