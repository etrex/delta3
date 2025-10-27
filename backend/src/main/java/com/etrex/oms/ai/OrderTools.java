/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import com.etrex.oms.dto.ProductDTO;
import com.etrex.oms.dto.OrderDTO;
import com.etrex.oms.entity.Product;
import com.etrex.oms.entity.User;
import com.etrex.oms.service.ProductService;
import com.etrex.oms.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderTools {
    private final ProductService productService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * Get the currently authenticated user from SecurityContext
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    @Tool("透過關鍵字來搜尋商品")
    public String searchProducts(String keyword) {
        try {
            Page<ProductDTO> products = productService.getProducts(keyword, Product.Status.ACTIVE, PageRequest.of(0, 10));
            return objectMapper.writeValueAsString(products.getContent());
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize products\"}";
        } catch (Exception e) {
            return "{\"error\": \"查詢商品時發生錯誤\"}";
        }
    }

    @Tool("以商品 ID 來取得商品資訊")
    public String getProductDetails(Long productId) {
        try {
            ProductDTO product = productService.getProductById(productId);
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize product\"}";
        } catch (Exception e) {
            return "{\"error\": \"無法找到該商品\"}";
        }
    }

    @Tool("取得當前用戶的訂單列表")
    public String getMyOrders(String status) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "{\"error\": \"無法取得使用者資訊，請先登入\"}";
            }

            Page<OrderDTO> orders = orderService.getOrders(
                user.getId(),
                status,
                PageRequest.of(0, 20)
            );

            return objectMapper.writeValueAsString(orders.getContent());
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize orders\"}";
        } catch (Exception e) {
            return String.format("{\"error\": \"查詢訂單列表失敗: %s\"}", e.getMessage());
        }
    }

    @Tool("以訂單 ID 來取得訂單資訊")
    public String getOrderDetails(Long orderId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "{\"error\": \"無法取得使用者資訊，請先登入\"}";
            }

            OrderDTO order = orderService.getOrderById(orderId);

            // Authorization check: user can only view their own orders
            if (!order.getCustomerId().equals(user.getId())) {
                return "{\"error\": \"權限不足：您只能查看自己的訂單\"}";
            }

            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize order\"}";
        } catch (Exception e) {
            return "{\"error\": \"無法找到該訂單\"}";
        }
    }

    @Tool("確認一個商品是否有足夠的貨")
    public String checkStock(Long productId, Integer quantity) {
        try {
            boolean available = productService.checkStock(productId, quantity);
            ProductDTO product = productService.getProductById(productId);

            Map<String, Object> result = new HashMap<>();
            result.put("available", available);
            result.put("productId", productId);
            result.put("productName", product.getName());
            result.put("requestedQuantity", quantity);
            result.put("currentStock", product.getStock());

            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize stock check result\"}";
        } catch (Exception e) {
            return "{\"error\": \"無法查詢庫存資訊\"}";
        }
    }

    @Tool("將商品加入購物車")
    public String addToCart(Long productId, Integer quantity) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "{\"error\": \"無法取得使用者資訊，請先登入\"}";
            }

            if (quantity == null || quantity <= 0) {
                return "{\"error\": \"請輸入有效的數量（必須大於 0）\"}";
            }

            OrderDTO cart = orderService.addToCart(user, productId, quantity);
            return objectMapper.writeValueAsString(cart);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize cart\"}";
        } catch (Exception e) {
            return String.format("{\"error\": \"加入購物車失敗: %s\"}", e.getMessage());
        }
    }

    @Tool("結帳購物車，將購物車轉換為訂單")
    public String checkoutCart() {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "{\"error\": \"無法取得使用者資訊，請先登入\"}";
            }

            OrderDTO order = orderService.checkoutCart(user);
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize order\"}";
        } catch (Exception e) {
            return String.format("{\"error\": \"結帳失敗: %s\"}", e.getMessage());
        }
    }

    @Tool("取消訂單，會退還商品庫存")
    public String cancelOrder(Long orderId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "{\"error\": \"無法取得使用者資訊，請先登入\"}";
            }

            // Authorization check: first get the order to verify ownership
            OrderDTO order = orderService.getOrderById(orderId);
            if (!order.getCustomerId().equals(user.getId())) {
                return "{\"error\": \"權限不足：您只能取消自己的訂單\"}";
            }

            // Proceed with cancellation
            OrderDTO cancelledOrder = orderService.cancelOrder(orderId);
            return objectMapper.writeValueAsString(cancelledOrder);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize cancelled order\"}";
        } catch (Exception e) {
            return String.format("{\"error\": \"取消訂單失敗: %s\"}", e.getMessage());
        }
    }
}