package com.example.onlinejava.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Response security headers shared by the production and dev filter chains.
 */
public final class SecurityHeaders {

  /**
   * Content-Security-Policy for every page.
   *
   * <ul>
   *   <li>Scripts only from this site and the two pinned CDNs (all CDN
   *       scripts also carry SRI hashes); no inline scripts or eval.</li>
   *   <li>Inline styles are allowed because CodeMirror, EasyMDE and the
   *       emoji picker set styles at runtime.</li>
   *   <li>Fonts from this site, cdnjs, and {@code data:} URLs (the code
   *       editor's icon font, e.g. the fold arrows, is embedded in its CSS).</li>
   *   <li>Images only from this site (screenshots) and GitHub avatars.</li>
   *   <li>{@code fetch()} only to this site.</li>
   *   <li>Web workers (the code editor's) only from this site.</li>
   *   <li>No plugins, no framing, forms only post back to this site.</li>
   * </ul>
   */
  public static final String CONTENT_SECURITY_POLICY = String.join("; ",
      "default-src 'self'",
      "script-src 'self' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net",
      "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net",
      "font-src 'self' data: https://cdnjs.cloudflare.com",
      "img-src 'self' data: blob: https://avatars.githubusercontent.com",
      "connect-src 'self'",
      "worker-src 'self'",
      "object-src 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'");

  private SecurityHeaders() {
  }

  /**
   * Applies the shared headers: CSP, a referrer policy that doesn't leak full
   * URLs to other sites, and denying framing.
   *
   * @param http the security configuration builder
   */
  public static void apply(final HttpSecurity http) {
    http.headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
        .referrerPolicy(referrer -> referrer.policy(
            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));
  }
}
