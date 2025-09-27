package com.etrex.oms.acceptance.order;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.CreateOrderRequest;
import com.etrex.oms.dto.OrderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CancelOrderTest extends BaseAcceptanceTest {

    // 7. 測試取消訂單 API - POST /api/orders/{orderNo}/cancel
    @Test
    void testCancelOrder() throws Exception {
        // 先建立訂單
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

        // 取消訂單
        mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}