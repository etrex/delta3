/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.PaymentDTO;
import com.etrex.oms.dto.PaymentRequest;
import com.etrex.oms.entity.User;
import com.etrex.oms.service.ChatHistoryService;
import com.etrex.oms.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private ChatHistoryService chatHistoryService;

    private PaymentDTO testPayment;
    private User testUser;

    @BeforeEach
    void setUp() {
        testPayment = new PaymentDTO();
        testPayment.setId(1L);
        testPayment.setOrderId(1L);
        testPayment.setPaymentMethod("CREDIT_CARD");
        testPayment.setAmount(100);
        testPayment.setStatus("SUCCESS");
        testPayment.setTransactionId("TXN-001");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.CUSTOMER);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createPayment_Success() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setPaymentMethod("CREDIT_CARD");
        request.setAmount(100);
        request.setCardExpiry("12/25");
        request.setCardCvv("123");
        request.setCardName("Test User");

        when(paymentService.initiatePayment(eq(1L), any(PaymentRequest.class))).thenReturn(testPayment);

        // When & Then
        mockMvc.perform(post("/api/orders/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));
    }
}
