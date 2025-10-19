/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Action Notification DTO
 * Sent via WebSocket when a user performs an action on the website
 * Channel: /topic/admin/user-actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActionNotification {
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
     * Action type: "navigate", "click", "submit", "open_modal"
     */
    private String actionType;

    /**
     * Action target: "/products/123", "buy_button", "checkout_form"
     */
    private String actionTarget;

    /**
     * Timestamp (Unix timestamp in milliseconds)
     */
    private Long timestamp;
}
