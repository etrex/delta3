/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Message Notification DTO
 * Sent via WebSocket to a specific customer
 * Channel: /user/{userId}/queue/messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMessageNotification {
    /**
     * Message content
     */
    private String content;

    /**
     * Timestamp (Unix timestamp in milliseconds)
     */
    private Long timestamp;

    /**
     * Message type: "ai_auto", "admin", "ai_approved"
     */
    private String messageType;

    /**
     * Message ID (chat_history ID)
     */
    private Long messageId;
}
