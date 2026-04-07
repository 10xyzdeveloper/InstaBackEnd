-- Stories Table
CREATE TABLE stories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_url VARCHAR(512) NOT NULL,
    media_type VARCHAR(10) NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    caption VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for querying unexpired stories efficiently (where created_at > NOW() - 24 hours)
CREATE INDEX idx_stories_user_id_created_at ON stories(user_id, created_at);
-- General index on created_at for global eviction job
CREATE INDEX idx_stories_created_at ON stories(created_at);
