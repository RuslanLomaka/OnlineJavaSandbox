package com.example.onlinejava.sandbox;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.security.DevSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link SandboxController}; the RabbitMQ gateway is mocked.
 */
@WebMvcTest(SandboxController.class)
@Import({ProblemRegistry.class, DevSecurityConfig.class})
@ActiveProfiles("dev")
class SandboxControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SandboxExecutionGateway gateway;

  @Test
  void runForwardsSourceToGateway() throws Exception {
    when(gateway.execute("class Main {}")).thenReturn("ok");

    mockMvc.perform(post("/sandbox/run")
            .contentType(MediaType.TEXT_PLAIN)
            .content("class Main {}"))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  @Test
  void runProblemWrapsSolutionInTestHarness() throws Exception {
    when(gateway.execute(anyString())).thenReturn("All tests passed");

    mockMvc.perform(post("/problems/bubble-sort/run")
            .contentType(MediaType.TEXT_PLAIN)
            .content("// my solution"))
        .andExpect(status().isOk())
        .andExpect(content().string("All tests passed"));

    verify(gateway).execute(argThat(source ->
        source.contains("// my solution") && source.contains("public class Main")));
  }

  @Test
  void runProblemReturns404ForUnknownSlug() throws Exception {
    mockMvc.perform(post("/problems/nope/run")
            .contentType(MediaType.TEXT_PLAIN)
            .content("x"))
        .andExpect(status().isNotFound());

    verify(gateway, never()).execute(anyString());
  }
}
