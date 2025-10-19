/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.ToolCallDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool Call Collector Service
 * Uses ThreadLocal to temporarily store tool call records during AI response generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallCollector {

    private final ObjectMapper objectMapper;

    private static final ThreadLocal<List<ToolCallDto>> toolCalls = ThreadLocal.withInitial(ArrayList::new);

    /**
     * Add a tool call record
     */
    public void addToolCall(String toolName, String arguments, String result, Integer executionTime) {
        ToolCallDto toolCall = ToolCallDto.builder()
                .toolName(toolName)
                .arguments(arguments)
                .result(result)
                .executionTime(executionTime)
                .build();

        toolCalls.get().add(toolCall);
        log.debug("Added tool call: {} (execution time: {}ms)", toolName, executionTime);
    }

    /**
     * Get all tool calls for the current thread
     */
    public List<ToolCallDto> getToolCalls() {
        return new ArrayList<>(toolCalls.get());
    }

    /**
     * Clear all tool calls for the current thread
     * Should be called at the start of each request
     */
    public void clear() {
        toolCalls.get().clear();
        log.debug("Cleared tool calls for current thread");
    }

    /**
     * Convert tool calls to JSON string
     */
    public String toJson() {
        try {
            List<ToolCallDto> calls = toolCalls.get();
            if (calls.isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(calls);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert tool calls to JSON", e);
            return null;
        }
    }

    /**
     * Get the number of tool calls
     */
    public int getToolCallCount() {
        return toolCalls.get().size();
    }

    /**
     * Check if there are any tool calls
     */
    public boolean hasToolCalls() {
        return !toolCalls.get().isEmpty();
    }

    /**
     * Remove the ThreadLocal value to prevent memory leaks
     * Should be called in a finally block or filter
     */
    public void remove() {
        toolCalls.remove();
        log.debug("Removed ThreadLocal tool calls");
    }
}
