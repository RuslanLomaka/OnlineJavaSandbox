-- Users who have signed in at least once. Identity is the pair
-- (provider, provider_user_id): stable across GitHub renames and ready for
-- additional OAuth providers. "user" is reserved in PostgreSQL, hence app_user.
CREATE TABLE app_user (
    id               BIGSERIAL    PRIMARY KEY,
    provider         VARCHAR(32)  NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    login            VARCHAR(100) NOT NULL,
    display_name     VARCHAR(255) NOT NULL,
    avatar_url       VARCHAR(512),
    created_at       TIMESTAMPTZ  NOT NULL,
    last_login_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_app_user_provider_identity UNIQUE (provider, provider_user_id)
);
