/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.repository;

import com.etrex.oms.entity.AiResponseStatus;
import com.etrex.oms.entity.ChatAiResponse;
import com.etrex.oms.entity.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatAiResponse entity
 * Manages AI response records with confidence scores and feedback
 */
@Repository
public interface ChatAiResponseRepository extends JpaRepository<ChatAiResponse, Long> {

    /**
     * Find all AI responses by session ID, ordered by creation time (newest first)
     */
    List<ChatAiResponse> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find all AI responses by status
     */
    List<ChatAiResponse> findByStatus(AiResponseStatus status);

    /**
     * Find pending suggestions (PENDING status and not expired)
     * @param cutoffTime Only return suggestions created after this time
     */
    @Query("SELECT r FROM ChatAiResponse r WHERE r.status = 'PENDING' AND r.createdAt > :cutoffTime ORDER BY r.createdAt DESC")
    List<ChatAiResponse> findPendingSuggestions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find AI response by user message ID
     */
    Optional<ChatAiResponse> findByUserMessageId(Long userMessageId);

    /**
     * Find all AI responses with specific feedback type
     */
    List<ChatAiResponse> findByFeedbackType(FeedbackType feedbackType);

    /**
     * Find all AI responses by session ID and status
     */
    List<ChatAiResponse> findBySessionIdAndStatus(String sessionId, AiResponseStatus status);

    /**
     * Count pending suggestions
     */
    @Query("SELECT COUNT(r) FROM ChatAiResponse r WHERE r.status = 'PENDING'")
    long countPendingSuggestions();

    /**
     * Find negative feedback responses for learning
     */
    @Query("SELECT r FROM ChatAiResponse r WHERE r.feedbackType = 'NEGATIVE' ORDER BY r.feedbackAt DESC")
    List<ChatAiResponse> findNegativeFeedbackResponses();

    /**
     * Find positive feedback responses for learning
     */
    @Query("SELECT r FROM ChatAiResponse r WHERE r.feedbackType = 'POSITIVE' ORDER BY r.feedbackAt DESC")
    List<ChatAiResponse> findPositiveFeedbackResponses();
}
