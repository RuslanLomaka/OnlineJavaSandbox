package com.example.onlinejava.editor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.security.SecurityConfig;
import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserLoginService;
import com.example.onlinejava.user.AppUserPrincipal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web-layer tests for {@link EditorController} under production security.
 */
@WebMvcTest(EditorController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class EditorControllerTest {

  private static final long USER_ID = 9L;

  private static final String BODY =
      "{\"code\":\"int x=1;\",\"kind\":\"METHOD_BODY\",\"organizeImports\":false}";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CodeFormattingService service;

  @MockitoBean
  private AppUserLoginService appUserLoginService;

  @Test
  void anonymousGets401() throws Exception {
    mockMvc.perform(post("/api/editor/format").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingCsrfGets403() throws Exception {
    mockMvc.perform(post("/api/editor/format").with(user())
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isForbidden());
    verify(service, never()).format(anyLong(), any());
  }

  @Test
  void returnsFormattedCode() throws Exception {
    when(service.format(eq(USER_ID), any())).thenReturn(new FormatResponse("int x = 1;\n", true));

    mockMvc.perform(post("/api/editor/format").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("int x = 1;\n"))
        .andExpect(jsonPath("$.changed").value(true));
    verify(service).format(USER_ID, new FormatRequest("int x=1;", SourceKind.METHOD_BODY, false));
  }

  @Test
  void rejectsOversizedCodeAndMissingKind() throws Exception {
    final String huge = "x".repeat(FormatRequest.MAX_CODE_LENGTH + 1);
    mockMvc.perform(post("/api/editor/format").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + huge + "\",\"kind\":\"CLASS\"}"))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/editor/format").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"x\"}"))
        .andExpect(status().isBadRequest());
    verify(service, never()).format(anyLong(), any());
  }

  @Test
  void syntaxErrorsBecome422WithTheMessage() throws Exception {
    when(service.format(eq(USER_ID), any()))
        .thenThrow(new UnformattableCodeException("Line 3: Syntax error"));

    mockMvc.perform(post("/api/editor/format").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.detail").value("Line 3: Syntax error"));
  }

  @Test
  void rateLimitBecomes429() throws Exception {
    when(service.format(eq(USER_ID), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));

    mockMvc.perform(post("/api/editor/format").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isTooManyRequests());
  }

  private static RequestPostProcessor user() {
    final AppUser appUser = new AppUser("github", "1", "alice", "Alice", null, Instant.now());
    ReflectionTestUtils.setField(appUser, "id", USER_ID);
    final AppUserPrincipal principal = new AppUserPrincipal(appUser, Map.of("login", "alice"),
        AuthorityUtils.createAuthorityList("OAUTH2_USER"));
    return authentication(
        new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github"));
  }
}
