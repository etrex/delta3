/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.acceptance.chat;

import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.service.ConfidenceEvaluator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Test configuration for ConfidenceEvaluator
 * Provides a controllable implementation for testing
 */
@TestConfiguration
public class TestConfidenceEvaluatorConfig {

    /**
     * Deterministic confidence evaluator for testing
     * Returns confidence based on simple keyword matching
     */
    @Bean
    @Primary
    public ConfidenceEvaluator testConfidenceEvaluator() {
        return new ConfidenceEvaluator(null) {
            @Override
            public double evaluateConfidence(String userQuestion, String aiResponse,
                                            String toolCallsJson, List<ChatHistory> conversationHistory) {
                // Deterministic confidence based on keywords for testing
                if (userQuestion.contains("請問有什麼商品") || userQuestion.contains("查詢訂單")) {
                    return 0.9; // High confidence
                } else if (userQuestion.contains("什麼時候會到")) {
                    return 0.65; // Medium confidence
                } else if (userQuestion.contains("退貨政策")) {
                    return 0.25; // Low confidence
                }
                return 0.5; // Default medium confidence
            }
        };
    }
}
