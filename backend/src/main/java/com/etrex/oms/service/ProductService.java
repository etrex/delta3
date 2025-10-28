/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.ProductDTO;
import com.etrex.oms.entity.Product;
import com.etrex.oms.exception.BusinessException;
import com.etrex.oms.exception.ResourceNotFoundException;
import com.etrex.oms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(Long id) {
        log.info("🔍 [CACHE] getProductById({}) - 從資料庫查詢（快取未命中）", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductDTO dto = convertToDTO(product);
        log.info("📦 [CACHE] getProductById({}) - 查詢結果: stock={}, 即將存入快取", id, dto.getStock());
        return dto;
    }

    public Page<ProductDTO> getProducts(String keyword, Product.Status status, Pageable pageable) {
        Page<Product> products;
        if (keyword != null && !keyword.isEmpty()) {
            if (status != null) {
                products = productRepository.searchByKeywordAndStatus(keyword, status, pageable);
            } else {
                products = productRepository.searchByKeyword(keyword, pageable);
            }
        } else {
            if (status != null) {
                products = productRepository.findByStatus(status, pageable);
            } else {
                products = productRepository.findAll(pageable);
            }
        }
        return products.map(this::convertToDTO);
    }

    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        product.setStatus(Product.Status.valueOf(productDTO.getStatus()));

        Product saved = productRepository.save(product);
        return convertToDTO(saved);
    }

    @CacheEvict(value = "products", key = "#id", beforeInvocation = true)
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        product.setStatus(Product.Status.valueOf(productDTO.getStatus()));

        Product updated = productRepository.save(product);
        return convertToDTO(updated);
    }

    @CacheEvict(value = "products", key = "#id", beforeInvocation = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setStatus(Product.Status.INACTIVE);
        productRepository.save(product);
    }

    /**
     * Atomically deduct stock from a product (防止超賣).
     * This method uses database-level atomic operation to prevent race conditions.
     *
     * @param productId The ID of the product
     * @param quantity The quantity to deduct
     * @throws BusinessException if stock is insufficient
     */
    @CacheEvict(value = "products", key = "#productId", beforeInvocation = true)
    public void deductStock(Long productId, Integer quantity) {
        log.info("🗑️  [CACHE] deductStock({}, {}) - 開始執行，已清除快取 key='products::{}'",
            productId, quantity, productId);
        int rowsAffected = productRepository.deductStock(productId, quantity);
        if (rowsAffected == 0) {
            // Stock deduction failed - either insufficient stock or product not found
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            throw new BusinessException("Insufficient stock for product: " + product.getName() +
                    " (requested: " + quantity + ", available: " + product.getStock() + ")");
        }
        log.info("✅ [CACHE] deductStock({}) - 庫存已扣減 -{}, 快取已清除", productId, quantity);
    }

    @CacheEvict(value = "products", key = "#productId", beforeInvocation = true)
    public void restoreStock(Long productId, Integer quantity) {
        log.info("🔄 [CACHE] restoreStock({}, {}) - 已清除快取 key='products::{}'",
            productId, quantity, productId);
        int rowsAffected = productRepository.restoreStock(productId, quantity);
        if (rowsAffected == 0) {
            throw new ResourceNotFoundException("Product not found or failed to restore stock");
        }
        log.info("✅ [CACHE] restoreStock({}) - 庫存已還原 +{}", productId, quantity);
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setStatus(product.getStatus().name());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }
}