/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.embedding;

import com.etrex.oms.entity.Faq;
import com.etrex.oms.entity.Product;
import com.etrex.oms.repository.FaqRepository;
import com.etrex.oms.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes embeddings on application startup.
 *
 * Behavior:
 * - If cache exists: skip initialization (fast startup)
 * - If no cache: compute embeddings for products and FAQs, then save cache
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingInitializer {
    private final EmbeddingService embeddingService;
    private final ProductRepository productRepository;
    private final FaqRepository faqRepository;

    @PostConstruct
    public void initialize() {
        log.info("=== Embedding Initialization Started ===");

        // Check if cache exists
        if (embeddingService.isCacheValid()) {
            int count = embeddingService.getDocumentCount();
            log.info("✅ Cache found. Loaded {} embeddings in < 3 seconds", count);
            log.info("=== Embedding Initialization Completed ===");
            return;
        }

        // No cache, compute embeddings
        log.info("No cache found. Computing embeddings (this may take 1-2 minutes)...");

        long startTime = System.currentTimeMillis();
        int totalCount = 0;

        // 1. Index products
        try {
            List<Product> products = productRepository.findAll();
            log.info("Indexing {} products...", products.size());

            List<String> productTexts = new ArrayList<>();
            for (Product product : products) {
                String text = buildProductText(product);
                productTexts.add(text);
            }

            embeddingService.addDocuments(productTexts);
            totalCount += products.size();
            log.info("✅ Indexed {} products", products.size());

        } catch (Exception e) {
            log.error("Failed to index products", e);
        }

        // 2. Index FAQs
        try {
            List<Faq> faqs = faqRepository.findAll();
            log.info("Indexing {} FAQs...", faqs.size());

            List<String> faqTexts = new ArrayList<>();
            for (Faq faq : faqs) {
                String text = buildFaqText(faq);
                faqTexts.add(text);
            }

            embeddingService.addDocuments(faqTexts);
            totalCount += faqs.size();
            log.info("✅ Indexed {} FAQs", faqs.size());

        } catch (Exception e) {
            log.error("Failed to index FAQs", e);
        }

        // 3. Save cache
        embeddingService.saveCache();

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Computed and cached {} embeddings in {} seconds",
            totalCount, duration / 1000);
        log.info("=== Embedding Initialization Completed ===");
    }

    /**
     * Build searchable text for a product.
     * Combines all relevant fields for semantic search.
     */
    private String buildProductText(Product product) {
        return String.format(
            "商品名稱：%s\n" +
            "描述：%s\n" +
            "價格：NT$%d\n" +
            "庫存：%d\n" +
            "狀態：%s",
            product.getName(),
            product.getDescription() != null ? product.getDescription() : "",
            product.getPrice(),
            product.getStock(),
            product.getStatus()
        );
    }

    /**
     * Build searchable text for a FAQ.
     */
    private String buildFaqText(Faq faq) {
        return String.format(
            "類別：%s\n" +
            "問題：%s\n" +
            "答案：%s",
            faq.getCategory(),
            faq.getQuestion(),
            faq.getAnswer()
        );
    }
}
