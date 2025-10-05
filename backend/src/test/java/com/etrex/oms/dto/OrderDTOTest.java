/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderDTOTest {

    @Test
    void testGettersAndSetters() {
        OrderDTO dto = new OrderDTO();

        dto.setId(1L);
        dto.setOrderNo("ORD-001");
        dto.setCustomerId(1L);
        dto.setCustomerName("Test Customer");
        dto.setTotalAmount(new BigDecimal("199.99"));
        dto.setStatus("CREATED");
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        List<PaymentDTO> payments = new ArrayList<>();
        dto.setPayments(payments);

        assertEquals(1L, dto.getId());
        assertEquals("ORD-001", dto.getOrderNo());
        assertEquals(1L, dto.getCustomerId());
        assertEquals("Test Customer", dto.getCustomerName());
        assertEquals(new BigDecimal("199.99"), dto.getTotalAmount());
        assertEquals("CREATED", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
        assertNotNull(dto.getPayments());
    }

    @Test
    void testNoArgsConstructor() {
        OrderDTO dto = new OrderDTO();
        assertNotNull(dto);
    }
}
