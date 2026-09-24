package com.example.onlinejava;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Helpers for redirecting from old URLs.
 */
public final class Redirects {

  private Redirects() {
  }

  /**
   * Builds a 301 redirect, so browsers and search engines update old links.
   *
   * @param path target path, e.g. {@code /problems/sorting/bubble-sort}
   * @return a view that answers with 301 Moved Permanently
   */
  public static ModelAndView permanent(final String path) {
    final RedirectView view = new RedirectView(path, true);
    view.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
    view.setExposeModelAttributes(false);
    return new ModelAndView(view);
  }
}
