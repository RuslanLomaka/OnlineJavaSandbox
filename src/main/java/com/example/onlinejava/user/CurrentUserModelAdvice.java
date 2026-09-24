package com.example.onlinejava.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the signed-in user's name to every page model as {@code username},
 * which the shared navbar fragment displays.
 */
@ControllerAdvice(annotations = Controller.class)
public class CurrentUserModelAdvice {

  /**
   * Returns a display name for the current principal.
   *
   * @param user the authenticated user, or {@code null} when anonymous
   * @return the name to show, or {@code null} when nobody is signed in
   */
  @ModelAttribute("username")
  public String username(@AuthenticationPrincipal final OAuth2User user) {
    return displayName(user);
  }

  /**
   * Provider-agnostic display name: prefers the application's own user
   * record, then common provider attributes.
   *
   * @param user an OAuth2 principal, may be {@code null}
   * @return the login/handle to display, or {@code null}
   */
  public static String displayName(final OAuth2User user) {
    if (user == null) {
      return null;
    }
    if (user instanceof AppUserPrincipal principal) {
      return principal.getLogin();
    }
    final Object login = user.getAttribute("login");
    return login != null ? login.toString() : user.getName();
  }
}
