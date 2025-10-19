/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import com.etrex.oms.entity.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feedback Request DTO
 * Used by admins to submit feedback on AI responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    /**
     * AI response ID (chat_ai_response ID)
     */
    private Long aiResponseId;

    /**
     * Feedback type: POSITIVE or NEGATIVE
     */
    private FeedbackType feedbackType;

    /**
     * Feedback reason (optional)
     */
    private String reason;
}
