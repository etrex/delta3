/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@OpenAPIDefinition(
        info = @Info(
                title = "Order Management System API",
                version = "1.0",
                description = "Intelligent Order Management System Backend APIs"
        )
)
public class OmsApplication {
    public static void main(String[] args) {
        // Load .env file (if exists) into system properties
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")  // Look for .env in project root
                    .ignoreIfMissing()  // Don't fail if .env doesn't exist (production)
                    .load();

            // Set all .env variables as system properties so Spring can read them
            dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );

            System.out.println("✅ Loaded .env file successfully");
        } catch (Exception e) {
            System.out.println("⚠️  No .env file found (will use environment variables or defaults)");
        }

        SpringApplication.run(OmsApplication.class, args);
    }
}