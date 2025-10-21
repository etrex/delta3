/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "CREDIT_CARD|BANK_TRANSFER|PAYPAL|CASH", message = "Invalid payment method")
    private String paymentMethod;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Integer amount;

    // Credit card information (for payment processing only, not stored)
    private String cardExpiry;  // Format: MM/YY
    private String cardCvv;
    private String cardName;
}
