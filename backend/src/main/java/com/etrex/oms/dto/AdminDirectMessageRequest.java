/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.Data;

/**
 * Request for admin to send direct message to user
 */
@Data
public class AdminDirectMessageRequest {
    private String sessionId;
    private Long userId;
    private String text;
}
