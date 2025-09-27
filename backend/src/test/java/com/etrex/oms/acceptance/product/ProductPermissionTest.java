package com.etrex.oms.acceptance.product;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProductPermissionTest extends BaseAcceptanceTest {

    // 13. 測試權限控制 - Customer 不能看下架商品
    @Test
    void testCustomerCannotViewInactiveProducts() throws Exception {
        // 先下架一個商品
        mockMvc.perform(delete("/api/product/2")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Customer 查詢商品列表，不應看到下架商品
        MvcResult customerResult = mockMvc.perform(get("/api/product")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn();

        String customerResponse = customerResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> customerProducts = objectMapper.readValue(customerResponse, List.class);

        // 確認沒有 INACTIVE 商品
        for (Map<String, Object> product : customerProducts) {
            assert !product.get("status").equals("INACTIVE");
        }

        // Admin 可以看到所有商品（包含下架）
        mockMvc.perform(get("/api/product")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 2)].status").value("INACTIVE"));
    }
}