package com.example.onlinejava.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit tests for {@link AppUserService}'s recovery from a concurrent
 * first-login race, using a mocked repository -- no Docker needed, unlike
 * {@link AppUserServiceTest}'s Testcontainers-backed suite.
 */
class AppUserServiceUpsertConflictTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private final AppUserRepository repository = mock(AppUserRepository.class);

  private final AppUserService service =
      new AppUserService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void recoversWhenConcurrentFirstLoginWinsTheInsertRace() {
    final ProviderProfile info = new ProviderProfile("github", "42", "octocat", "Octo", null);
    final AppUser winnersRow = new AppUser("github", "42", "octocat", "Octo", null, NOW);

    // Both logins miss the lookup (neither row exists yet); this one loses
    // the insert race, so by the time it looks again, the winner's row is
    // there.
    when(repository.findByProviderAndProviderUserId("github", "42"))
        .thenReturn(Optional.empty(), Optional.of(winnersRow));
    when(repository.save(any(AppUser.class)))
        .thenThrow(new DataIntegrityViolationException("uq_app_user_provider_identity"))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final AppUser result = service.upsert(info);

    assertThat(result).isSameAs(winnersRow);
    verify(repository, times(2)).findByProviderAndProviderUserId("github", "42");
  }

  @Test
  void stillFailsIfTheConflictWasNotActuallyAnInsertRace() {
    // A constraint violation with no matching row afterward means something
    // else is wrong (a different constraint, a real DB problem) -- this
    // must not be swallowed as if it were the race it's designed to recover
    // from.
    final ProviderProfile info = new ProviderProfile("github", "42", "octocat", "Octo", null);
    final DataIntegrityViolationException failure =
        new DataIntegrityViolationException("some other constraint");

    when(repository.findByProviderAndProviderUserId("github", "42"))
        .thenReturn(Optional.empty());
    when(repository.save(any(AppUser.class))).thenThrow(failure);

    assertThatThrownBy(() -> service.upsert(info)).isSameAs(failure);
  }
}
