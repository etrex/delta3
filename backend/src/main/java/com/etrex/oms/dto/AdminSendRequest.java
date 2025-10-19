/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin Send Request DTO
 * Used by admins to send messages (shared by three operations)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSendRequest {
    /**
     * Session ID
     */
    private String sessionId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * AI response ID (optional, used when responding to an AI suggestion)
     */
    private Long aiResponseId;

    /**
     * Message text to send
     */
    private String text;

    /**
     * Original suggestion (used for MODIFIED status)
     */
    private String originalSuggestion;
}
