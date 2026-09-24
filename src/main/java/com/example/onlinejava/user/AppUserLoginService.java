package com.example.onlinejava.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Loads the user from the OAuth2 provider, then records the login in the
 * database and returns an {@link AppUserPrincipal}.
 *
 * <p>Plugged into Spring Security via
 * {@code oauth2Login().userInfoEndpoint().userService(...)}.
 */
@Service
public class AppUserLoginService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final AppUserService appUserService;

  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

  /**
   * Creates the service using Spring Security's default user-info client.
   *
   * @param appUserService upserts users
   */
  @Autowired
  public AppUserLoginService(final AppUserService appUserService) {
    this(appUserService, new DefaultOAuth2UserService());
  }

  /**
   * Creates the service with an explicit delegate (used by tests).
   *
   * @param appUserService upserts users
   * @param delegate fetches the raw user from the provider
   */
  AppUserLoginService(
      final AppUserService appUserService,
      final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate
  ) {
    this.appUserService = appUserService;
    this.delegate = delegate;
  }

  @Override
  public OAuth2User loadUser(final OAuth2UserRequest request)
      throws OAuth2AuthenticationException {
    final OAuth2User providerUser = delegate.loadUser(request);
    final String provider = request.getClientRegistration().getRegistrationId();
    final ProviderProfile info = ProviderProfile.from(provider, providerUser.getAttributes());
    final AppUser user = appUserService.upsert(info);
    return new AppUserPrincipal(user, providerUser.getAttributes(),
        providerUser.getAuthorities());
  }
}
