/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import com.etrex.oms.embedding.EmbeddingService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG (Retrieval-Augmented Generation) search tool for AI agents.
 *
 * This tool allows AI assistants to search through product descriptions and FAQs
 * using semantic similarity, providing relevant context to answer user questions.
 *
 * Usage by AI:
 * - When user asks technical questions: "DDR4 和 DDR5 有什麼差別？"
 * - When user asks about product features: "哪些筆電適合影片剪輯？"
 * - When user asks shopping/policy questions: "7天鑑賞期怎麼算？"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchTool {
    private final EmbeddingService embeddingService;

    @Tool("Search knowledge base using semantic similarity. Use this when user asks about product features, technical specs, FAQs, shopping policies, or any question that requires domain knowledge.")
    public String searchKnowledgeBase(String query) {
        try {
            log.info("RAG search triggered with query: {}", query);

            // Search with top 3 results, minimum similarity 0.6
            String results = embeddingService.searchAndFormat(query, 3);

            if (results == null || results.trim().isEmpty()) {
                return "抱歉，我在知識庫中找不到相關資訊。";
            }

            log.info("RAG search returned {} characters", results.length());
            return results;

        } catch (Exception e) {
            log.error("RAG search failed for query: {}", query, e);
            return "抱歉，搜尋知識庫時發生錯誤。";
        }
    }

    /**
     * Direct service access (for programmatic use, not by AI)
     */
    public EmbeddingService getEmbeddingService() {
        return embeddingService;
    }
}
