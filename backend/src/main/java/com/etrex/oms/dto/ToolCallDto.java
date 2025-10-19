/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool Call DTO
 * Records information about tool executions during AI response generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallDto {
    /**
     * Tool name (e.g., "getOrderById", "searchProducts")
     */
    private String toolName;

    /**
     * Tool arguments in JSON format
     */
    private String arguments;

    /**
     * Tool execution result
     */
    private String result;

    /**
     * Tool execution time in milliseconds
     */
    private Integer executionTime;
}
