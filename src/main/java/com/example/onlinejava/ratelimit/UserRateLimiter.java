package com.example.onlinejava.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Token-bucket rate limits per user and action (Bucket4j), so one account
 * can't flood discussions or fill the database with uploads.
 *
 * <p>Buckets live in memory, which fits the single-instance deployment; idle
 * buckets are evicted after an hour. Moving to several instances would mean
 * switching to a Bucket4j distributed backend.
 */
@Component
public class UserRateLimiter {

  /**
   * Rate-limited actions and their budgets.
   */
  public enum Action {
    /** Creating a post or reply. */
    POST(5, Duration.ofMinutes(1)),
    /** Editing or deleting one of the user's own posts. */
    EDIT_OR_DELETE(10, Duration.ofMinutes(1)),
    /** Adding or removing a reaction. */
    REACTION(30, Duration.ofMinutes(1)),
    /** Uploading a screenshot. */
    UPLOAD(10, Duration.ofMinutes(10));

    private final int capacity;

    private final Duration period;

    Action(final int capacity, final Duration period) {
      this.capacity = capacity;
      this.period = period;
    }

    /**
     * Returns how many times the action may run per period.
     *
     * @return capacity
     */
    public int capacity() {
      return capacity;
    }
  }

  private record Key(long userId, Action action) {
  }

  private final Cache<Key, Bucket> buckets = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofHours(1))
      .maximumSize(100_000)
      .build();

  /**
   * Consumes one unit of the user's budget for an action.
   *
   * @param userId app user id
   * @param action action being performed
   * @throws ResponseStatusException 429 if the budget is exhausted
   */
  public void check(final long userId, final Action action) {
    final Bucket bucket = buckets.get(new Key(userId, action), key -> Bucket.builder()
        .addLimit(limit -> limit
            .capacity(action.capacity)
            .refillGreedy(action.capacity, action.period))
        .build());
    if (!bucket.tryConsume(1)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
          "You're doing that too often. Please wait a moment and try again.");
    }
  }
}
