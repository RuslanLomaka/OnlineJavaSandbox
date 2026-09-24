-- Screenshots uploaded for discussion posts, stored in the database so one
-- backup covers everything. Rows start unclaimed (post_id NULL) and are
-- linked to a post when it is saved; unclaimed rows are purged after a day.
CREATE TABLE attachment (
    id           UUID        PRIMARY KEY,
    uploader_id  BIGINT      NOT NULL REFERENCES app_user (id),
    post_id      BIGINT      REFERENCES discussion_post (id) ON DELETE CASCADE,
    content_type VARCHAR(32) NOT NULL,
    width        INTEGER     NOT NULL,
    height       INTEGER     NOT NULL,
    size_bytes   INTEGER     NOT NULL,
    data         BYTEA       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_attachment_post ON attachment (post_id);

CREATE INDEX idx_attachment_orphans ON attachment (created_at) WHERE post_id IS NULL;
