/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.ProductDTO;
import com.etrex.oms.entity.Product;
import com.etrex.oms.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDTO testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new ProductDTO();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(100);
        testProduct.setStatus("ACTIVE");
    }

    @Test
    void getProducts_Success() throws Exception {
        // Given
        Page<ProductDTO> page = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.getProducts(any(), any(Product.Status.class), any()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/product")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.content[0].price").value(99.99));
    }

    @Test
    void getProducts_WithKeyword() throws Exception {
        // Given
        Page<ProductDTO> page = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.getProducts(eq("Test"), any(Product.Status.class), any()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/product")
                        .param("keyword", "Test")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());

        verify(productService).getProducts(eq("Test"), any(Product.Status.class), any());
    }

    @Test
    void getProduct_Success() throws Exception {
        // Given
        when(productService.getProductById(1L)).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(get("/api/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void createProduct_Success() throws Exception {
        // Given
        when(productService.createProduct(any(ProductDTO.class))).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void updateProduct_Success() throws Exception {
        // Given
        when(productService.updateProduct(eq(1L), any(ProductDTO.class))).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(put("/api/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void deleteProduct_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/product/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }
}
