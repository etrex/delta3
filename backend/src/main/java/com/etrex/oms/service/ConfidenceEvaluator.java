/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.entity.ChatHistory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Confidence Evaluator Service
 * Calculates AI response confidence score using 5 independent LLM evaluation questions
 * Score range: 0.0 - 1.0 (total 25 points / 25 = confidence)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfidenceEvaluator {

    private final ChatLanguageModel chatLanguageModel;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Evaluate confidence score for an AI response
     * Uses 5 independent evaluation questions (0-5 points each)
     *
     * @param userQuestion User's original question
     * @param aiResponse AI's response
     * @param toolCallsJson Tool calls in JSON format
     * @param conversationHistory Recent conversation history
     * @return Confidence score (0.0-1.0)
     */
    public double evaluateConfidence(
            String userQuestion,
            String aiResponse,
            String toolCallsJson,
            List<ChatHistory> conversationHistory
    ) {
        log.info("Evaluating confidence for AI response");

        try {
            // Format conversation history
            String conversationHistoryText = formatConversationHistory(conversationHistory);

            // Execute 5 evaluation questions in parallel
            CompletableFuture<Integer> scoreCompleteness = CompletableFuture.supplyAsync(
                    () -> evaluateCompleteness(userQuestion, aiResponse), executorService);

            CompletableFuture<Integer> scoreCertainty = CompletableFuture.supplyAsync(
                    () -> evaluateCertainty(aiResponse), executorService);

            CompletableFuture<Integer> scoreSensitivity = CompletableFuture.supplyAsync(
                    () -> evaluateSensitivity(userQuestion, aiResponse), executorService);

            CompletableFuture<Integer> scoreToolCallSuccess = CompletableFuture.supplyAsync(
                    () -> evaluateToolCallSuccess(toolCallsJson), executorService);

            CompletableFuture<Integer> scoreNeedsHumanReview = CompletableFuture.supplyAsync(
                    () -> evaluateNeedsHumanReview(userQuestion, aiResponse, conversationHistoryText), executorService);

            // Wait for all evaluations to complete
            CompletableFuture.allOf(scoreCompleteness, scoreCertainty, scoreSensitivity,
                    scoreToolCallSuccess, scoreNeedsHumanReview).join();

            // Get scores
            int completeness = scoreCompleteness.get();
            int certainty = scoreCertainty.get();
            int sensitivity = scoreSensitivity.get();
            int toolCallSuccess = scoreToolCallSuccess.get();
            int needsHumanReview = scoreNeedsHumanReview.get();

            // Calculate total score (max 25 points)
            int totalScore = completeness + certainty + sensitivity + toolCallSuccess + needsHumanReview;

            // Convert to confidence (0.0-1.0)
            double confidence = totalScore / 25.0;

            log.info("Confidence evaluation complete: {} (completeness={}, certainty={}, sensitivity={}, toolCallSuccess={}, needsHumanReview={})",
                    String.format("%.2f", confidence), completeness, certainty, sensitivity, toolCallSuccess, needsHumanReview);

            return Math.max(0.0, Math.min(1.0, confidence)); // Clamp to [0.0, 1.0]

        } catch (Exception e) {
            log.error("Error evaluating confidence", e);
            return 0.0; // Return 0 on error
        }
    }

    /**
     * Question 1: Response Completeness (0-5 points)
     * Does the AI response fully answer the user's question?
     */
    private int evaluateCompleteness(String userQuestion, String aiResponse) {
        String prompt = String.format(
                "評估以下 AI 回應是否完整回答了用戶的問題。\n\n" +
                        "用戶問題：%s\n\n" +
                        "AI 回應：%s\n\n" +
                        "請用 0-5 分評分（0=完全沒回答，5=完全回答）。\n" +
                        "只回答一個數字（0、1、2、3、4 或 5），不要有其他文字。",
                userQuestion, aiResponse
        );

        return extractScore(chatLanguageModel.generate(prompt));
    }

    /**
     * Question 2: Language Certainty (0-5 points)
     * Is the AI response confident and certain, or hesitant and uncertain?
     */
    private int evaluateCertainty(String aiResponse) {
        String prompt = String.format(
                "評估以下 AI 回應的語氣是否肯定、有信心。\n\n" +
                        "AI 回應：%s\n\n" +
                        "請用 0-5 分評分（0=非常不確定，含有大量「可能」「也許」等詞，5=非常肯定確定）。\n" +
                        "只回答一個數字（0、1、2、3、4 或 5），不要有其他文字。",
                aiResponse
        );

        return extractScore(chatLanguageModel.generate(prompt));
    }

    /**
     * Question 3: Sensitivity Assessment (0-5 points)
     * Does the response involve sensitive topics that need human review?
     */
    private int evaluateSensitivity(String userQuestion, String aiResponse) {
        String prompt = String.format(
                "評估以下對話是否涉及敏感話題（退款、投訴、法律糾紛、個人資料、金錢等）。\n\n" +
                        "用戶問題：%s\n\n" +
                        "AI 回應：%s\n\n" +
                        "請用 0-5 分評分（0=高度敏感，需要人工處理，5=完全不敏感，可安全自動回覆）。\n" +
                        "只回答一個數字（0、1、2、3、4 或 5），不要有其他文字。",
                userQuestion, aiResponse
        );

        return extractScore(chatLanguageModel.generate(prompt));
    }

    /**
     * Question 4: Tool Call Success Rate (0-5 points)
     * Were all tool calls successful?
     */
    private int evaluateToolCallSuccess(String toolCallsJson) {
        if (toolCallsJson == null || toolCallsJson.trim().isEmpty() || toolCallsJson.equals("null")) {
            // No tool calls = perfect score (no failures)
            return 5;
        }

        String prompt = String.format(
                "評估以下工具呼叫是否全部成功。\n\n" +
                        "工具呼叫記錄：%s\n\n" +
                        "請用 0-5 分評分（0=全部失敗，5=全部成功或沒有呼叫）。\n" +
                        "只回答一個數字（0、1、2、3、4 或 5），不要有其他文字。",
                toolCallsJson
        );

        return extractScore(chatLanguageModel.generate(prompt));
    }

    /**
     * Question 5: Needs Human Review (0-5 points)
     * Does this response need human confirmation before sending?
     */
    private int evaluateNeedsHumanReview(String userQuestion, String aiResponse, String conversationHistory) {
        String prompt = String.format(
                "評估以下 AI 回應是否需要人工審核後才能發送給用戶。\n\n" +
                        "對話歷史：%s\n\n" +
                        "用戶問題：%s\n\n" +
                        "AI 回應：%s\n\n" +
                        "請用 0-5 分評分（0=必須人工審核，5=可以直接自動發送）。\n" +
                        "只回答一個數字（0、1、2、3、4 或 5），不要有其他文字。",
                conversationHistory, userQuestion, aiResponse
        );

        return extractScore(chatLanguageModel.generate(prompt));
    }

    /**
     * Format conversation history for evaluation
     */
    private String formatConversationHistory(List<ChatHistory> history) {
        if (history == null || history.isEmpty()) {
            return "(無對話歷史)";
        }

        StringBuilder sb = new StringBuilder();
        for (ChatHistory h : history) {
            if ("ACTION".equals(h.getMessageType())) {
                sb.append(h.getContent()).append("\n");
            } else if ("USER".equals(h.getRole())) {
                sb.append("用戶: ").append(h.getContent()).append("\n");
            } else if ("ASSISTANT".equals(h.getRole())) {
                sb.append("助手: ").append(h.getContent()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Extract score from LLM response
     * Tries to parse the first digit found in the response
     */
    private int extractScore(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Empty response from LLM, defaulting to 0");
            return 0;
        }

        // Try to extract first digit from response
        String trimmed = response.trim();
        for (char c : trimmed.toCharArray()) {
            if (Character.isDigit(c)) {
                int score = Character.getNumericValue(c);
                // Clamp to valid range [0, 5]
                return Math.max(0, Math.min(5, score));
            }
        }

        log.warn("Could not extract score from response: {}, defaulting to 0", response);
        return 0;
    }
}
