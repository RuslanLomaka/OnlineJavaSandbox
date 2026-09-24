package com.example.onlinejava;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.security.DevSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link PageController}.
 */
@WebMvcTest(PageController.class)
@Import({ProblemRegistry.class, DevSecurityConfig.class})
@ActiveProfiles("dev")
class PageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void rendersRegisteredProblem() throws Exception {
    mockMvc.perform(get("/problems/arrays/bubble-sort"))
        .andExpect(status().isOk())
        .andExpect(view().name("problem"))
        .andExpect(model().attributeExists("problem"));
  }

  @Test
  void returns404ForUnknownSlug() throws Exception {
    mockMvc.perform(get("/problems/arrays/does-not-exist"))
        .andExpect(status().isNotFound());
  }

  @Test
  void returns404WhenCategoryDoesNotMatch() throws Exception {
    mockMvc.perform(get("/problems/collections/bubble-sort"))
        .andExpect(status().isNotFound());
  }

  @Test
  void arraysListingContainsArrayProblems() throws Exception {
    mockMvc.perform(get("/problems/arrays"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("problems"));
  }
}
