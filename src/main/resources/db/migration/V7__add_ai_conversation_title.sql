-- Add title column for AI conversations
ALTER TABLE ai_conversations
  ADD COLUMN title VARCHAR(255) DEFAULT NULL;
