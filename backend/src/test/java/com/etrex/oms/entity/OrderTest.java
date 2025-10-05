/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;
    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("testuser");

        order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-001");
        order.setCustomer(customer);
        order.setTotalAmount(new BigDecimal("199.99"));
        order.setStatus(Order.Status.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, order.getId());
        assertEquals("ORD-001", order.getOrderNo());
        assertEquals(customer, order.getCustomer());
        assertEquals(new BigDecimal("199.99"), order.getTotalAmount());
        assertEquals(Order.Status.CREATED, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void testOrderItems() {
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setQuantity(2);
        items.add(item);

        order.setItems(items);
        assertEquals(1, order.getItems().size());
        assertEquals(2, order.getItems().get(0).getQuantity());
    }

    @Test
    void testPayments() {
        List<Payment> payments = new ArrayList<>();
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setAmount(new BigDecimal("199.99"));
        payments.add(payment);

        order.setPayments(payments);
        assertEquals(1, order.getPayments().size());
    }

    @Test
    void testOrderEvents() {
        List<OrderEvent> events = new ArrayList<>();
        OrderEvent event = new OrderEvent();
        event.setId(1L);
        event.setEventType("CREATED");
        events.add(event);

        order.setEvents(events);
        assertEquals(1, order.getEvents().size());
    }

    @Test
    void testStatusEnum() {
        assertEquals("CREATED", Order.Status.CREATED.name());
        assertEquals("PAID", Order.Status.PAID.name());
        assertEquals("APPROVED", Order.Status.APPROVED.name());
        assertEquals("SHIPPED", Order.Status.SHIPPED.name());
        assertEquals("CANCELLED", Order.Status.CANCELLED.name());

        assertEquals(Order.Status.CREATED, Order.Status.valueOf("CREATED"));
        assertEquals(Order.Status.PAID, Order.Status.valueOf("PAID"));
        assertEquals(Order.Status.APPROVED, Order.Status.valueOf("APPROVED"));
        assertEquals(Order.Status.SHIPPED, Order.Status.valueOf("SHIPPED"));
        assertEquals(Order.Status.CANCELLED, Order.Status.valueOf("CANCELLED"));
    }

    @Test
    void testNoArgsConstructor() {
        Order newOrder = new Order();
        assertNotNull(newOrder);
        assertNull(newOrder.getId());
    }
}
