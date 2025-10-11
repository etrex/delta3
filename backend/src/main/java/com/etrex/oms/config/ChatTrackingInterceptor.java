/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import com.etrex.oms.entity.User;
import com.etrex.oms.service.ChatHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to automatically track user operations for chat history
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatTrackingInterceptor implements HandlerInterceptor {
    private final ChatHistoryService chatHistoryService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip tracking for chat API itself
        String path = request.getRequestURI();
        if (path.contains("/api/chat")) {
            return true;
        }

        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            return true;
        }

        User user = (User) auth.getPrincipal();
        String method = request.getMethod();

        // Use user ID as session ID
        String sessionId = String.valueOf(user.getId());

        try {
            // Generate operation description
            String operationDesc = generateOperationDescription(method, path);

            // Record the operation
            chatHistoryService.saveAction(sessionId, user.getId(), "api_call", operationDesc);

            log.debug("Tracked operation: {} {} for user {}", method, path, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to track operation", e);
            // Don't fail the request if tracking fails
        }

        return true;
    }

    /**
     * Generate human-readable operation description
     */
    private String generateOperationDescription(String method, String path) {
        // Remove /api prefix
        String normalizedPath = path.replace("/api", "");

        // GET operations
        if (method.equals("GET")) {
            if (normalizedPath.equals("/products")) {
                return "瀏覽商品列表";
            } else if (normalizedPath.matches("/products/\\d+")) {
                return "查看商品詳情";
            } else if (normalizedPath.equals("/orders")) {
                return "查看訂單列表";
            } else if (normalizedPath.matches("/orders/\\d+")) {
                return "查看訂單詳情";
            } else if (normalizedPath.matches("/orders/by-order-no/.+")) {
                return "查看訂單詳情";
            } else if (normalizedPath.equals("/orders/cart")) {
                return "查看購物車";
            } else {
                return String.format("查看 %s", normalizedPath);
            }
        }

        // POST operations
        if (normalizedPath.startsWith("/orders/cart/items") && method.equals("POST")) {
            return "加入商品到購物車";
        } else if (normalizedPath.matches("/orders/cart/items/\\d+") && method.equals("PUT")) {
            return "更新購物車數量";
        } else if (normalizedPath.matches("/orders/cart/items/\\d+") && method.equals("DELETE")) {
            return "從購物車移除商品";
        } else if (normalizedPath.startsWith("/orders/cart/checkout") && method.equals("POST")) {
            return "結帳購物車";
        } else if (normalizedPath.equals("/orders") && method.equals("POST")) {
            return "建立新訂單";
        } else if (normalizedPath.matches("/orders/\\d+/pay") && method.equals("POST")) {
            return "支付訂單";
        } else if (normalizedPath.matches("/orders/\\d+/cancel") && method.equals("POST")) {
            return "取消訂單";
        } else if (normalizedPath.matches("/orders/\\d+/ship") && method.equals("POST")) {
            return "出貨訂單";
        } else if (normalizedPath.matches("/orders/by-order-no/.+/approve") && method.equals("POST")) {
            return "批准訂單";
        } else if (normalizedPath.matches("/orders/by-order-no/.+/deliver") && method.equals("POST")) {
            return "完成配送";
        } else if (normalizedPath.equals("/products") && method.equals("POST")) {
            return "新增商品";
        } else if (normalizedPath.matches("/products/\\d+") && method.equals("PUT")) {
            return "更新商品";
        } else if (normalizedPath.matches("/products/\\d+") && method.equals("DELETE")) {
            return "刪除商品";
        } else {
            // Generic description
            return String.format("%s %s", getMethodName(method), normalizedPath);
        }
    }

    private String getMethodName(String method) {
        return switch (method) {
            case "GET" -> "查看";
            case "POST" -> "建立";
            case "PUT" -> "更新";
            case "DELETE" -> "刪除";
            default -> method;
        };
    }
}
