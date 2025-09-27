package com.etrex.oms;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    @Test
    void generatePasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Matches: " + encoder.matches(password, hash));

        // 測試現有的雜湊
        String existingHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFDYj.1sVjtmGpRB4UG3hM6";
        System.out.println("Existing hash matches: " + encoder.matches(password, existingHash));
    }
}