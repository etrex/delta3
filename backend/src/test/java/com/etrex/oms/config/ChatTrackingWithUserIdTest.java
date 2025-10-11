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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test to verify that ChatTrackingInterceptor records operations with user.getId() as sessionId
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatTrackingWithUserIdTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatHistoryService chatHistoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("customer1");
        testUser.setRole(User.Role.CUSTOMER);

        // Clean up
        chatHistoryRepository.deleteAll();
    }

    @Test
    void testOperationsAreRecordedWithUserIdAsSessionId() throws Exception {
        // Given: User ID is 1
        Long userId = testUser.getId();

        // When: User adds product to cart
        String requestBody = """
            {
                "productId": 1,
                "quantity": 2
            }
            """;

        mockMvc.perform(post("/api/orders/cart/items")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Then: History should be stored with sessionId = "1" (user.getId() as String)
        String expectedSessionId = String.valueOf(userId);
        List<ChatHistory> history = chatHistoryService.getHistory(expectedSessionId);

        System.out.println("🔍 Looking for sessionId: " + expectedSessionId);
        System.out.println("📊 Found " + history.size() + " records");

        // Debug: print all records
        List<ChatHistory> allRecords = chatHistoryRepository.findAll();
        System.out.println("📊 Total records in DB: " + allRecords.size());
        allRecords.forEach(r -> {
            System.out.println("  - SessionId: " + r.getSessionId() +
                             ", UserId: " + r.getUserId() +
                             ", Content: " + r.getContent());
        });

        assertFalse(history.isEmpty(), "History should not be empty for sessionId=" + expectedSessionId);

        ChatHistory action = history.get(0);
        assertEquals(expectedSessionId, action.getSessionId(), "SessionId should be user.getId() as String");
        assertEquals(userId, action.getUserId(), "UserId should match");
        assertEquals(ChatHistory.MessageType.ACTION.name(), action.getMessageType());
        assertTrue(action.getContent().contains("加入商品到購物車"));

        System.out.println("✅ Successfully recorded operation with sessionId=" + expectedSessionId);
        System.out.println("   Content: " + action.getContent());
    }

    @Test
    void testMultipleOperationsForSameUser() throws Exception {
        // When: User performs multiple operations
        mockMvc.perform(post("/api/orders/cart/items")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": 1}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/orders/cart/items/1")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk());

        // Then: All operations should be under the same sessionId (user.getId())
        String sessionId = String.valueOf(testUser.getId());
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);

        assertEquals(2, history.size(), "Should have 2 operations for user " + sessionId);

        System.out.println("✅ All operations for user " + sessionId + ":");
        history.forEach(h -> System.out.println("  - " + h.getContent()));
    }
}
