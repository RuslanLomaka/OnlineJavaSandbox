-- Per-problem discussion. Threads are two levels deep: a top-level post has
-- thread_id NULL; every reply points at its thread's root via thread_id, and
-- at the post it actually answers via reply_to_id (for "replying to" quotes).
CREATE TABLE discussion_post (
    id            BIGSERIAL    PRIMARY KEY,
    problem_slug  VARCHAR(100) NOT NULL,
    author_id     BIGINT       NOT NULL REFERENCES app_user (id),
    thread_id     BIGINT       REFERENCES discussion_post (id),
    reply_to_id   BIGINT       REFERENCES discussion_post (id),
    body_markdown TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    edited_at     TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0
);

-- Newest threads of a problem (the discussion page's main query).
CREATE INDEX idx_discussion_post_threads
    ON discussion_post (problem_slug, created_at DESC)
    WHERE thread_id IS NULL;

-- Replies of a set of threads, oldest first.
CREATE INDEX idx_discussion_post_replies
    ON discussion_post (thread_id, created_at);

-- One row per (post, user, reaction); the primary key makes reacting idempotent.
CREATE TABLE post_reaction (
    post_id    BIGINT      NOT NULL REFERENCES discussion_post (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id),
    reaction   VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (post_id, user_id, reaction)
);
