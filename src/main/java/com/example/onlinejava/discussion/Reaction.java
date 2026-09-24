package com.example.onlinejava.discussion;

import java.util.Arrays;
import java.util.Locale;

/**
 * The fixed set of reactions a post can receive. Using a closed set (rather
 * than arbitrary emoji) keeps storage and validation trivial.
 */
public enum Reaction {
  THUMBS_UP("👍"),
  HEART("❤️"),
  TADA("🎉"),
  SMILE("😄"),
  THINKING("🤔"),
  ROCKET("🚀");

  private final String emoji;

  Reaction(final String emoji) {
    this.emoji = emoji;
  }

  /**
   * Returns the emoji shown for this reaction.
   *
   * @return emoji character(s)
   */
  public String emoji() {
    return emoji;
  }

  /**
   * Returns the URL/JSON code, e.g. {@code thumbs_up}.
   *
   * @return lower-case code
   */
  public String code() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Parses a reaction code.
   *
   * @param code lower-case code such as {@code rocket}
   * @return the matching reaction
   * @throws IllegalArgumentException if the code is unknown
   */
  public static Reaction fromCode(final String code) {
    return Arrays.stream(values())
        .filter(r -> r.code().equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown reaction: " + code));
  }
}
