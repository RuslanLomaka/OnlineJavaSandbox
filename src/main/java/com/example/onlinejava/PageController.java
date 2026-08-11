package com.example.onlinejava;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller that serves the application's HTML pages: the sandbox, the
 * problem category pages, and individual problem pages.
 */
@Controller
public class PageController {

  /**
   * Renders the sandbox page, attaching the authenticated user's login
   * name (or a fallback for local development) to the view model.
   *
   * @param user  the authenticated OAuth2 user, or {@code null} if not logged in
   * @param model the view model to populate
   * @return the sandbox view name
   */
  @GetMapping("/sandbox")
  public String sandbox(
      @AuthenticationPrincipal OAuth2User user,
      Model model
  ) {
    String username;

    if (user == null) {
      username = "Local developer";
    } else {
      username = user.getAttribute("login");
    }

    model.addAttribute("username", username);

    return "sandbox";
  }

  @GetMapping("/problems")
  public String problems() {
    return "problems";
  }

  @GetMapping("/problems/arrays")
  public String arrays() {
    return "problems-arrays";
  }

  @GetMapping("/problems/collections")
  public String collections() {
    return "problems-collections";
  }

  @GetMapping("/problems/algorithms")
  public String algorithms() {
    return "problems-algorithms";
  }

  @GetMapping("/problems/arrays/bubble-sort")
  public String bubbleSort() {
    return "arrays/bubble-sort";
  }

  @GetMapping(
      "/problems/collections/longest-unique-substring"
  )
  public String longestUniqueSubstring() {
    return "collections/longest-unique-substring";
  }

}