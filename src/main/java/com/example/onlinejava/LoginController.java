package com.example.onlinejava;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
  @GetMapping("/user")
  public String user(
      @AuthenticationPrincipal OAuth2User user
  ) {
    return "Logged in as: " + user.getAttribute("login");
  }
}
