-- Create chat_ai_response table for AI-assisted customer service
-- This table stores AI response records with confidence scores and feedback

CREATE TABLE chat_ai_response (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_message_id BIGINT NOT NULL,
    suggested_response TEXT NOT NULL,
    confidence_score DECIMAL(3,2) NOT NULL,
    tool_calls_json TEXT,
    status VARCHAR(20) NOT NULL,
    actual_response TEXT,
    response_message_id BIGINT,
    reviewed_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,

    -- Feedback fields (NULLABLE)
    feedback_type VARCHAR(20),
    feedback_reason TEXT,
    feedback_by_admin_id BIGINT,
    feedback_at TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_user_message FOREIGN KEY (user_message_id)
        REFERENCES chat_history(id) ON DELETE CASCADE,
    CONSTRAINT fk_response_message FOREIGN KEY (response_message_id)
        REFERENCES chat_history(id) ON DELETE SET NULL,
    CONSTRAINT fk_reviewed_by_admin FOREIGN KEY (reviewed_by_admin_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_feedback_by_admin FOREIGN KEY (feedback_by_admin_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_chat_ai_response_session_id ON chat_ai_response(session_id);
CREATE INDEX idx_chat_ai_response_status ON chat_ai_response(status);
CREATE INDEX idx_chat_ai_response_confidence ON chat_ai_response(confidence_score);
CREATE INDEX idx_chat_ai_response_created_at ON chat_ai_response(created_at);
CREATE INDEX idx_chat_ai_response_feedback_type ON chat_ai_response(feedback_type);

-- Comments
COMMENT ON TABLE chat_ai_response IS 'AI response records with confidence evaluation and admin feedback';
COMMENT ON COLUMN chat_ai_response.confidence_score IS 'AI confidence score (0.00-1.00) based on 5 evaluation questions';
COMMENT ON COLUMN chat_ai_response.status IS 'Status: AUTO_SENT, PENDING, APPROVED, MODIFIED, REJECTED, IGNORED';
COMMENT ON COLUMN chat_ai_response.tool_calls_json IS 'JSON array of tool calls executed during AI response generation';
COMMENT ON COLUMN chat_ai_response.feedback_type IS 'Admin feedback: POSITIVE or NEGATIVE';
