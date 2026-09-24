package com.example.onlinejava;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
 * Web-layer tests for {@link PageController}: overview, topic pages, problem
 * pages, and permanent redirects from the old category URLs.
 */
@WebMvcTest(PageController.class)
@Import({ProblemRegistry.class, DevSecurityConfig.class})
@ActiveProfiles("dev")
class PageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void rendersProblemAtCanonicalUrl() throws Exception {
    mockMvc.perform(get("/problems/sorting/bubble-sort"))
        .andExpect(status().isOk())
        .andExpect(view().name("problem"))
        .andExpect(model().attributeExists("problem"));
  }

  @Test
  void oldCategoryUrlsRedirectPermanentlyToCanonicalUrl() throws Exception {
    mockMvc.perform(get("/problems/arrays/bubble-sort"))
        .andExpect(status().isMovedPermanently())
        .andExpect(redirectedUrl("/problems/sorting/bubble-sort"));
    mockMvc.perform(get("/problems/collections/longest-unique-substring"))
        .andExpect(status().isMovedPermanently())
        .andExpect(redirectedUrl("/problems/sliding-window/longest-unique-substring"));
  }

  @Test
  void unknownProblemIs404() throws Exception {
    mockMvc.perform(get("/problems/sorting/does-not-exist"))
        .andExpect(status().isNotFound());
  }

  @Test
  void overviewListsBothSections() throws Exception {
    mockMvc.perform(get("/problems"))
        .andExpect(status().isOk())
        .andExpect(view().name("problems"))
        .andExpect(content().string(containsString("Data Structures")))
        .andExpect(content().string(containsString("Algorithms")))
        .andExpect(content().string(containsString("/problems/sorting/bubble-sort")));
  }

  @Test
  void topicPageListsItsProblems() throws Exception {
    mockMvc.perform(get("/problems/sliding-window"))
        .andExpect(status().isOk())
        .andExpect(view().name("topic"))
        .andExpect(content().string(containsString("Longest Substring")));
  }

  @Test
  void emptyTopicPageSaysComingSoon() throws Exception {
    mockMvc.perform(get("/problems/trees"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Coming soon")));
  }

  @Test
  void oldSectionPagesRedirectToOverview() throws Exception {
    mockMvc.perform(get("/problems/collections"))
        .andExpect(status().isMovedPermanently())
        .andExpect(redirectedUrl("/problems"));
    mockMvc.perform(get("/problems/algorithms"))
        .andExpect(status().isMovedPermanently())
        .andExpect(redirectedUrl("/problems"));
  }

  @Test
  void unknownTopicIs404() throws Exception {
    mockMvc.perform(get("/problems/no-such-topic"))
        .andExpect(status().isNotFound());
  }

  @Test
  void navbarShowsBothMenusOnEveryPage() throws Exception {
    mockMvc.perform(get("/sandbox"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Data Structures")))
        .andExpect(content().string(containsString("href=\"/problems/hash-maps-and-sets\"")));
  }
}
