/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.PaymentDTO;
import com.etrex.oms.dto.PaymentRequest;
import com.etrex.oms.service.PaymentService;
import com.etrex.oms.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
@Tag(name = "Payment", description = "Payment management APIs")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final ChatHistoryService chatHistoryService;

    @PostMapping
    @Operation(summary = "Create payment", description = "Process payment for an order")
    public ResponseEntity<PaymentDTO> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request) {

        // Ensure orderId in path matches request
        request.setOrderId(orderId);

        PaymentDTO result = paymentService.initiatePayment(orderId, request);

        // Track payment
        chatHistoryService.track(
            String.format("支付訂單 (訂單 ID: %d, 金額: %d, 支付方式: %s)",
                orderId, result.getAmount(), result.getPaymentMethod()));

        return ResponseEntity.ok(result);
    }
}
