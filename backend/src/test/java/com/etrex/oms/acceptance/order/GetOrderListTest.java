package com.etrex.oms.acceptance.order;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class GetOrderListTest extends BaseAcceptanceTest {

    // 3. 測試訂單列表 API - GET /api/orders (分頁、篩選、排序)
    @Test
    void testGetOrdersWithPaginationFilteringSorting() throws Exception {
        // 測試分頁
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));

        // 測試篩選 by status
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("CREATED"))));

        // 測試排序
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + adminToken)
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}