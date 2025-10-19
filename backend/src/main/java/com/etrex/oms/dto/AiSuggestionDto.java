/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Suggestion DTO
 * Represents an AI-generated suggestion for admin review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionDto {
    /**
     * AI response ID (chat_ai_response ID)
     */
    private Long aiResponseId;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Suggested text
     */
    private String suggestedText;

    /**
     * Confidence score (0.0-1.0)
     */
    private Double confidence;

    /**
     * Tool calls executed during response generation
     */
    private List<ToolCallDto> toolCalls;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;
}
