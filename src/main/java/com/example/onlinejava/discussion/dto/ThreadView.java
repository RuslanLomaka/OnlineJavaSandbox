package com.example.onlinejava.discussion.dto;

import java.util.List;

/**
 * A top-level post with its replies.
 *
 * @param root the post that started the thread
 * @param replies replies, oldest first
 */
public record ThreadView(PostView root, List<PostView> replies) {
}
