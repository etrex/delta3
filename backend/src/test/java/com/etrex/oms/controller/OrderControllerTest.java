/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.CreateOrderRequest;
import com.etrex.oms.dto.OrderDTO;
import com.etrex.oms.dto.PaymentDTO;
import com.etrex.oms.entity.User;
import com.etrex.oms.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDTO testOrder;
    private PaymentDTO testPayment;
    private User testUser;

    @BeforeEach
    void setUp() {
        testOrder = new OrderDTO();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD-001");
        testOrder.setCustomerId(1L);
        testOrder.setCustomerName("Test Customer");
        testOrder.setTotalAmount(new BigDecimal("199.99"));
        testOrder.setStatus("CREATED");

        testPayment = new PaymentDTO();
        testPayment.setId(1L);
        testPayment.setAmount(new BigDecimal("199.99"));
        testPayment.setStatus("SUCCESS");
        testPayment.setPaymentMethod("CREDIT_CARD");

        // Setup security context with User
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.CUSTOMER);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getOrders_Success() throws Exception {
        // Given
        Page<OrderDTO> page = new PageImpl<>(Arrays.asList(testOrder));
        when(orderService.getOrders(any(), any(), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNo").value("ORD-001"));
    }

    @Test
    void getOrders_WithFilters() throws Exception {
        // Given
        Page<OrderDTO> page = new PageImpl<>(Arrays.asList(testOrder));
        when(orderService.getOrders(eq(1L), eq("CREATED"), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("customerId", "1")
                        .param("status", "CREATED"))
                .andExpect(status().isOk());

        verify(orderService).getOrders(eq(1L), eq("CREATED"), any());
    }

    @Test
    void getOrder_Success() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNo").value("ORD-001"));
    }

    @Test
    void getOrderByOrderNo_Success() throws Exception {
        // Given
        when(orderService.getOrderByOrderNo("ORD-001")).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/api/orders/by-order-no/ORD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("ORD-001"));
    }

    @Test
    void createOrder_Success() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(Arrays.asList(item));

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNo").value("ORD-001"));
    }

    @Test
    void payOrder_Success() throws Exception {
        // Given
        when(orderService.initiatePayment(eq(1L), any(PaymentDTO.class))).thenReturn(testPayment);

        // When & Then
        mockMvc.perform(post("/api/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void cancelOrder_Success() throws Exception {
        // Given
        testOrder.setStatus("CANCELLED");
        when(orderService.cancelOrder(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shipOrder_Success() throws Exception {
        // Given
        testOrder.setStatus("SHIPPED");
        when(orderService.shipOrder(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void approveOrder_Success() throws Exception {
        // Given
        testOrder.setStatus("APPROVED");
        when(orderService.approveOrder("ORD-001")).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/by-order-no/ORD-001/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shipOrderWithDetails_Success() throws Exception {
        // Given
        testOrder.setStatus("SHIPPED");
        OrderController.ShippingRequest request = new OrderController.ShippingRequest();
        request.setTrackingNumber("TRACK-123");
        request.setCarrier("UPS");
        request.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
        request.setNotes("Handle with care");

        when(orderService.shipOrderWithDetails(
                eq("ORD-001"),
                eq("TRACK-123"),
                eq("UPS"),
                any(LocalDateTime.class),
                eq("Handle with care")
        )).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/by-order-no/ORD-001/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void deliverOrder_Success() throws Exception {
        // Given
        testOrder.setStatus("DELIVERED");
        OrderController.DeliveryRequest request = new OrderController.DeliveryRequest();
        request.setDeliveredDate(LocalDateTime.now());
        request.setNotes("Delivered successfully");

        when(orderService.deliverOrder(
                eq("ORD-001"),
                any(LocalDateTime.class),
                eq("Delivered successfully")
        )).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/by-order-no/ORD-001/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void getCart_Success() throws Exception {
        // Given
        testOrder.setStatus("CART");
        when(orderService.getOrCreateCart(any())).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/api/orders/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CART"));
    }

    @Test
    void addToCart_Success() throws Exception {
        // Given
        OrderController.AddToCartRequest request = new OrderController.AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(orderService.addToCart(any(), eq(1L), eq(2))).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("ORD-001"));
    }

    @Test
    void updateCartItem_Success() throws Exception {
        // Given
        OrderController.UpdateCartItemRequest request = new OrderController.UpdateCartItemRequest();
        request.setQuantity(5);

        when(orderService.updateCartItem(any(), eq(1L), eq(5))).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(put("/api/orders/cart/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("ORD-001"));
    }

    @Test
    void removeCartItem_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/orders/cart/items/1"))
                .andExpect(status().isNoContent());

        verify(orderService).removeCartItem(any(), eq(1L));
    }

    @Test
    void checkout_Success() throws Exception {
        // Given
        testOrder.setStatus("CREATED");
        when(orderService.checkoutCart(any())).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/api/orders/cart/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
}
