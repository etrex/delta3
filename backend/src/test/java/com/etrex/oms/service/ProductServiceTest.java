/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.ProductDTO;
import com.etrex.oms.entity.Product;
import com.etrex.oms.exception.BusinessException;
import com.etrex.oms.exception.ResourceNotFoundException;
import com.etrex.oms.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDTO testProductDTO;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(1000);
        testProduct.setStock(100);
        testProduct.setStatus(Product.Status.ACTIVE);
        testProduct.setCreatedAt(LocalDateTime.now());

        testProductDTO = new ProductDTO();
        testProductDTO.setId(1L);
        testProductDTO.setName("Test Product");
        testProductDTO.setDescription("Test Description");
        testProductDTO.setPrice(1000);
        testProductDTO.setStock(100);
        testProductDTO.setStatus("ACTIVE");
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductDTO result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getName(), result.getName());
        assertEquals(testProduct.getDescription(), result.getDescription());
        assertEquals(testProduct.getPrice(), result.getPrice());
        assertEquals(testProduct.getStock(), result.getStock());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById(999L);
        });
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    void getProducts_WithKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.searchByKeywordAndStatus("test", Product.Status.ACTIVE, pageable))
                .thenReturn(productPage);

        Page<ProductDTO> result = productService.getProducts("test", Product.Status.ACTIVE, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Product", result.getContent().get(0).getName());
        verify(productRepository, times(1)).searchByKeywordAndStatus("test", Product.Status.ACTIVE, pageable);
    }

    @Test
    void getProducts_WithoutKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findByStatus(Product.Status.ACTIVE, pageable))
                .thenReturn(productPage);

        Page<ProductDTO> result = productService.getProducts(null, Product.Status.ACTIVE, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByStatus(Product.Status.ACTIVE, pageable);
    }

    @Test
    void getProducts_WithEmptyKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findByStatus(Product.Status.ACTIVE, pageable))
                .thenReturn(productPage);

        Page<ProductDTO> result = productService.getProducts("", Product.Status.ACTIVE, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByStatus(Product.Status.ACTIVE, pageable);
    }

    @Test
    void createProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.createProduct(testProductDTO);

        assertNotNull(result);
        assertEquals(testProductDTO.getName(), result.getName());
        assertEquals(testProductDTO.getPrice(), result.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_Success() {
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("Updated Product");
        updateDTO.setDescription("Updated Description");
        updateDTO.setPrice(2000);
        updateDTO.setStock(200);
        updateDTO.setStatus("ACTIVE");

        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setPrice(2000);
        updatedProduct.setStock(200);
        updatedProduct.setStatus(Product.Status.ACTIVE);
        updatedProduct.setCreatedAt(LocalDateTime.now());

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        ProductDTO result = productService.updateProduct(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Updated Product", result.getName());
        assertEquals(2000, result.getPrice());
        assertEquals(200, result.getStock());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_Success_Alternative() {
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("Updated Product");
        updateDTO.setDescription("Updated Description");
        updateDTO.setPrice(2000);
        updateDTO.setStock(200);
        updateDTO.setStatus("ACTIVE");

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.updateProduct(1L, updateDTO);

        assertNotNull(result);
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.updateProduct(999L, testProductDTO);
        });
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
        assertEquals(Product.Status.INACTIVE, testProduct.getStatus());
    }

    @Test
    void deleteProduct_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.deleteProduct(999L);
        });
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deductStock_Success() {
        when(productRepository.deductStock(1L, 30)).thenReturn(1);

        productService.deductStock(1L, 30);

        verify(productRepository, times(1)).deductStock(1L, 30);
        verify(productRepository, never()).findById(any());
    }

    @Test
    void deductStock_InsufficientStock() {
        when(productRepository.deductStock(1L, 200)).thenReturn(0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            productService.deductStock(1L, 200);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("requested: 200"));
        assertTrue(exception.getMessage().contains("available: 100"));
        verify(productRepository, times(1)).deductStock(1L, 200);
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void deductStock_ProductNotFound() {
        when(productRepository.deductStock(999L, 10)).thenReturn(0);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.deductStock(999L, 10);
        });
        verify(productRepository, times(1)).deductStock(999L, 10);
        verify(productRepository, times(1)).findById(999L);
    }
}
