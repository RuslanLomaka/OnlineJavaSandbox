package com.example.onlinejava.discussion.dto;

/**
 * Public profile of a post's author.
 *
 * @param login username/handle
 * @param displayName human-readable name
 * @param avatarUrl HTTPS avatar URL, or {@code null}
 */
public record AuthorView(String login, String displayName, String avatarUrl) {
}
