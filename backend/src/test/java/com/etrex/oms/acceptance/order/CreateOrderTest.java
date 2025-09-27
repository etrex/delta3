package com.etrex.oms.acceptance.order;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CreateOrderTest extends BaseAcceptanceTest {

    // 4. 測試建立訂單 API - POST /api/orders (庫存檢查)
    @Test
    void testCreateOrderWithStockValidation() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(2L); // customer1's ID

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        // 成功建立訂單
        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").exists());

        // 測試庫存不足
        CreateOrderRequest largeRequest = new CreateOrderRequest();
        largeRequest.setCustomerId(2L);

        CreateOrderRequest.OrderItemRequest largeItem = new CreateOrderRequest.OrderItemRequest();
        largeItem.setProductId(1L);
        largeItem.setQuantity(10000); // 超過庫存
        largeRequest.setItems(List.of(largeItem));

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isBadRequest());
    }
}