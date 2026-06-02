ALTER TABLE chat_rooms
    ADD COLUMN last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

UPDATE chat_rooms
    SET last_activity = created_at
    WHERE last_activity IS NULL;

ALTER TABLE ai_conversations
    ADD COLUMN last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

UPDATE ai_conversations
    SET last_activity = created_at
    WHERE last_activity IS NULL;

CREATE INDEX idx_chat_rooms_last_activity ON chat_rooms(last_activity);
CREATE INDEX idx_ai_conversations_last_activity ON ai_conversations(last_activity);
