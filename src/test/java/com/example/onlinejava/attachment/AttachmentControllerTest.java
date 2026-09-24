package com.example.onlinejava.attachment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.security.SecurityConfig;
import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserLoginService;
import com.example.onlinejava.user.AppUserPrincipal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
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
 * Web-layer tests for {@link AttachmentController} under production security.
 */
@WebMvcTest(AttachmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class AttachmentControllerTest {

  private static final long USER_ID = 3L;

  private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AttachmentService service;

  @MockitoBean
  private AppUserLoginService appUserLoginService;

  @Test
  void servesImageWithHardenedHeaders() throws Exception {
    final byte[] png = TestImages.png(2, 2);
    when(service.load(ID)).thenReturn(
        new Attachment(ID, USER_ID, "image/png", 2, 2, png, Instant.now()));

    mockMvc.perform(get("/attachments/" + ID).with(user()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(png))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"))
        .andExpect(header().string("Content-Disposition", "inline; filename=\"screenshot.png\""))
        .andExpect(header().string("Cache-Control", "max-age=31536000, private, immutable"));
  }

  @Test
  void unknownAttachmentIs404() throws Exception {
    when(service.load(ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(get("/attachments/" + ID).with(user()))
        .andExpect(status().isNotFound());
  }

  @Test
  void malformedIdIs400() throws Exception {
    mockMvc.perform(get("/attachments/not-a-uuid").with(user()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void anonymousUploadIs401() throws Exception {
    mockMvc.perform(multipart("/api/attachments").file(file()).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void uploadWithoutCsrfIs403() throws Exception {
    mockMvc.perform(multipart("/api/attachments").file(file()).with(user()))
        .andExpect(status().isForbidden());

    verify(service, never()).upload(anyLong(), any());
  }

  @Test
  void uploadReturnsMarkdownSnippet() throws Exception {
    when(service.upload(eq(USER_ID), any())).thenReturn(AttachmentView.of(ID));

    mockMvc.perform(multipart("/api/attachments").file(file()).with(user()).with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.url").value("/attachments/" + ID))
        .andExpect(jsonPath("$.markdown").value("![screenshot](/attachments/" + ID + ")"));
  }

  private static MockMultipartFile file() {
    return new MockMultipartFile("file", "shot.png", "image/png", TestImages.png(2, 2));
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
