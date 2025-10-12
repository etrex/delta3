/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;

    private String sessionId;

    // For action recording
    private String actionType;    // navigate, click, submit, etc.
    private String actionTarget;  // path, button id, etc.

    // Page context for better AI understanding
    private PageContext pageContext;

    @Data
    public static class PageContext {
        private String path;           // Current page path, e.g., "/products"
        private String title;          // Page title for context
        private String pageType;       // e.g., "product_list", "product_detail", "checkout"
        private Object data;           // Optional: key data from current page
    }
}