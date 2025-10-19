/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * New Message Notification DTO
 * Sent via WebSocket when a user sends a new message
 * Channel: /topic/admin/new-messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageNotification {
    /**
     * User ID
     */
    private Long userId;

    /**
     * User name (for display)
     */
    private String userName;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * Message content
     */
    private String message;

    /**
     * Message ID (chat_history ID)
     */
    private Long messageId;

    /**
     * Timestamp (Unix timestamp in milliseconds)
     */
    private Long timestamp;
}
