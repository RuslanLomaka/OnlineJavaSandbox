package com.example.onlinejava.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.PageController;
import com.example.onlinejava.problem.ProblemRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks that the dev profile uses the same Content-Security-Policy as
 * production, so CSP problems show up during local development.
 */
@WebMvcTest(PageController.class)
@Import({ProblemRegistry.class, DevSecurityConfig.class})
@ActiveProfiles("dev")
class DevSecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void devProfileSendsProductionContentSecurityPolicy() throws Exception {
    mockMvc.perform(get("/sandbox"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Security-Policy",
            SecurityHeaders.CONTENT_SECURITY_POLICY));
  }
}
