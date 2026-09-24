package com.example.onlinejava.discussion.dto;

/**
 * Reaction count on a post.
 *
 * @param code reaction code used in URLs, e.g. {@code thumbs_up}
 * @param emoji emoji to display
 * @param count number of users who reacted
 * @param reactedByMe whether the current user is one of them
 */
public record ReactionView(String code, String emoji, long count, boolean reactedByMe) {
}
