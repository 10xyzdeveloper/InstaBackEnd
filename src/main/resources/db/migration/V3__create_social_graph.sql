-- V3__create_social_graph.sql

CREATE TABLE follows (
    follower_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PENDING')),
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY  (follower_id, following_id)
);

-- Fast lookup: "who does this user follow?" + "who follows this user?"
CREATE INDEX idx_follows_follower  ON follows (follower_id, status);
CREATE INDEX idx_follows_following ON follows (following_id, status);

CREATE TABLE likes (
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

CREATE INDEX idx_likes_post_id ON likes (post_id, created_at DESC);

CREATE TABLE comments (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id  UUID               REFERENCES comments(id) ON DELETE CASCADE,  -- threaded replies
    body       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP          -- soft delete
);

CREATE INDEX idx_comments_post_id    ON comments (post_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_parent_id  ON comments (parent_id) WHERE parent_id IS NOT NULL;

CREATE TABLE comment_likes (
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_id UUID      NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, comment_id)
);
