/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.embedding;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for EmbeddingService RAG functionality.
 *
 * This test verifies:
 * 1. Embeddings are properly initialized
 * 2. Semantic search works correctly
 * 3. Results are formatted properly
 */
@SpringBootTest
class EmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void testEmbeddingsAreInitialized() {
        // Given: Application has started and EmbeddingInitializer has run

        // When: Check document count
        int count = embeddingService.getDocumentCount();

        // Then: Should have documents (15 products + 15 FAQs = 30)
        assertTrue(count > 0, "Embeddings should be initialized with at least some documents");
        System.out.println("✅ Total documents in embedding store: " + count);
    }

    @Test
    void testSemanticSearchForProductQuery() {
        // Given: User asks about laptops
        String query = "適合影片剪輯的筆電";

        // When: Search for relevant documents
        List<EmbeddingMatch<TextSegment>> results = embeddingService.search(query, 3, 0.6);

        // Then: Should return relevant results
        assertFalse(results.isEmpty(), "Should find relevant products for laptop query");

        System.out.println("\n🔍 Query: " + query);
        System.out.println("Found " + results.size() + " results:");

        for (int i = 0; i < results.size(); i++) {
            EmbeddingMatch<TextSegment> match = results.get(i);
            System.out.println(String.format("\n%d. Score: %.3f", i + 1, match.score()));
            System.out.println(match.embedded().text());
        }
    }

    @Test
    void testSemanticSearchForFaqQuery() {
        // Given: User asks about return policy
        String query = "7天鑑賞期怎麼算？";

        // When: Search for relevant documents
        List<EmbeddingMatch<TextSegment>> results = embeddingService.search(query, 3, 0.6);

        // Then: Should return FAQ about return policy
        assertFalse(results.isEmpty(), "Should find FAQ about return policy");

        System.out.println("\n🔍 Query: " + query);
        System.out.println("Found " + results.size() + " results:");

        for (int i = 0; i < results.size(); i++) {
            EmbeddingMatch<TextSegment> match = results.get(i);
            System.out.println(String.format("\n%d. Score: %.3f", i + 1, match.score()));
            System.out.println(match.embedded().text());
        }
    }

    @Test
    void testFormattedSearchResults() {
        // Given: User asks about RAM
        String query = "RAM 16GB 夠用嗎？";

        // When: Search and format results
        String formatted = embeddingService.searchAndFormat(query, 3);

        // Then: Should return formatted context
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty(), "Formatted results should not be empty");
        assertTrue(formatted.contains("相關參考資訊"), "Should contain header");

        System.out.println("\n📋 Formatted results for: " + query);
        System.out.println(formatted);
    }

    @Test
    void testDirectServiceAccess() {
        // Given: Service is injected

        // When: Add a new document directly
        String testDoc = "這是測試用的商品：測試筆電，售價 50000 元";
        String docId = embeddingService.addDocument(testDoc, "test");

        // Then: Document should be added successfully
        assertNotNull(docId, "Document ID should not be null");

        // And: Should be searchable
        List<EmbeddingMatch<TextSegment>> results =
            embeddingService.search("測試筆電", 1, 0.5);

        assertFalse(results.isEmpty(), "Should find the newly added document");

        System.out.println("\n✅ Direct service access test passed");
        System.out.println("Added document with ID: " + docId);
    }
}
