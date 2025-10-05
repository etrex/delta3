/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.entity.Product;
import com.etrex.oms.entity.User;
import com.etrex.oms.repository.OrderRepository;
import com.etrex.oms.repository.ProductRepository;
import com.etrex.oms.repository.UserRepository;
import com.etrex.oms.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
// 注意：生產環境應該透過 Spring Security 或移除此 Controller 來保護
public class TestDataController {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/reset")
    public ResponseEntity<String> resetDatabase() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        return ResponseEntity.ok("Database reset successfully");
    }

    @PostMapping("/products/seed")
    public ResponseEntity<String> seedProducts(@RequestBody List<ProductSeedRequest> products) {
        for (ProductSeedRequest req : products) {
            Product product = new Product();
            product.setName(req.getName());
            product.setDescription(req.getDescription() != null ? req.getDescription() : "測試商品描述");
            product.setPrice(req.getPrice());
            product.setStock(req.getStock());
            product.setStockThreshold(req.getStockThreshold() != null ? req.getStockThreshold() : 10);
            product.setStatus(Product.Status.valueOf(req.getStatus() != null ? req.getStatus() : "ACTIVE"));
            productRepository.save(product);
        }
        return ResponseEntity.ok("Products seeded: " + products.size());
    }

    @PostMapping("/users/init")
    public ResponseEntity<String> initDefaultUsers() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setEmail("admin@example.com");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);

            User customer1 = new User();
            customer1.setUsername("customer1");
            customer1.setPassword(passwordEncoder.encode("password123"));
            customer1.setEmail("customer1@example.com");
            customer1.setRole(User.Role.CUSTOMER);
            userRepository.save(customer1);

            return ResponseEntity.ok("Default users initialized");
        }
        return ResponseEntity.ok("Users already exist");
    }

    @Data
    public static class ProductSeedRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private Integer stockThreshold;
        private String status;
    }
}
