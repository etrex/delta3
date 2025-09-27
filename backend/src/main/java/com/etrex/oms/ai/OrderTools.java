/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import com.etrex.oms.dto.ProductDTO;
import com.etrex.oms.dto.OrderDTO;
import com.etrex.oms.entity.Product;
import com.etrex.oms.service.ProductService;
import com.etrex.oms.service.OrderService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderTools {
    private final ProductService productService;
    private final OrderService orderService;

    @Tool("Search products by keyword")
    public String searchProducts(String keyword) {
        try {
            Page<ProductDTO> products = productService.getProducts(keyword, Product.Status.ACTIVE, PageRequest.of(0, 10));
            StringBuilder result = new StringBuilder("找到以下商品：\n");

            for (ProductDTO product : products.getContent()) {
                result.append(String.format("- %s：NT$%.2f，庫存：%d\n",
                    product.getName(), product.getPrice(), product.getStock()));
            }

            return result.toString();
        } catch (Exception e) {
            return "抱歉，查詢商品時發生錯誤。";
        }
    }

    @Tool("Get product details by ID")
    public String getProductDetails(Long productId) {
        try {
            ProductDTO product = productService.getProductById(productId);
            return String.format("商品詳情：\n名稱：%s\n描述：%s\n價格：NT$%.2f\n庫存：%d\n狀態：%s",
                product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getStatus());
        } catch (Exception e) {
            return "抱歉，無法找到該商品。";
        }
    }

    @Tool("Get order details by ID")
    public String getOrderDetails(Long orderId) {
        try {
            OrderDTO order = orderService.getOrderById(orderId);
            StringBuilder result = new StringBuilder();
            result.append(String.format("訂單詳情：\n訂單編號：%d\n顧客：%s\n總金額：NT$%.2f\n狀態：%s\n建立時間：%s\n",
                order.getId(), order.getCustomerName(), order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt()));

            if (order.getItems() != null && !order.getItems().isEmpty()) {
                result.append("\n訂單商品：\n");
                order.getItems().forEach(item ->
                    result.append(String.format("- %s x%d，單價：NT$%.2f\n",
                        item.getProductName(), item.getQuantity(), item.getPrice())));
            }

            return result.toString();
        } catch (Exception e) {
            return "抱歉，無法找到該訂單。";
        }
    }

    @Tool("Check stock availability for a product")
    public String checkStock(Long productId, Integer quantity) {
        try {
            boolean available = productService.checkStock(productId, quantity);
            ProductDTO product = productService.getProductById(productId);

            if (available) {
                return String.format("商品 %s 庫存充足，可購買 %d 件。目前庫存：%d",
                    product.getName(), quantity, product.getStock());
            } else {
                return String.format("商品 %s 庫存不足，無法購買 %d 件。目前庫存：%d",
                    product.getName(), quantity, product.getStock());
            }
        } catch (Exception e) {
            return "抱歉，無法查詢庫存資訊。";
        }
    }
}