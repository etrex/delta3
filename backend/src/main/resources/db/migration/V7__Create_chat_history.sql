-- Create chat_history table
CREATE TABLE chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT,
    role VARCHAR(20) NOT NULL,           -- 'USER', 'ASSISTANT', 'SYSTEM'
    message_type VARCHAR(50) NOT NULL,   -- 'MESSAGE', 'ACTION'
    content TEXT NOT NULL,
    action_type VARCHAR(50),             -- 'navigate', 'click', 'submit', etc.
    action_target VARCHAR(255),          -- path, button id, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_history_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create index for fast session queries
CREATE INDEX idx_chat_history_session ON chat_history(session_id, created_at DESC);
CREATE INDEX idx_chat_history_user ON chat_history(user_id, created_at DESC);
