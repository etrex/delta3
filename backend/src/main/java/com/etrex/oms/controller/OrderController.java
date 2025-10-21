/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.*;
import com.etrex.oms.entity.User;
import com.etrex.oms.exception.BusinessException;
import com.etrex.oms.service.ChatHistoryService;
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
    private final ChatHistoryService chatHistoryService;

    @GetMapping
    @Operation(summary = "Get orders", description = "Get paginated list of orders")
    public ResponseEntity<Page<OrderDTO>> getOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "true") boolean tracking,
            @RequestParam(required = false) String context,
            Pageable pageable) {
        Page<OrderDTO> result = orderService.getOrders(customerId, status, keyword, startDate, endDate, pageable);

        // Track operation only if tracking=true
        if (tracking && context != null) {
            String message = switch (context.toLowerCase()) {
                case "dashboard" -> "查看儀表板";
                case "orders" -> "查看訂單管理頁面";
                case "shipping" -> "查看出貨管理頁面";
                default -> "查看訂單列表";
            };
            chatHistoryService.track(message);
        } else if (tracking) {
            chatHistoryService.track("查看訂單列表");
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Get single order details")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        OrderDTO result = orderService.getOrderById(id);

        // Track operation
        chatHistoryService.track(
            String.format("查看訂單詳情 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-order-no/{orderNo}")
    @Operation(summary = "Get order by order number", description = "Get single order details by order number")
    public ResponseEntity<OrderDTO> getOrderByOrderNo(@PathVariable String orderNo) {
        OrderDTO result = orderService.getOrderByOrderNo(orderNo);

        // Track operation
        chatHistoryService.track(
            String.format("查看訂單詳情 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get order events", description = "Get event history for an order")
    public ResponseEntity<java.util.List<OrderEventDTO>> getOrderEvents(@PathVariable Long id) {
        java.util.List<OrderEventDTO> result = orderService.getOrderEvents(id);

        // Note: Events 只是訂單詳情的一部分，不需要單獨追蹤

        return ResponseEntity.ok(result);
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
        PaymentDTO result = orderService.initiatePayment(id, paymentDTO);

        // Track payment with actual amount from result
        chatHistoryService.track(
            String.format("支付訂單 (訂單 ID: %d, 金額: %d, 支付方式: %s)",
                id, result.getAmount(), result.getPaymentMethod()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        OrderDTO result = orderService.cancelOrder(id);

        // Track operation
        chatHistoryService.track(
            String.format("取消訂單 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ship order", description = "Mark order as shipped (Admin only)")
    public ResponseEntity<OrderDTO> shipOrder(@PathVariable Long id) {
        OrderDTO result = orderService.shipOrder(id);

        // Track operation
        chatHistoryService.track(
            String.format("出貨訂單 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/by-order-no/{orderNo}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve order", description = "Approve order for shipping (Admin only)")
    public ResponseEntity<OrderDTO> approveOrder(@PathVariable String orderNo) {
        OrderDTO result = orderService.approveOrder(orderNo);

        // Track operation
        chatHistoryService.track(
            String.format("批准訂單 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/by-order-no/{orderNo}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ship order with details", description = "Ship order with tracking info (Admin only)")
    public ResponseEntity<OrderDTO> shipOrderWithDetails(
            @PathVariable String orderNo,
            @RequestBody ShippingRequest request) {
        OrderDTO result = orderService.shipOrderWithDetails(
                orderNo,
                request.getTrackingNumber(),
                request.getCarrier(),
                request.getEstimatedDelivery(),
                request.getNotes()
        );

        // Track operation with shipping details
        String trackingInfo = request.getTrackingNumber() != null
            ? String.format(", 追蹤號碼: %s", request.getTrackingNumber())
            : "";
        String carrierInfo = request.getCarrier() != null
            ? String.format(", 物流商: %s", request.getCarrier())
            : "";
        chatHistoryService.track(
            String.format("出貨訂單 (訂單 ID: %d%s%s)",
                result.getId(), trackingInfo, carrierInfo));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/by-order-no/{orderNo}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deliver order", description = "Mark order as delivered (Admin only)")
    public ResponseEntity<OrderDTO> deliverOrder(
            @PathVariable String orderNo,
            @RequestBody DeliveryRequest request) {
        OrderDTO result = orderService.deliverOrder(
                orderNo,
                request.getDeliveredDate(),
                request.getNotes()
        );

        // Track operation
        chatHistoryService.track(
            String.format("完成配送 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
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
    public ResponseEntity<OrderDTO> getCart(
            @RequestParam(defaultValue = "true") boolean tracking,
            @RequestParam(required = false) String context) {
        User user = getCurrentUser();
        OrderDTO result = orderService.getOrCreateCart(user);

        // Track operation with context
        if (tracking && context != null) {
            String message = switch (context.toLowerCase()) {
                case "sidebar" -> "查看購物車";
                case "checkout" -> "進入結帳頁面";
                default -> "查看購物車";
            };
            chatHistoryService.track(user, message);
        } else if (tracking) {
            // Default tracking message if no context provided
            chatHistoryService.track(user, "查看購物車");
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/cart/items")
    @Operation(summary = "Add item to cart", description = "Add a product to cart")
    public ResponseEntity<OrderDTO> addToCart(@Valid @RequestBody AddToCartRequest request) {
        User user = getCurrentUser();
        OrderDTO result = orderService.addToCart(user, request.getProductId(), request.getQuantity());

        // Track operation with actual values
        chatHistoryService.track(user,
            String.format("加入商品到購物車 (商品 ID: %d, 數量: %d)",
                request.getProductId(), request.getQuantity()));

        return ResponseEntity.ok(result);
    }

    @PutMapping("/cart/items/{itemId}")
    @Operation(summary = "Update cart item", description = "Update quantity of cart item")
    public ResponseEntity<OrderDTO> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        User user = getCurrentUser();
        OrderDTO result = orderService.updateCartItem(user, itemId, request.getQuantity());

        // Track operation with actual values
        chatHistoryService.track(user,
            String.format("更新購物車數量 (購物車項目 ID: %d, 數量: %d)",
                itemId, request.getQuantity()));

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/cart/items/{itemId}")
    @Operation(summary = "Remove cart item", description = "Remove item from cart")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long itemId) {
        User user = getCurrentUser();
        orderService.removeCartItem(user, itemId);

        // Track operation
        chatHistoryService.track(user,
            String.format("從購物車移除商品 (購物車項目 ID: %d)", itemId));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cart/checkout")
    @Operation(summary = "Checkout cart", description = "Convert cart to order (CART -> CREATED)")
    public ResponseEntity<OrderDTO> checkout() {
        User user = getCurrentUser();
        OrderDTO result = orderService.checkoutCart(user);

        // Track operation with order ID
        chatHistoryService.track(user,
            String.format("結帳購物車 (訂單 ID: %d)", result.getId()));

        return ResponseEntity.ok(result);
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