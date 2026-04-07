-- V2__create_posts_and_media.sql

CREATE TABLE posts (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    caption    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP          -- NULL = active (soft delete)
);

-- Composite index for feed: ORDER BY created_at DESC, id DESC
CREATE INDEX idx_posts_feed    ON posts (created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_posts_user_id ON posts (user_id) WHERE deleted_at IS NULL;

CREATE TABLE post_media (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    url        VARCHAR(512) NOT NULL,
    media_type VARCHAR(10) NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    position   INTEGER     NOT NULL
);

CREATE TABLE hashtags (
    id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE  -- always stored lowercase
);

-- Enable pg_trgm for fast ILIKE search on hashtag names
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_hashtags_name_trgm ON hashtags USING gin (name gin_trgm_ops);

CREATE TABLE post_hashtags (
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    hashtag_id UUID NOT NULL REFERENCES hashtags(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, hashtag_id)
);
