/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto.ws;

import com.etrex.oms.dto.ToolCallDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Session Update Notification DTO
 * Sent via WebSocket to admins for a specific session
 * Channel: /topic/session/{sessionId}/updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionUpdateNotification {
    /**
     * Update type: "ai_auto_reply", "admin_reply", "new_message"
     */
    private String type;

    /**
     * Message content
     */
    private String content;

    /**
     * Timestamp (Unix timestamp in milliseconds)
     */
    private Long timestamp;

    /**
     * Message ID (chat_history ID)
     */
    private Long messageId;

    /**
     * AI confidence score (only for ai_auto_reply)
     */
    private Double confidence;

    /**
     * AI response ID (chat_ai_response ID, only for ai_auto_reply)
     */
    private Long aiResponseId;

    /**
     * Tool calls (only for ai_auto_reply)
     */
    private List<ToolCallDto> toolCalls;
}
