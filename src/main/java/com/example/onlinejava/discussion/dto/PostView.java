package com.example.onlinejava.discussion.dto;

import java.time.Instant;
import java.util.List;

/**
 * A post as returned to the browser.
 *
 * @param id post id
 * @param author author profile
 * @param bodyHtml sanitized HTML body (empty if deleted)
 * @param createdAt creation time
 * @param editedAt last edit time, or {@code null}
 * @param deleted whether the post was deleted
 * @param own whether the current user wrote it (controls edit/delete buttons;
 *     the server re-checks ownership on every change)
 * @param replyTo the post this one answers, or {@code null}
 * @param reactions reaction counts
 */
public record PostView(
    long id,
    AuthorView author,
    String bodyHtml,
    Instant createdAt,
    Instant editedAt,
    boolean deleted,
    boolean own,
    ReplyRef replyTo,
    List<ReactionView> reactions
) {
}
