package com.etrex.oms.acceptance.product;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.ProductDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CreateProductTest extends BaseAcceptanceTest {

    // 9. 測試新增商品 API - POST /api/product (Admin 權限)
    @Test
    void testCreateProductAdminOnly() throws Exception {
        ProductDTO newProduct = new ProductDTO();
        newProduct.setName("新商品");
        newProduct.setDescription("測試商品");
        newProduct.setPrice(BigDecimal.valueOf(299.99));
        newProduct.setStock(50);

        // Customer 嘗試新增 (應該失敗)
        mockMvc.perform(post("/api/product")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());

        // Admin 新增 (應該成功)
        mockMvc.perform(post("/api/product")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("新商品"));
    }
}