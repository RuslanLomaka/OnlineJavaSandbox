package com.example.onlinejava;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlinejava.security.DevSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link LoginController}.
 */
@WebMvcTest(LoginController.class)
@Import(DevSecurityConfig.class)
@ActiveProfiles("dev")
class LoginControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void neverServesUserControlledNameAsHtml() throws Exception {
    mockMvc.perform(get("/user")
            .accept(MediaType.TEXT_HTML, MediaType.ALL)
            .with(oauth2Login().attributes(a -> a.put("login", "<script>alert(1)</script>"))))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string(
            "Logged in as: &lt;script&gt;alert(1)&lt;/script&gt;"));
  }
}
