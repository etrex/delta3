/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.entity.Faq;
import com.etrex.oms.repository.FaqRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FAQ Controller
 * Provides REST API for FAQ management
 */
@Slf4j
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
@Tag(name = "FAQ", description = "FAQ Management APIs")
public class FaqController {

    private final FaqRepository faqRepository;

    /**
     * Get all FAQs or filter by category
     * GET /api/faqs
     * GET /api/faqs?category=商品技術
     */
    @GetMapping
    @Operation(summary = "Get all FAQs", description = "Get all FAQs, optionally filter by category")
    public ResponseEntity<List<Faq>> getAllFaqs(@RequestParam(required = false) String category) {
        List<Faq> faqs;

        if (category != null && !category.trim().isEmpty()) {
            faqs = faqRepository.findByCategory(category);
            log.debug("Retrieved {} FAQs for category: {}", faqs.size(), category);
        } else {
            faqs = faqRepository.findAllByOrderByCreatedAtDesc();
            log.debug("Retrieved all {} FAQs", faqs.size());
        }

        return ResponseEntity.ok(faqs);
    }

    /**
     * Get all FAQ categories
     * GET /api/faqs/categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Get all categories", description = "Get list of all FAQ categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = faqRepository.findAll()
                .stream()
                .map(Faq::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        log.debug("Retrieved {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    /**
     * Search FAQs by keyword
     * GET /api/faqs/search?keyword=運費
     */
    @GetMapping("/search")
    @Operation(summary = "Search FAQs", description = "Search FAQs by keyword in question and answer")
    public ResponseEntity<List<Faq>> searchFaqs(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Faq> faqs = faqRepository.searchByKeyword(keyword.trim());
        log.debug("Search for '{}' returned {} results", keyword, faqs.size());

        return ResponseEntity.ok(faqs);
    }

    /**
     * Get single FAQ by ID
     * GET /api/faqs/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get FAQ by ID", description = "Get single FAQ by ID")
    public ResponseEntity<Faq> getFaqById(@PathVariable Long id) {
        return faqRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new FAQ (Admin only)
     * POST /api/faqs
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create FAQ", description = "Create new FAQ (Admin only)")
    public ResponseEntity<Faq> createFaq(@RequestBody Faq faq) {
        Faq savedFaq = faqRepository.save(faq);
        log.info("Created new FAQ: id={}, category={}", savedFaq.getId(), savedFaq.getCategory());
        return ResponseEntity.ok(savedFaq);
    }

    /**
     * Update existing FAQ (Admin only)
     * PUT /api/faqs/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update FAQ", description = "Update existing FAQ (Admin only)")
    public ResponseEntity<Faq> updateFaq(@PathVariable Long id, @RequestBody Faq faq) {
        return faqRepository.findById(id)
                .map(existingFaq -> {
                    existingFaq.setQuestion(faq.getQuestion());
                    existingFaq.setAnswer(faq.getAnswer());
                    existingFaq.setCategory(faq.getCategory());
                    Faq updatedFaq = faqRepository.save(existingFaq);
                    log.info("Updated FAQ: id={}, category={}", updatedFaq.getId(), updatedFaq.getCategory());
                    return ResponseEntity.ok(updatedFaq);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete FAQ (Admin only)
     * DELETE /api/faqs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete FAQ", description = "Delete FAQ (Admin only)")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        return faqRepository.findById(id)
                .map(faq -> {
                    faqRepository.delete(faq);
                    log.info("Deleted FAQ: id={}, question={}", id, faq.getQuestion());
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
