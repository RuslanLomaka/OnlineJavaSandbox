package com.example.onlinejava.discussion.dto;

/**
 * Lightweight discussion stats for a problem page link.
 *
 * @param postCount number of non-deleted posts
 */
public record DiscussionSummary(long postCount) {
}
