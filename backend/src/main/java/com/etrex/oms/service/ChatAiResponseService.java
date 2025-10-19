/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.entity.AiResponseStatus;
import com.etrex.oms.entity.ChatAiResponse;
import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.FeedbackType;
import com.etrex.oms.entity.User;
import com.etrex.oms.repository.ChatAiResponseRepository;
import com.etrex.oms.repository.ChatHistoryRepository;
import com.etrex.oms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat AI Response Service
 * Manages AI response records with confidence scores and admin feedback
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAiResponseService {

    private final ChatAiResponseRepository chatAiResponseRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Save AI response record
     */
    @Transactional
    public ChatAiResponse saveAiResponse(
            String sessionId,
            Long userMessageId,
            String suggestedResponse,
            Double confidenceScore,
            String toolCallsJson,
            AiResponseStatus status
    ) {
        ChatHistory userMessage = chatHistoryRepository.findById(userMessageId)
                .orElseThrow(() -> new IllegalArgumentException("User message not found: " + userMessageId));

        ChatAiResponse aiResponse = ChatAiResponse.builder()
                .sessionId(sessionId)
                .userMessage(userMessage)
                .suggestedResponse(suggestedResponse)
                .confidenceScore(BigDecimal.valueOf(confidenceScore))
                .toolCallsJson(toolCallsJson)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        ChatAiResponse saved = chatAiResponseRepository.save(aiResponse);
        log.info("Saved AI response: id={}, sessionId={}, confidence={}, status={}",
                saved.getId(), sessionId, confidenceScore, status);

        return saved;
    }

    /**
     * Mark AI response as auto-sent
     */
    @Transactional
    public void markAsAutoSent(Long aiResponseId, Long responseMessageId) {
        ChatAiResponse aiResponse = chatAiResponseRepository.findById(aiResponseId)
                .orElseThrow(() -> new IllegalArgumentException("AI response not found: " + aiResponseId));

        ChatHistory responseMessage = chatHistoryRepository.findById(responseMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Response message not found: " + responseMessageId));

        aiResponse.setStatus(AiResponseStatus.AUTO_SENT);
        aiResponse.setResponseMessage(responseMessage);
        aiResponse.setActualResponse(responseMessage.getContent());

        chatAiResponseRepository.save(aiResponse);
        log.info("Marked AI response as AUTO_SENT: id={}", aiResponseId);
    }

    /**
     * Mark AI response as approved by admin
     */
    @Transactional
    public void markAsApproved(Long aiResponseId, Long responseMessageId, Long adminId) {
        ChatAiResponse aiResponse = chatAiResponseRepository.findById(aiResponseId)
                .orElseThrow(() -> new IllegalArgumentException("AI response not found: " + aiResponseId));

        ChatHistory responseMessage = chatHistoryRepository.findById(responseMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Response message not found: " + responseMessageId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));

        aiResponse.setStatus(AiResponseStatus.APPROVED);
        aiResponse.setResponseMessage(responseMessage);
        aiResponse.setActualResponse(responseMessage.getContent());
        aiResponse.setReviewedByAdmin(admin);
        aiResponse.setReviewedAt(LocalDateTime.now());

        chatAiResponseRepository.save(aiResponse);
        log.info("Marked AI response as APPROVED: id={}, adminId={}", aiResponseId, adminId);
    }

    /**
     * Mark AI response as modified by admin
     */
    @Transactional
    public void markAsModified(Long aiResponseId, String actualResponse, Long responseMessageId, Long adminId) {
        ChatAiResponse aiResponse = chatAiResponseRepository.findById(aiResponseId)
                .orElseThrow(() -> new IllegalArgumentException("AI response not found: " + aiResponseId));

        ChatHistory responseMessage = chatHistoryRepository.findById(responseMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Response message not found: " + responseMessageId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));

        aiResponse.setStatus(AiResponseStatus.MODIFIED);
        aiResponse.setActualResponse(actualResponse);
        aiResponse.setResponseMessage(responseMessage);
        aiResponse.setReviewedByAdmin(admin);
        aiResponse.setReviewedAt(LocalDateTime.now());

        chatAiResponseRepository.save(aiResponse);
        log.info("Marked AI response as MODIFIED: id={}, adminId={}", aiResponseId, adminId);
    }

    /**
     * Mark AI response as rejected by admin
     */
    @Transactional
    public void markAsRejected(Long aiResponseId, String actualResponse, Long responseMessageId, Long adminId) {
        ChatAiResponse aiResponse = chatAiResponseRepository.findById(aiResponseId)
                .orElseThrow(() -> new IllegalArgumentException("AI response not found: " + aiResponseId));

        ChatHistory responseMessage = chatHistoryRepository.findById(responseMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Response message not found: " + responseMessageId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));

        aiResponse.setStatus(AiResponseStatus.REJECTED);
        aiResponse.setActualResponse(actualResponse);
        aiResponse.setResponseMessage(responseMessage);
        aiResponse.setReviewedByAdmin(admin);
        aiResponse.setReviewedAt(LocalDateTime.now());

        chatAiResponseRepository.save(aiResponse);
        log.info("Marked AI response as REJECTED: id={}, adminId={}", aiResponseId, adminId);
    }

    /**
     * Update feedback for AI response
     */
    @Transactional
    public void updateFeedback(Long aiResponseId, FeedbackType feedbackType, String reason, Long adminId) {
        ChatAiResponse aiResponse = chatAiResponseRepository.findById(aiResponseId)
                .orElseThrow(() -> new IllegalArgumentException("AI response not found: " + aiResponseId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));

        aiResponse.setFeedbackType(feedbackType);
        aiResponse.setFeedbackReason(reason);
        aiResponse.setFeedbackByAdmin(admin);
        aiResponse.setFeedbackAt(LocalDateTime.now());

        chatAiResponseRepository.save(aiResponse);
        log.info("Updated feedback for AI response: id={}, feedbackType={}, adminId={}",
                aiResponseId, feedbackType, adminId);
    }

    /**
     * Get pending suggestions (created within last 24 hours)
     */
    public List<ChatAiResponse> getPendingSuggestions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        return chatAiResponseRepository.findPendingSuggestions(cutoffTime);
    }

    /**
     * Get AI responses by session ID
     */
    public List<ChatAiResponse> getBySessionId(String sessionId) {
        return chatAiResponseRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    /**
     * Get AI response by ID
     */
    public ChatAiResponse getById(Long id) {
        return chatAiResponseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI response not found: " + id));
    }

    /**
     * Get negative feedback responses for learning
     */
    public List<ChatAiResponse> getNegativeFeedbackResponses() {
        return chatAiResponseRepository.findNegativeFeedbackResponses();
    }

    /**
     * Get positive feedback responses for learning
     */
    public List<ChatAiResponse> getPositiveFeedbackResponses() {
        return chatAiResponseRepository.findPositiveFeedbackResponses();
    }

    /**
     * Count pending suggestions
     */
    public long countPendingSuggestions() {
        return chatAiResponseRepository.countPendingSuggestions();
    }
}
