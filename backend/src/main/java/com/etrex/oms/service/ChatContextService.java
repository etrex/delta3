/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.OrderDTO;
import com.etrex.oms.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
    private final ObjectMapper objectMapper;

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
        context.append("(以下為用戶當下的狀態)");

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
     * Build shopping cart context in JSON format
     */
    private String buildCartContext(User user) {
        try {
            OrderDTO cart = orderService.getOrCreateCart(user);
            String cartJson = objectMapper.writeValueAsString(cart);

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n# 目前購物車:\n");
            sb.append("```json\n");
            sb.append(cartJson);
            sb.append("\n```\n");

            return sb.toString();
        } catch (Exception e) {
            // If cart query fails, don't break the chat
            return "\n\n# 目前購物車:\n無法取得購物車資訊\n";
        }
    }

    /**
     * Build page context in JSON format
     */
    private String buildPageContext(ChatRequest.PageContext pageContext) {
        try {
            Map<String, Object> pageData = new HashMap<>();

            if (pageContext.getPath() != null) {
                pageData.put("path", pageContext.getPath());
            }

            if (pageContext.getTitle() != null) {
                pageData.put("title", pageContext.getTitle());
            }

            if (pageContext.getPageType() != null) {
                pageData.put("pageType", pageContext.getPageType());
            }

            if (pageContext.getData() != null) {
                pageData.put("data", pageContext.getData());
            }

            String pageJson = objectMapper.writeValueAsString(pageData);

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n# 用戶當前頁面:\n");
            sb.append("```json\n");
            sb.append(pageJson);
            sb.append("\n```\n");

            return sb.toString();
        } catch (Exception e) {
            return "\n\n# 用戶當前頁面:\n無法取得頁面資訊\n";
        }
    }
}
