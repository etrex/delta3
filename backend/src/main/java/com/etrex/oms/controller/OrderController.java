/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.*;
import com.etrex.oms.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get orders", description = "Get paginated list of orders")
    public ResponseEntity<Page<OrderDTO>> getOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrders(customerId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Get single order details")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/by-order-no/{orderNo}")
    @Operation(summary = "Get order by order number", description = "Get single order details by order number")
    public ResponseEntity<OrderDTO> getOrderByOrderNo(@PathVariable String orderNo) {
        return ResponseEntity.ok(orderService.getOrderByOrderNo(orderNo));
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Create new order")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay for order", description = "Initiate payment for an order")
    public ResponseEntity<PaymentDTO> payOrder(
            @PathVariable Long id,
            @Valid @RequestBody PaymentDTO paymentDTO) {
        return ResponseEntity.ok(orderService.initiatePayment(id, paymentDTO));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ship order", description = "Mark order as shipped (Admin only)")
    public ResponseEntity<OrderDTO> shipOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.shipOrder(id));
    }

    @PostMapping("/by-order-no/{orderNo}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve order", description = "Approve order for shipping (Admin only)")
    public ResponseEntity<OrderDTO> approveOrder(@PathVariable String orderNo) {
        return ResponseEntity.ok(orderService.approveOrder(orderNo));
    }

    @PostMapping("/by-order-no/{orderNo}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ship order with details", description = "Ship order with tracking info (Admin only)")
    public ResponseEntity<OrderDTO> shipOrderWithDetails(
            @PathVariable String orderNo,
            @RequestBody ShippingRequest request) {
        return ResponseEntity.ok(orderService.shipOrderWithDetails(
                orderNo,
                request.getTrackingNumber(),
                request.getCarrier(),
                request.getEstimatedDelivery(),
                request.getNotes()
        ));
    }

    @PostMapping("/by-order-no/{orderNo}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deliver order", description = "Mark order as delivered (Admin only)")
    public ResponseEntity<OrderDTO> deliverOrder(
            @PathVariable String orderNo,
            @RequestBody DeliveryRequest request) {
        return ResponseEntity.ok(orderService.deliverOrder(
                orderNo,
                request.getDeliveredDate(),
                request.getNotes()
        ));
    }

    // Request DTOs
    @Data
    static class ShippingRequest {
        private String trackingNumber;
        private String carrier;
        private LocalDateTime estimatedDelivery;
        private String notes;
    }

    @Data
    static class DeliveryRequest {
        private LocalDateTime deliveredDate;
        private String notes;
    }
}