package com.example.onlinejava.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.onlinejava.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link AppUserService} against PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, AppUserService.class,
    AppUserServiceTest.FixedClock.class})
class AppUserServiceTest {

  @Autowired
  private AppUserService service;

  @Autowired
  private AppUserRepository repository;

  @Test
  void firstLoginCreatesUser() {
    final AppUser user = service.upsert(
        new ProviderProfile("github", "42", "octocat", "Octo", "https://a/42"));

    assertThat(user.getId()).isNotNull();
    assertThat(user.getLogin()).isEqualTo("octocat");
    assertThat(user.getCreatedAt()).isEqualTo(FixedClock.NOW);
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void laterLoginUpdatesProfileButKeepsIdentity() {
    final AppUser first = service.upsert(
        new ProviderProfile("github", "42", "octocat", "Octo", null));

    final AppUser second = service.upsert(
        new ProviderProfile("github", "42", "renamed", "New Name", "https://a/new"));

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getLogin()).isEqualTo("renamed");
    assertThat(second.getDisplayName()).isEqualTo("New Name");
    assertThat(second.getAvatarUrl()).isEqualTo("https://a/new");
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void sameIdFromDifferentProvidersAreDifferentUsers() {
    final AppUser github = service.upsert(new ProviderProfile("github", "1", "a", "A", null));
    final AppUser dev = service.upsert(new ProviderProfile("dev", "1", "b", "B", null));

    assertThat(github.getId()).isNotEqualTo(dev.getId());
  }

  /**
   * Supplies a fixed clock so timestamps are deterministic.
   */
  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClock {
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
