/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShippingDTO {
    private Long id;
    private Long orderId;
    private String status;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String notes;
}
