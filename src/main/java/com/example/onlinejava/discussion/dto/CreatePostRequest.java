package com.example.onlinejava.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/problems/{slug}/posts}.
 *
 * @param body Markdown body
 * @param replyToId id of the post being answered, or {@code null} to start a
 *     new thread
 */
public record CreatePostRequest(
    @NotBlank @Size(max = CreatePostRequest.MAX_BODY_LENGTH) String body,
    @Positive Long replyToId
) {

  /** Maximum body length in characters. */
  public static final int MAX_BODY_LENGTH = 10_000;
}
