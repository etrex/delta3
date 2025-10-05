/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void testGettersAndSetters() {
        Product product = new Product();

        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(100);
        product.setStockThreshold(10);
        product.setStatus(Product.Status.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);

        assertEquals(1L, product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals("Test Description", product.getDescription());
        assertEquals(new BigDecimal("99.99"), product.getPrice());
        assertEquals(100, product.getStock());
        assertEquals(10, product.getStockThreshold());
        assertEquals(Product.Status.ACTIVE, product.getStatus());
        assertEquals(now, product.getCreatedAt());
    }

    @Test
    void testStatusEnum() {
        assertEquals("ACTIVE", Product.Status.ACTIVE.name());
        assertEquals("INACTIVE", Product.Status.INACTIVE.name());
        assertEquals(Product.Status.ACTIVE, Product.Status.valueOf("ACTIVE"));
        assertEquals(Product.Status.INACTIVE, Product.Status.valueOf("INACTIVE"));
    }

    @Test
    void testNoArgsConstructor() {
        Product product = new Product();
        assertNotNull(product);
        assertNull(product.getId());
    }
}
