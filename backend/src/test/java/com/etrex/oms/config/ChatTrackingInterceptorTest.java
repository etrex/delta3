/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.User;
import com.etrex.oms.repository.ChatHistoryRepository;
import com.etrex.oms.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatTrackingInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatHistoryService chatHistoryService;

    private User testUser;
    private String sessionId;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("customer1");
        testUser.setRole(User.Role.CUSTOMER);

        sessionId = "test-session-" + System.currentTimeMillis();

        // Clean up
        chatHistoryRepository.deleteAll();
    }

    @Test
    void testAddToCartTracking() throws Exception {
        // Given: Add product to cart
        String requestBody = """
            {
                "productId": 1,
                "quantity": 2
            }
            """;

        // When: Call API with session ID header
        mockMvc.perform(post("/api/orders/cart/items")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Then: Check if action was recorded
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);

        assertFalse(history.isEmpty(), "History should not be empty");

        ChatHistory action = history.get(0);
        assertEquals(sessionId, action.getSessionId());
        assertEquals(testUser.getId(), action.getUserId());
        assertEquals(ChatHistory.MessageType.ACTION.name(), action.getMessageType());
        assertEquals("api_call", action.getActionType());
        assertTrue(action.getContent().contains("加入商品到購物車"),
                "Content should be: " + action.getContent());

        System.out.println("✅ Tracked action: " + action.getContent());
    }

    @Test
    void testUpdateCartTracking() throws Exception {
        // When: Update cart item
        String requestBody = """
            {
                "quantity": 5
            }
            """;

        mockMvc.perform(put("/api/orders/cart/items/1")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Then: Check if action was recorded
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);

        assertFalse(history.isEmpty(), "History should not be empty");

        ChatHistory action = history.get(0);
        assertTrue(action.getContent().contains("更新購物車數量"),
                "Content should contain '更新購物車數量', but was: " + action.getContent());

        System.out.println("✅ Tracked action: " + action.getContent());
    }

    @Test
    void testDeleteFromCartTracking() throws Exception {
        // When: Delete cart item
        mockMvc.perform(delete("/api/orders/cart/items/1")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId))
                .andExpect(status().isOk());

        // Then: Check if action was recorded
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);

        assertFalse(history.isEmpty(), "History should not be empty");

        ChatHistory action = history.get(0);
        assertTrue(action.getContent().contains("從購物車移除商品"),
                "Content should contain '從購物車移除商品', but was: " + action.getContent());

        System.out.println("✅ Tracked action: " + action.getContent());
    }

    @Test
    void testCreateOrderTracking() throws Exception {
        // Given: Create order
        String requestBody = """
            {
                "items": [
                    {"productId": 1, "quantity": 2}
                ]
            }
            """;

        // When: Call API
        mockMvc.perform(post("/api/orders")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Then: Check if action was recorded
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);

        assertFalse(history.isEmpty(), "History should not be empty");

        ChatHistory action = history.get(0);
        assertTrue(action.getContent().contains("建立新訂單"),
                "Content should contain '建立新訂單', but was: " + action.getContent());

        System.out.println("✅ Tracked action: " + action.getContent());
    }

    @Test
    void testChatAPINotTracked() throws Exception {
        // Given: Chat API call (should be excluded)
        String requestBody = """
            {
                "message": "Hello",
                "sessionId": "test-session"
            }
            """;

        // When: Call chat API
        mockMvc.perform(post("/api/chat")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Then: Chat API should NOT be tracked as action
        List<ChatHistory> history = chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Should have USER message and ASSISTANT response, but no ACTION for the API call
        long actionCount = history.stream()
                .filter(h -> h.getMessageType().equals(ChatHistory.MessageType.ACTION.name()))
                .filter(h -> "api_call".equals(h.getActionType()))
                .count();

        assertEquals(0, actionCount, "Chat API should not be tracked as action");

        System.out.println("✅ Chat API correctly excluded from tracking");
    }

    @Test
    void testMultipleOperationsInSequence() throws Exception {
        // Simulate user flow: add to cart -> update -> checkout

        // 1. Add to cart
        mockMvc.perform(post("/api/orders/cart/items")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": 1}"))
                .andExpect(status().isOk());

        // 2. Update quantity
        mockMvc.perform(put("/api/orders/cart/items/1")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk());

        // 3. Checkout
        mockMvc.perform(post("/api/orders/cart/checkout")
                        .with(user(testUser))
                        .header("X-Chat-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then: All operations should be tracked
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);

        assertEquals(3, history.size(), "Should have 3 tracked actions");

        assertTrue(history.get(0).getContent().contains("加入商品到購物車"));
        assertTrue(history.get(1).getContent().contains("更新購物車數量"));
        assertTrue(history.get(2).getContent().contains("結帳購物車"));

        System.out.println("✅ All operations tracked in sequence:");
        history.forEach(h -> System.out.println("  - " + h.getContent()));
    }
}
