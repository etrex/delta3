/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.*;
import com.etrex.oms.entity.User;
import com.etrex.oms.exception.BusinessException;
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
import org.springframework.security.core.context.SecurityContextHolder;
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

    // ========== Cart APIs ==========

    @GetMapping("/cart")
    @Operation(summary = "Get cart", description = "Get current user's cart (order with CART status)")
    public ResponseEntity<OrderDTO> getCart() {
        User user = getCurrentUser();
        return ResponseEntity.ok(orderService.getOrCreateCart(user));
    }

    @PostMapping("/cart/items")
    @Operation(summary = "Add item to cart", description = "Add a product to cart")
    public ResponseEntity<OrderDTO> addToCart(@Valid @RequestBody AddToCartRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(orderService.addToCart(user, request.getProductId(), request.getQuantity()));
    }

    @PutMapping("/cart/items/{itemId}")
    @Operation(summary = "Update cart item", description = "Update quantity of cart item")
    public ResponseEntity<OrderDTO> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(orderService.updateCartItem(user, itemId, request.getQuantity()));
    }

    @DeleteMapping("/cart/items/{itemId}")
    @Operation(summary = "Remove cart item", description = "Remove item from cart")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long itemId) {
        User user = getCurrentUser();
        orderService.removeCartItem(user, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cart/checkout")
    @Operation(summary = "Checkout cart", description = "Convert cart to order (CART -> CREATED)")
    public ResponseEntity<OrderDTO> checkout() {
        User user = getCurrentUser();
        return ResponseEntity.ok(orderService.checkoutCart(user));
    }

    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No authenticated user found");
        }

        return (User) authentication.getPrincipal();
    }

    @Data
    static class AddToCartRequest {
        private Long productId;
        private Integer quantity;
    }

    @Data
    static class UpdateCartItemRequest {
        private Integer quantity;
    }
}