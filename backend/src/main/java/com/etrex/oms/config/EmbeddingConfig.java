/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import com.etrex.oms.embedding.CachedInMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RAG (Retrieval-Augmented Generation) components.
 *
 * Configures:
 * 1. EmbeddingModel: AllMiniLmL6V2 (runs locally, no API key needed)
 * 2. EmbeddingStore: CachedInMemoryEmbeddingStore (with file-based cache)
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${embedding.cache-path:./data/embeddings.cache}")
    private String cachePath;

    /**
     * Local embedding model that runs in-process.
     *
     * Model: sentence-transformers/all-MiniLM-L6-v2
     * - Dimension: 384
     * - Best for: semantic search, sentence similarity
     * - Speed: ~100 sentences/second on CPU
     * - Size: ~80MB
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing AllMiniLmL6V2 embedding model...");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Cached in-memory embedding store with JSON-based persistence.
     *
     * Features:
     * - First run: computes embeddings and saves to cache (1-2 minutes)
     * - Subsequent runs: loads from cache (2-3 seconds)
     * - Cache file: ./data/embeddings.cache (JSON format)
     *
     * Note: For production use, consider using a persistent vector database
     * like PGVector, ChromaDB, or similar.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Initializing cached in-memory embedding store with cache path: {}", cachePath);
        return new CachedInMemoryEmbeddingStore(cachePath);
    }
}
