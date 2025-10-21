package com.etrex.oms.acceptance.order;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.CreateOrderRequest;
import com.etrex.oms.dto.OrderDTO;
import com.etrex.oms.dto.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ShipOrderTest extends BaseAcceptanceTest {

    // 8. 測試出貨 API - POST /api/orders/{orderNo}/ship (Admin 權限)
    @Test
    void testShipOrderAdminOnly() throws Exception {
        // 先建立並支付訂單
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(101L);

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(List.of(item));

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderDTO order = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderDTO.class);

        // 付款
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(order.getId());
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(order.getTotalAmount());
        paymentRequest.setCardExpiry("12/25");
        paymentRequest.setCardCvv("123");
        paymentRequest.setCardName("Test User");

        mockMvc.perform(post("/api/orders/" + order.getId() + "/payments")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());

        // Customer 嘗試出貨 (應該失敗)
        mockMvc.perform(post("/api/orders/" + order.getId() + "/ship")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Admin 出貨 (應該成功)
        mockMvc.perform(post("/api/orders/" + order.getId() + "/ship")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}