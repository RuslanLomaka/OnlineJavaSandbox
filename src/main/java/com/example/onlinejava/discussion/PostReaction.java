package com.example.onlinejava.discussion;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user's reaction (e.g. 👍) on a discussion post.
 */
@Entity
@Table(name = "post_reaction")
public class PostReaction {

  @EmbeddedId
  private PostReactionId id;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Required by JPA.
   */
  protected PostReaction() {
  }

  /**
   * Creates a reaction.
   *
   * @param id composite key
   * @param now creation time
   */
  public PostReaction(final PostReactionId id, final Instant now) {
    this.id = id;
    this.createdAt = now;
  }

  /**
   * Returns the composite key.
   *
   * @return key
   */
  public PostReactionId getId() {
    return id;
  }
}
