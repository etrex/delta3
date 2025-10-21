/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import com.etrex.oms.entity.User;
import com.etrex.oms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 只初始化基本用戶（測試資料由 TestDataController 管理）
            if (userRepository.count() == 0) {
                log.info("Initializing default users...");

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

                User customer2 = new User();
                customer2.setUsername("customer2");
                customer2.setPassword(passwordEncoder.encode("password123"));
                customer2.setEmail("customer2@example.com");
                customer2.setRole(User.Role.CUSTOMER);
                userRepository.save(customer2);

                log.info("Default users initialized successfully");
            }

            log.info("Application started. Use /api/test endpoints to seed test data");
        };
    }
}
