/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chat AI Response Entity
 * Stores AI-generated responses with confidence scores and admin feedback
 */
@Entity
@Table(name = "chat_ai_response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_message_id", nullable = false)
    private ChatHistory userMessage;

    @Column(name = "suggested_response", nullable = false, columnDefinition = "TEXT")
    private String suggestedResponse;

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "tool_calls_json", columnDefinition = "TEXT")
    private String toolCallsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiResponseStatus status;

    @Column(name = "actual_response", columnDefinition = "TEXT")
    private String actualResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_message_id")
    private ChatHistory responseMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // Feedback fields (nullable)
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", length = 20)
    private FeedbackType feedbackType;

    @Column(name = "feedback_reason", columnDefinition = "TEXT")
    private String feedbackReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_by_admin_id")
    private User feedbackByAdmin;

    @Column(name = "feedback_at")
    private LocalDateTime feedbackAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
