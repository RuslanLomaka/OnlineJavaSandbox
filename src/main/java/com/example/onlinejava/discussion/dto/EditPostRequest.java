package com.example.onlinejava.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PATCH /api/posts/{id}}.
 *
 * @param body new Markdown body
 */
public record EditPostRequest(
    @NotBlank @Size(max = CreatePostRequest.MAX_BODY_LENGTH) String body
) {
}
