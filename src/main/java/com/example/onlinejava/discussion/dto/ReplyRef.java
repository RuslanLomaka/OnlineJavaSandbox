package com.example.onlinejava.discussion.dto;

/**
 * Short reference to the post a reply answers, for "replying to" quotes.
 *
 * @param id replied-to post id
 * @param authorLogin its author's login
 * @param excerpt plain-text start of its body (empty if it was deleted);
 *     clients must insert it as text, not HTML
 */
public record ReplyRef(long id, String authorLogin, String excerpt) {
}
