package com.etrex.oms;

import com.etrex.oms.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // 取得 Admin Token
        AuthRequest adminAuth = new AuthRequest();
        adminAuth.setUsername("admin");
        adminAuth.setPassword("password123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminAuth)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse adminResponse = objectMapper.readValue(
                adminResult.getResponse().getContentAsString(),
                AuthResponse.class);
        adminToken = adminResponse.getToken();

        // 取得 Customer Token
        AuthRequest customerAuth = new AuthRequest();
        customerAuth.setUsername("customer1");
        customerAuth.setPassword("password123");

        MvcResult customerResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerAuth)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse customerResponse = objectMapper.readValue(
                customerResult.getResponse().getContentAsString(),
                AuthResponse.class);
        customerToken = customerResponse.getToken();
    }

    // 1. 測試身分認證 - Customer/Admin 登入
    @Test
    void testAuthentication() throws Exception {
        // Admin 登入
        AuthRequest adminRequest = new AuthRequest();
        adminRequest.setUsername("admin");
        adminRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Customer 登入
        AuthRequest customerRequest = new AuthRequest();
        customerRequest.setUsername("customer1");
        customerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

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

    // 5. 測試訂單詳情 API - GET /api/orders/{orderNo}
    @Test
    void testGetOrderDetails() throws Exception {
        // 先建立訂單
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

        OrderDTO order = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderDTO.class);

        // 查詢訂單詳情
        mockMvc.perform(get("/api/orders/" + order.getId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").exists())
                .andExpect(jsonPath("$.items[0].quantity").exists())
                .andExpect(jsonPath("$.payment").exists());
    }

    // 6. 測試付款 API - POST /api/orders/{orderNo}/pay
    @Test
    void testPayOrder() throws Exception {
        // 先建立訂單
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

        OrderDTO order = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderDTO.class);

        // 付款
        PaymentDTO paymentRequest = new PaymentDTO();
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(order.getTotalAmount());

        mockMvc.perform(post("/api/orders/" + order.getId() + "/pay")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // 7. 測試取消訂單 API - POST /api/orders/{orderNo}/cancel
    @Test
    void testCancelOrder() throws Exception {
        // 先建立訂單
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

        OrderDTO order = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderDTO.class);

        // 取消訂單
        mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // 8. 測試出貨 API - POST /api/orders/{orderNo}/ship (Admin 權限)
    @Test
    void testShipOrderAdminOnly() throws Exception {
        // 先建立並支付訂單
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

        OrderDTO order = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderDTO.class);

        // 付款
        PaymentDTO paymentRequest = new PaymentDTO();
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(order.getTotalAmount());

        mockMvc.perform(post("/api/orders/" + order.getId() + "/pay")
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
        mockMvc.perform(put("/api/product")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProduct)))
                .andExpect(status().isForbidden());

        // Admin 更新 (應該成功)
        mockMvc.perform(put("/api/product")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新商品名稱"));
    }

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
        List<Map> customerProducts = objectMapper.readValue(customerResponse, List.class);

        // 確認沒有 INACTIVE 商品
        for (Map product : customerProducts) {
            assert !product.get("status").equals("INACTIVE");
        }

        // Admin 可以看到所有商品（包含下架）
        mockMvc.perform(get("/api/product")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 2)].status").value("INACTIVE"));
    }

    // 14. 測試聊天 API - POST /api/chat (AI 整合)
    @Test
    void testChatAPI() throws Exception {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage("我想查詢訂單狀態");
        chatRequest.setSessionId("test-session-123");

        mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.sessionId").value("test-session-123"));
    }
}