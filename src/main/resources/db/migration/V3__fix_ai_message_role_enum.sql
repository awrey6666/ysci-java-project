-- Fix ai_messages.role column to use ENUM type instead of VARCHAR
ALTER TABLE ai_messages MODIFY COLUMN role ENUM('user', 'assistant', 'system') NOT NULL;
