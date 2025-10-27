-- Add metadata column to chat_history table for storing tool execution requests/results
ALTER TABLE chat_history ADD COLUMN metadata TEXT;
