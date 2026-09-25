package com.example.onlinejava.security;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.PageController;
import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.sandbox.SandboxController;
import com.example.onlinejava.sandbox.SandboxExecutionGateway;
import com.example.onlinejava.user.AppUserLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the production {@link SecurityConfig} rules: which routes are
 * public, that everything else requires an OAuth2 login, and CSRF handling.
 */
@WebMvcTest({PageController.class, SandboxController.class})
@Import({ProblemRegistry.class, SecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SandboxExecutionGateway gateway;

  @MockitoBean
  private AppUserLoginService appUserLoginService;

  @Test
  void anonymousUserIsRedirectedToLogin() throws Exception {
    mockMvc.perform(get("/sandbox"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/oauth2/authorization/github"));
  }

  @Test
  void anonymousUserCannotReachProblemPages() throws Exception {
    mockMvc.perform(get("/problems/arrays/bubble-sort"))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void loggedInUserCanReachSandbox() throws Exception {
    mockMvc.perform(get("/sandbox").with(oauth2Login()))
        .andExpect(status().isOk());
  }

  @Test
  void sandboxRunIsExemptFromCsrf() throws Exception {
    mockMvc.perform(post("/sandbox/run")
            .with(oauth2Login())
            .contentType(MediaType.TEXT_PLAIN)
            .content("x"))
        .andExpect(status().isOk());
  }

  @Test
  void problemRunRequiresCsrfToken() throws Exception {
    mockMvc.perform(post("/problems/bubble-sort/run")
            .with(oauth2Login())
            .contentType(MediaType.TEXT_PLAIN)
            .content("x"))
        .andExpect(status().isForbidden());
  }

  @Test
  void problemRunSucceedsWithCsrfToken() throws Exception {
    mockMvc.perform(post("/problems/bubble-sort/run")
            .with(oauth2Login())
            .with(csrf())
            .contentType(MediaType.TEXT_PLAIN)
            .content("x"))
        .andExpect(status().isOk());
  }

  @Test
  void pagesSendContentSecurityPolicy() throws Exception {
    mockMvc.perform(get("/sandbox").with(oauth2Login()))
        .andExpect(header().string("Content-Security-Policy",
            allOf(
                containsString(
                    "script-src 'self' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net"),
                containsString("object-src 'none'"),
                containsString("frame-ancestors 'none'"),
                containsString("connect-src 'self'"),
                containsString("worker-src 'self'"))))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
  }
}
