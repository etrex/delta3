/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG (Retrieval-Augmented Generation) operations.
 *
 * Provides methods to:
 * 1. Add documents to vector store
 * 2. Search for relevant documents
 * 3. Format search results for LLM context
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    /**
     * Add a single document to the embedding store.
     *
     * @param text Document text
     * @param metadata Optional metadata (e.g., "productId=123")
     * @return Document ID
     */
    public String addDocument(String text, String metadata) {
        TextSegment segment = metadata != null
            ? TextSegment.from(text, new dev.langchain4j.data.document.Metadata().put("source", metadata))
            : TextSegment.from(text);

        Embedding embedding = embeddingModel.embed(text).content();
        return embeddingStore.add(embedding, segment);
    }

    /**
     * Add multiple documents to the embedding store.
     *
     * @param texts List of document texts
     * @return List of document IDs
     */
    public List<String> addDocuments(List<String> texts) {
        List<TextSegment> segments = texts.stream()
            .map(TextSegment::from)
            .collect(Collectors.toList());

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        return embeddingStore.addAll(embeddings, segments);
    }

    /**
     * Search for relevant documents using semantic similarity.
     *
     * @param query User query
     * @param maxResults Maximum number of results
     * @param minScore Minimum similarity score (0.0 - 1.0)
     * @return List of matching documents with scores
     */
    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults, double minScore) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build()
        );

        return result.matches();
    }

    /**
     * Search and format results as context string for LLM.
     *
     * @param query User query
     * @param maxResults Maximum number of results
     * @return Formatted context string
     */
    public String searchAndFormat(String query, int maxResults) {
        List<EmbeddingMatch<TextSegment>> matches = search(query, maxResults, 0.6);

        if (matches.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("## 相關參考資訊\n\n");

        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            context.append(String.format("%d. %s (相似度: %.2f)\n\n",
                i + 1,
                match.embedded().text(),
                match.score()));
        }

        return context.toString();
    }

    /**
     * Get total number of documents in the embedding store.
     *
     * @return Document count
     */
    public int getDocumentCount() {
        if (embeddingStore instanceof CachedInMemoryEmbeddingStore) {
            return ((CachedInMemoryEmbeddingStore) embeddingStore).size();
        }
        return 0;
    }

    /**
     * Save current embeddings to cache file.
     * Only works with CachedInMemoryEmbeddingStore.
     */
    public void saveCache() {
        if (embeddingStore instanceof CachedInMemoryEmbeddingStore) {
            ((CachedInMemoryEmbeddingStore) embeddingStore).saveCache();
        }
    }

    /**
     * Check if cache exists and is valid.
     * Only works with CachedInMemoryEmbeddingStore.
     *
     * @return true if cache is valid
     */
    public boolean isCacheValid() {
        if (embeddingStore instanceof CachedInMemoryEmbeddingStore) {
            return ((CachedInMemoryEmbeddingStore) embeddingStore).isCacheValid();
        }
        return false;
    }
}
