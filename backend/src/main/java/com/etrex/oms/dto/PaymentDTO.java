/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;

    private Long orderId;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "CREDIT_CARD|BANK_TRANSFER|PAYPAL|CASH")
    private String paymentMethod;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    private String status;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}