package com.example.onlinejava.discussion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.discussion.dto.AuthorView;
import com.example.onlinejava.discussion.dto.CreatePostRequest;
import com.example.onlinejava.discussion.dto.PostView;
import com.example.onlinejava.security.SecurityConfig;
import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserLoginService;
import com.example.onlinejava.user.AppUserPrincipal;
import java.time.Instant;
import java.util.List;
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
 * Web-layer tests for {@link DiscussionApiController} under the production
 * security rules: authentication, CSRF, validation and error mapping.
 */
@WebMvcTest(DiscussionApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class DiscussionApiControllerTest {

  private static final long USER_ID = 7L;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DiscussionService service;

  @MockitoBean
  private AppUserLoginService appUserLoginService;

  @Test
  void anonymousApiCallsGet401InsteadOfRedirect() throws Exception {
    mockMvc.perform(get("/api/problems/bubble-sort/threads"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/problems/bubble-sort/posts").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"x\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void postingWithoutCsrfTokenIsForbidden() throws Exception {
    mockMvc.perform(post("/api/problems/bubble-sort/posts").with(user())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"x\"}"))
        .andExpect(status().isForbidden());

    verify(service, never()).createPost(any(), anyLong(), any());
  }

  @Test
  void createsPostForSignedInUser() throws Exception {
    when(service.createPost(eq("bubble-sort"), eq(USER_ID), any())).thenReturn(samplePost());

    mockMvc.perform(post("/api/problems/bubble-sort/posts").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hello\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.author.login").value("alice"));

    verify(service).createPost("bubble-sort", USER_ID, new CreatePostRequest("hello", null));
  }

  @Test
  void rejectsBlankAndOversizedBodies() throws Exception {
    mockMvc.perform(post("/api/problems/bubble-sort/posts").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
        .andExpect(status().isBadRequest());

    final String huge = "a".repeat(CreatePostRequest.MAX_BODY_LENGTH + 1);
    mockMvc.perform(post("/api/problems/bubble-sort/posts").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + huge + "\"}"))
        .andExpect(status().isBadRequest());

    verify(service, never()).createPost(any(), anyLong(), any());
  }

  @Test
  void serviceForbiddenBecomes403() throws Exception {
    when(service.editPost(1L, USER_ID, "x"))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your post"));

    mockMvc.perform(patch("/api/posts/1").with(user()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"x\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteReturns204() throws Exception {
    mockMvc.perform(delete("/api/posts/1").with(user()).with(csrf()))
        .andExpect(status().isNoContent());

    verify(service).deletePost(1L, USER_ID);
  }

  @Test
  void knownReactionIsAccepted() throws Exception {
    when(service.addReaction(1L, USER_ID, Reaction.ROCKET)).thenReturn(List.of());

    mockMvc.perform(put("/api/posts/1/reactions/rocket").with(user()).with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void unknownReactionIsBadRequest() throws Exception {
    mockMvc.perform(put("/api/posts/1/reactions/poop").with(user()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private static RequestPostProcessor user() {
    final AppUser appUser = new AppUser("github", "1", "alice", "Alice", null, Instant.now());
    ReflectionTestUtils.setField(appUser, "id", USER_ID);
    final AppUserPrincipal principal = new AppUserPrincipal(appUser, Map.of("login", "alice"),
        AuthorityUtils.createAuthorityList("OAUTH2_USER"));
    return authentication(
        new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github"));
  }

  private static PostView samplePost() {
    return new PostView(1L, new AuthorView("alice", "Alice", null), "<p>hello</p>",
        Instant.now(), null, false, true, null, List.of());
  }
}
