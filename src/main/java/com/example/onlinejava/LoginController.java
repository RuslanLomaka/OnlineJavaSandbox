package com.example.onlinejava;

import com.example.onlinejava.user.CurrentUserModelAdvice;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * REST controller exposing information about the currently authenticated user.
 */
@RestController
public class LoginController {

  /**
   * Returns a greeting containing the authenticated user's login name.
   *
   * @param user the authenticated OAuth2 user
   * @return a message identifying the logged-in user
   */
  // Always plain text, and escaped as well: the name comes from the OAuth
  // provider, so it must never be interpreted as HTML.
  @GetMapping(value = "/user", produces = MediaType.TEXT_PLAIN_VALUE)
  public String user(
      @AuthenticationPrincipal OAuth2User user
  ) {
    return "Logged in as: "
        + HtmlUtils.htmlEscape(String.valueOf(CurrentUserModelAdvice.displayName(user)));
  }
}
