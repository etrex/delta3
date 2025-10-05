/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(0)
    private Integer stock;

    @Min(0)
    private Integer stockThreshold = 5;

    @Pattern(regexp = "ACTIVE|INACTIVE")
    private String status = "ACTIVE";

    private LocalDateTime createdAt;
}