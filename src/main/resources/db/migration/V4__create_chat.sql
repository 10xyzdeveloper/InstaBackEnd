-- V4__create_chat.sql

CREATE TABLE conversations (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conv_members_user ON conversation_members (user_id);

CREATE TABLE messages (
    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body            TEXT      NOT NULL,
    read_at         TIMESTAMP,          -- NULL = unread
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- For paginating message history efficiently
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at DESC);
