/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.User;
import com.etrex.oms.repository.ChatHistoryRepository;
import com.etrex.oms.repository.UserRepository;
import com.etrex.oms.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private String adminToken;
    private User customerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Clear chat history
        chatHistoryRepository.deleteAll();

        // Create customer user
        customerUser = new User();
        customerUser.setUsername("customer_tracking_test");
        customerUser.setEmail("customer@test.com");
        customerUser.setPassword(passwordEncoder.encode("password"));
        customerUser.setRole(User.Role.CUSTOMER);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtUtil.generateToken(customerUser.getUsername(), "CUSTOMER");

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin_tracking_test");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getUsername(), "ADMIN");
    }

    @Test
    void getProducts_ShouldTrack瀏覽商品列表() throws Exception {
        // When
        mockMvc.perform(get("/api/product")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Then - 驗證有追蹤記錄
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(customerUser.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getContent()).contains("瀏覽商品列表");
    }

    @Test
    void getProductById_ShouldTrack查看商品詳情() throws Exception {
        // When
        mockMvc.perform(get("/api/product/1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Then - 驗證有追蹤記錄包含商品ID
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(customerUser.getId());
        assertThat(history).isNotEmpty();
        String lastContent = history.get(0).getContent();
        assertThat(lastContent).contains("查看商品詳情");
        assertThat(lastContent).contains("商品 ID: 1");
    }

    @Test
    void getOrders_ShouldTrack查看訂單列表() throws Exception {
        // When
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Then - 驗證有追蹤記錄
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(customerUser.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getContent()).contains("查看訂單列表");
    }

    // Note: getOrderById test 需要實際的訂單資料，會在後續的 E2E 測試中驗證

    @Test
    void getCart_ShouldTrack查看購物車() throws Exception {
        // When
        mockMvc.perform(get("/api/orders/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Then - 驗證有追蹤記錄
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(customerUser.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getContent()).contains("查看購物車");
    }

    @Test
    void adminGetProducts_ShouldAlsoTrack() throws Exception {
        // When
        mockMvc.perform(get("/api/product")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Then - Admin 也應該有追蹤記錄
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(adminUser.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getContent()).contains("瀏覽商品列表");
    }
}
