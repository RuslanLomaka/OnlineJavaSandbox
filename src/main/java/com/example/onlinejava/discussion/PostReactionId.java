package com.example.onlinejava.discussion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serial;
import java.io.Serializable;

/**
 * Composite key of {@link PostReaction}: one reaction type per user per post.
 *
 * @param postId reacted-to post
 * @param userId reacting user
 * @param reaction reaction type
 */
@Embeddable
public record PostReactionId(
    @Column(name = "post_id") Long postId,
    @Column(name = "user_id") Long userId,
    @Enumerated(EnumType.STRING) @Column(name = "reaction") Reaction reaction
) implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;
}
