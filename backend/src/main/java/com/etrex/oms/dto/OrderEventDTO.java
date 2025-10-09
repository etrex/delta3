/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderEventDTO {
    private Long id;
    private Long orderId;
    private String eventType;
    private String message;
    private Long modifiedBy;
    private String modifiedByUsername;
    private LocalDateTime createdAt;
}
