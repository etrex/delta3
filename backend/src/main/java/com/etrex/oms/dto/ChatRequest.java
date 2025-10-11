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
}