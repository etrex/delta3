/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductDTOTest {

    @Test
    void testGettersAndSetters() {
        ProductDTO dto = new ProductDTO();

        dto.setId(1L);
        dto.setName("Test Product");
        dto.setDescription("Test Description");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setStock(100);
        dto.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals("Test Product", dto.getName());
        assertEquals("Test Description", dto.getDescription());
        assertEquals(new BigDecimal("99.99"), dto.getPrice());
        assertEquals(100, dto.getStock());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        ProductDTO dto = new ProductDTO();
        assertNotNull(dto);
    }
}
