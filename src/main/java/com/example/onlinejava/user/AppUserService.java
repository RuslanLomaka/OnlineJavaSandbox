package com.example.onlinejava.user;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates or refreshes {@link AppUser} rows when someone signs in.
 */
@Service
public class AppUserService {

  private final AppUserRepository repository;

  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository user repository
   * @param clock source of the current time
   */
  public AppUserService(final AppUserRepository repository, final Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /**
   * Inserts the user on first login, or updates their profile snapshot and
   * last-login time on subsequent logins.
   *
   * @param info normalized provider profile
   * @return the persisted user
   */
  @Transactional
  public AppUser upsert(final ProviderProfile info) {
    final Instant now = clock.instant();
    return repository
        .findByProviderAndProviderUserId(info.provider(), info.providerUserId())
        .map(existing -> {
          existing.recordLogin(info.login(), info.displayName(), info.avatarUrl(), now);
          return existing;
        })
        .orElseGet(() -> repository.save(new AppUser(
            info.provider(),
            info.providerUserId(),
            info.login(),
            info.displayName(),
            info.avatarUrl(),
            now
        )));
  }
}
