/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String response;
    private String sessionId;

    public ChatResponse() {
    }

    public ChatResponse(String response, Long timestamp) {
        this.response = response;
        this.sessionId = String.valueOf(timestamp);
    }
}