package com.example.onlinejava.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserPrincipal;
import com.example.onlinejava.user.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link DevUserAuthenticationFilter}.
 */
class DevUserAuthenticationFilterTest {

  private final AppUserService appUserService = mock(AppUserService.class);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void refusesToStartWhenServerIsNotBoundToLoopback() {
    assertThatThrownBy(() -> new DevUserAuthenticationFilter(appUserService, "0.0.0.0"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new DevUserAuthenticationFilter(appUserService, ""))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void authenticatesRequestAsLocalDevUser() throws Exception {
    final AppUser devUser = new AppUser("dev", "local-dev", "local-dev", "Local developer",
        null, null);
    ReflectionTestUtils.setField(devUser, "id", 1L);
    when(appUserService.upsert(any())).thenReturn(devUser);
    final DevUserAuthenticationFilter filter =
        new DevUserAuthenticationFilter(appUserService, "127.0.0.1");
    final Authentication[] seen = new Authentication[1];

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
        new MockFilterChain() {
          @Override
          public void doFilter(jakarta.servlet.ServletRequest request,
                               jakarta.servlet.ServletResponse response) {
            seen[0] = SecurityContextHolder.getContext().getAuthentication();
          }
        });
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
        new MockFilterChain());

    assertThat(seen[0].getPrincipal()).isInstanceOf(AppUserPrincipal.class);
    assertThat(((AppUserPrincipal) seen[0].getPrincipal()).getUserId()).isEqualTo(1L);
    verify(appUserService, times(1)).upsert(any());
  }
}
