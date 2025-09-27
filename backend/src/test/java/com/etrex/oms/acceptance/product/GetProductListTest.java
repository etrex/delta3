package com.etrex.oms.acceptance.product;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class GetProductListTest extends BaseAcceptanceTest {

    // 2. 測試商品列表 API - GET /api/product
    @Test
    void testGetProducts() throws Exception {
        mockMvc.perform(get("/api/product")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].stock").exists());
    }
}