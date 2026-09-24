package com.example.onlinejava.discussion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.security.DevSecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link DiscussionPageController}.
 */
@WebMvcTest(DiscussionPageController.class)
@Import({ProblemRegistry.class, DevSecurityConfig.class})
@ActiveProfiles("dev")
class DiscussionPageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void rendersDiscussionPageForRegisteredProblem() throws Exception {
    mockMvc.perform(get("/problems/sorting/bubble-sort/discussion"))
        .andExpect(status().isOk())
        .andExpect(view().name("discussion"))
        .andExpect(model().attributeExists("problem"))
        .andExpect(content().string(Matchers.containsString("data-problem-slug=\"bubble-sort\"")));
  }

  @Test
  void returns404ForUnknownProblem() throws Exception {
    mockMvc.perform(get("/problems/sorting/nope/discussion"))
        .andExpect(status().isNotFound());
  }

  @Test
  void oldUrlRedirectsPermanentlyToCanonicalDiscussion() throws Exception {
    mockMvc.perform(get("/problems/arrays/bubble-sort/discussion"))
        .andExpect(status().isMovedPermanently())
        .andExpect(redirectedUrl("/problems/sorting/bubble-sort/discussion"));
  }
}
