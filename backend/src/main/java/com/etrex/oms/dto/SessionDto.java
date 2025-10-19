/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session DTO
 * Represents a chat session in the admin session list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    /**
     * Session ID
     */
    private String sessionId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Last message content
     */
    private String lastMessage;

    /**
     * Last message timestamp (Unix timestamp in milliseconds)
     */
    private Long lastMessageTime;

    /**
     * Has unread messages
     */
    private Boolean hasUnread;

    /**
     * Has pending AI suggestion
     */
    private Boolean hasPendingSuggestion;
}
