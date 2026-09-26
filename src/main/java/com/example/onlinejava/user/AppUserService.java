package com.example.onlinejava.user;

import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
   * <p>Not itself wrapped in one transaction: the find and the eventual
   * save each get their own via {@link AppUserRepository}'s own per-method
   * transactionality, which is what lets {@link #insertOrRecordLogin} run
   * a clean retry after a failed insert -- see its Javadoc.
   *
   * @param info normalized provider profile
   * @return the persisted user
   */
  public AppUser upsert(final ProviderProfile info) {
    return repository
        .findByProviderAndProviderUserId(info.provider(), info.providerUserId())
        .map(existing -> recordLogin(existing, info))
        .orElseGet(() -> insertOrRecordLogin(info));
  }

  /**
   * Inserts a brand-new user, or falls back to recording a login if a
   * concurrent first login for the same identity won the race first.
   *
   * <p>Two logins for a user who has never signed in before can both miss
   * the {@code findBy} lookup in {@link #upsert} (neither row exists yet)
   * and both reach here. The database's unique constraint on
   * {@code (provider, provider_user_id)} -- not this code -- is what
   * actually prevents a duplicate row: the loser's {@code save} fails
   * with {@link DataIntegrityViolationException}. Rather than let that
   * fail the login outright, the loser looks the row up again (the
   * winner's insert already committed in its own transaction by the time
   * this one aborts) and records its login against that instead.
   *
   * @param info normalized provider profile
   * @return the persisted user
   */
  private AppUser insertOrRecordLogin(final ProviderProfile info) {
    try {
      return repository.save(new AppUser(
          info.provider(),
          info.providerUserId(),
          info.login(),
          info.displayName(),
          info.avatarUrl(),
          clock.instant()
      ));
    } catch (DataIntegrityViolationException exception) {
      return repository
          .findByProviderAndProviderUserId(info.provider(), info.providerUserId())
          .map(existing -> recordLogin(existing, info))
          .orElseThrow(() -> exception);
    }
  }

  private AppUser recordLogin(final AppUser existing, final ProviderProfile info) {
    existing.recordLogin(info.login(), info.displayName(), info.avatarUrl(), clock.instant());
    return repository.save(existing);
  }
}
