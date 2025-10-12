/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.OrderDTO;
import com.etrex.oms.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for building dynamic context to append at the end of AI prompts.
 * This context includes real-time information like shopping cart and current page.
 *
 * Example output:
 * <pre>
 * ## 當前狀態資訊 (供參考，請根據用戶問題決定是否需要使用)
 *
 * ### 購物車狀態
 * 商品數量: 2 件
 * 總金額: NT$ 25000
 * 商品清單:
 *   - iPhone 15 Pro x 1 (NT$ 15000)
 *   - AirPods Pro x 2 (NT$ 10000)
 *
 * ### 用戶當前頁面
 * 路徑: /products
 * 頁面: 商品列表
 * 類型: product_list
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ChatContextService {
    private final OrderService orderService;

    /**
     * Build dynamic context string to append at the END of the prompt.
     * This maximizes cache hit rate as fixed system prompt and conversation history remain unchanged.
     *
     * Structure:
     * 1. [Fixed System Prompt]    ← Always cached
     * 2. [Conversation History]   ← Mostly cached (grows incrementally)
     * 3. [New User Message]       ← New content
     * 4. [Dynamic Context]        ← This method generates this (always new)
     *
     * @param user Current user
     * @param pageContext Optional page context from frontend
     * @return Context string to append
     */
    public String buildDynamicContext(User user, ChatRequest.PageContext pageContext) {
        StringBuilder context = new StringBuilder();
        context.append("\n\n## 當前狀態資訊 (供參考，請根據用戶問題決定是否需要使用)\n\n");

        // 1. Shopping cart info
        String cartInfo = buildCartContext(user);
        if (cartInfo != null) {
            context.append(cartInfo);
        }

        // 2. Current page info
        if (pageContext != null) {
            String pageInfo = buildPageContext(pageContext);
            if (pageInfo != null) {
                context.append(pageInfo);
            }
        }

        return context.toString();
    }

    /**
     * Build shopping cart context
     */
    private String buildCartContext(User user) {
        try {
            OrderDTO cart = orderService.getOrCreateCart(user);

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                return "### 購物車狀態\n購物車目前是空的\n\n";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("### 購物車狀態\n");
            sb.append(String.format("商品數量: %d 件\n", cart.getItems().size()));
            sb.append(String.format("總金額: NT$ %d\n", cart.getTotalAmount()));
            sb.append("商品清單:\n");

            cart.getItems().forEach(item -> {
                sb.append(String.format("  - %s x %d (NT$ %d)\n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice() * item.getQuantity()));
            });
            sb.append("\n");

            return sb.toString();
        } catch (Exception e) {
            // If cart query fails, don't break the chat
            return "### 購物車狀態\n無法取得購物車資訊\n\n";
        }
    }

    /**
     * Build page context
     */
    private String buildPageContext(ChatRequest.PageContext pageContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 用戶當前頁面\n");

        if (pageContext.getPath() != null) {
            sb.append(String.format("路徑: %s\n", pageContext.getPath()));
        }

        if (pageContext.getTitle() != null) {
            sb.append(String.format("頁面: %s\n", pageContext.getTitle()));
        }

        if (pageContext.getPageType() != null) {
            sb.append(String.format("類型: %s\n", pageContext.getPageType()));
        }

        // Optional: include page-specific data
        if (pageContext.getData() != null) {
            sb.append(String.format("頁面資料: %s\n", pageContext.getData().toString()));
        }

        sb.append("\n");
        return sb.toString();
    }
}
