package com.etrex.oms.acceptance.product;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.ProductDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UpdateProductTest extends BaseAcceptanceTest {

    // 10. 測試更新商品 API - PUT /api/product (Admin 權限)
    @Test
    void testUpdateProductAdminOnly() throws Exception {
        ProductDTO updateProduct = new ProductDTO();
        updateProduct.setId(1L);
        updateProduct.setName("更新商品名稱");
        updateProduct.setDescription("更新描述");
        updateProduct.setPrice(BigDecimal.valueOf(399.99));
        updateProduct.setStock(30);
        updateProduct.setStatus("ACTIVE");

        // Customer 嘗試更新 (應該失敗)
        mockMvc.perform(put("/api/product/1")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProduct)))
                .andExpect(status().isForbidden());

        // Admin 更新 (應該成功)
        mockMvc.perform(put("/api/product/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新商品名稱"));
    }
}