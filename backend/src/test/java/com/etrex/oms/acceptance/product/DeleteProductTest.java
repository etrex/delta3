package com.etrex.oms.acceptance.product;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class DeleteProductTest extends BaseAcceptanceTest {

    // 11. 測試下架商品 API - DELETE /api/product (Admin 權限)
    @Test
    void testDeleteProductAdminOnly() throws Exception {
        // Customer 嘗試下架 (應該失敗)
        mockMvc.perform(delete("/api/product/1")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Admin 下架 (應該成功)
        mockMvc.perform(delete("/api/product/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 確認商品已下架
        mockMvc.perform(get("/api/product")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 1)].status").value("INACTIVE"));
    }
}