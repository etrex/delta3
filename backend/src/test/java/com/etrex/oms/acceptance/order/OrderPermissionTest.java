package com.etrex.oms.acceptance.order;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.CreateOrderRequest;
import com.etrex.oms.dto.OrderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderPermissionTest extends BaseAcceptanceTest {

    // 12. 測試權限控制 - Customer 只能查自己訂單
    @Test
    void testCustomerCanOnlyViewOwnOrders() throws Exception {
        // Customer 查詢自己的訂單列表
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].customerId", everyItem(is(2))));

        // 建立一個屬於 customer1 的訂單
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(2L);

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

        OrderDTO ownOrder = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderDTO.class);

        // Customer 可以查看自己的訂單
        mockMvc.perform(get("/api/orders/" + ownOrder.getId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Customer 不能查看其他人的訂單 (假設 ID 99999 不屬於該 customer)
        mockMvc.perform(get("/api/orders/99999")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}