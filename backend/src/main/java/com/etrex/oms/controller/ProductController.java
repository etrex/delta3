/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.ProductDTO;
import com.etrex.oms.entity.Product;
import com.etrex.oms.service.ChatHistoryService;
import com.etrex.oms.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {
    private final ProductService productService;
    private final ChatHistoryService chatHistoryService;

    @GetMapping
    @Operation(summary = "Get all products", description = "Get paginated list of products")
    public ResponseEntity<Page<ProductDTO>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "true") boolean tracking,
            @RequestParam(required = false) String context,
            Pageable pageable) {

        // Check if user is admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Determine product status filter
        Product.Status productStatus;
        if (status != null) {
            productStatus = Product.Status.valueOf(status);
        } else if (!isAdmin) {
            // Non-admin users can only see ACTIVE products by default
            productStatus = Product.Status.ACTIVE;
        } else {
            // Admin can see all products by default
            productStatus = null;
        }

        Page<ProductDTO> result = productService.getProducts(keyword, productStatus, pageable);

        // Track operation only if tracking=true
        if (tracking && context != null) {
            String message = switch (context.toLowerCase()) {
                case "dashboard" -> "查看儀表板";
                case "products" -> "查看商品管理頁面";
                case "customer_products" -> "瀏覽商品列表";
                default -> "瀏覽商品列表";
            };
            chatHistoryService.track(message);
        } else if (tracking) {
            chatHistoryService.track("瀏覽商品列表");
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Get single product details")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id) {
        ProductDTO result = productService.getProductById(id);

        // Track operation
        chatHistoryService.track(
            String.format("查看商品詳情 (商品 ID: %d)", id));

        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Create new product (Admin only)")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO result = productService.createProduct(productDTO);

        // Track operation with product details
        chatHistoryService.track(
            String.format("新增商品 (商品 ID: %d, 名稱: %s, 價格: %d)",
                result.getId(), result.getName(), result.getPrice()));

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", description = "Update existing product (Admin only)")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO result = productService.updateProduct(id, productDTO);

        // Track operation with product details
        chatHistoryService.track(
            String.format("更新商品 (商品 ID: %d, 名稱: %s)", result.getId(), result.getName()));

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Soft delete product (Admin only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);

        // Track operation
        chatHistoryService.track(
            String.format("刪除商品 (商品 ID: %d)", id));

        return ResponseEntity.noContent().build();
    }
}