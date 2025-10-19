/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.entity;

/**
 * AI Response Status Enum
 * Defines the lifecycle status of an AI-generated response
 */
public enum AiResponseStatus {
    /**
     * AI is currently generating the response
     */
    GENERATING,

    /**
     * Auto-sent to customer (confidence >= 80%)
     */
    AUTO_SENT,

    /**
     * Pending admin decision (confidence 40-80%)
     */
    PENDING,

    /**
     * Admin approved and sent the original suggestion
     */
    APPROVED,

    /**
     * Admin modified the suggestion before sending
     */
    MODIFIED,

    /**
     * Admin rejected the suggestion and wrote their own response
     */
    REJECTED,

    /**
     * Admin ignored the suggestion (timeout or manually ignored)
     */
    IGNORED
}
