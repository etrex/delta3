package com.etrex.oms.acceptance.auth;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthenticationTest extends BaseAcceptanceTest {

    // 1. 測試身分認證 - Customer/Admin 登入
    @Test
    void testAuthentication() throws Exception {
        // Admin 登入
        AuthRequest adminRequest = new AuthRequest();
        adminRequest.setUsername("admin");
        adminRequest.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Customer 登入
        AuthRequest customerRequest = new AuthRequest();
        customerRequest.setUsername("customer1");
        customerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}