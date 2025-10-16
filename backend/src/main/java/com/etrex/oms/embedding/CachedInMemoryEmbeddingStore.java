/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Cached In-Memory Embedding Store with JSON-based serialization.
 *
 * This implementation wraps InMemoryEmbeddingStore and adds automatic caching:
 * - On first run: computes embeddings and saves to cache file (slow, 1-2 minutes)
 * - On subsequent runs: loads from cache file (fast, 2-3 seconds)
 *
 * Uses JSON serialization instead of Java serialization for better compatibility.
 * Cache file is stored in: ./data/embeddings.cache
 */
@Slf4j
public class CachedInMemoryEmbeddingStore implements EmbeddingStore<TextSegment> {
    private final InMemoryEmbeddingStore<TextSegment> delegate;
    private final String cacheFilePath;
    private final ObjectMapper objectMapper;
    private int documentCount = 0;

    public CachedInMemoryEmbeddingStore(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        this.objectMapper = new ObjectMapper();
        this.delegate = loadFromCache();
    }

    /**
     * Load embeddings from JSON cache file, or create new empty store if cache doesn't exist
     */
    private InMemoryEmbeddingStore<TextSegment> loadFromCache() {
        File cacheFile = new File(cacheFilePath);

        if (cacheFile.exists()) {
            try {
                CacheData cacheData = objectMapper.readValue(cacheFile, CacheData.class);

                InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

                // Rebuild the store from cached entries
                for (CachedEntry entry : cacheData.getEntries()) {
                    Embedding embedding = new Embedding(entry.getVector());
                    TextSegment segment = TextSegment.from(entry.getText());
                    store.add(entry.getId(), embedding, segment);
                    documentCount++;
                }

                log.info("✅ Loaded {} embeddings from cache: {}", documentCount, cacheFilePath);
                return store;

            } catch (Exception e) {
                log.warn("Failed to load cache ({}), will recompute embeddings", e.getMessage());
            }
        } else {
            log.info("Cache file not found: {}, will compute embeddings on first run", cacheFilePath);
        }

        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Save current embeddings to JSON cache file
     */
    public void saveCache() {
        try {
            // Ensure directory exists
            File cacheFile = new File(cacheFilePath);
            cacheFile.getParentFile().mkdirs();

            // Extract all embeddings from the delegate store
            EmbeddingSearchResult<TextSegment> result = delegate.search(
                EmbeddingSearchRequest.builder()
                    .queryEmbedding(new Embedding(new float[384])) // dummy query
                    .maxResults(Integer.MAX_VALUE)
                    .minScore(0.0)
                    .build()
            );

            List<CachedEntry> entries = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                CachedEntry entry = new CachedEntry();
                entry.setId(match.embeddingId());
                entry.setVector(match.embedding().vector());
                entry.setText(match.embedded().text());
                entries.add(entry);
            }

            CacheData cacheData = new CacheData();
            cacheData.setVersion("1.0");
            cacheData.setEntries(entries);

            // Write to JSON file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, cacheData);

            log.info("✅ Saved {} embeddings to cache: {}", entries.size(), cacheFilePath);

        } catch (Exception e) {
            log.error("Failed to save cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Invalidate cache file (force recomputation on next startup)
     */
    public void invalidateCache() {
        File cacheFile = new File(cacheFilePath);
        if (cacheFile.exists()) {
            boolean deleted = cacheFile.delete();
            if (deleted) {
                log.info("Cache invalidated: {}", cacheFilePath);
            } else {
                log.warn("Failed to delete cache file: {}", cacheFilePath);
            }
        }
    }

    /**
     * Check if cache exists and is not empty
     */
    public boolean isCacheValid() {
        File cacheFile = new File(cacheFilePath);
        return cacheFile.exists() && cacheFile.length() > 0;
    }

    /**
     * Get total number of embeddings in store
     */
    public int size() {
        return documentCount;
    }

    // ==================== EmbeddingStore Interface Implementation ====================

    @Override
    public String add(Embedding embedding) {
        String id = delegate.add(embedding);
        documentCount++;
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        delegate.add(id, embedding);
        documentCount++;
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = delegate.add(embedding, textSegment);
        documentCount++;
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = delegate.addAll(embeddings);
        documentCount += embeddings.size();
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = delegate.addAll(embeddings, embedded);
        documentCount += embeddings.size();
        return ids;
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        return delegate.search(request);
    }

    @Override
    public void remove(String id) {
        delegate.remove(id);
        documentCount--;
    }

    @Override
    public void removeAll() {
        delegate.removeAll();
        documentCount = 0;
    }

    // ==================== Cache Data Structures ====================

    /**
     * Container for cache data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheData {
        private String version;
        private List<CachedEntry> entries;
    }

    /**
     * Single cached embedding entry
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedEntry {
        private String id;
        private float[] vector;
        private String text;
    }
}
