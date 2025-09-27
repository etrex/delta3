package com.etrex.oms.acceptance;

import com.etrex.oms.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public abstract class BaseAcceptanceTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminToken;
    protected String customerToken;

    @BeforeEach
    void setUpBaseTest() throws Exception {
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
}