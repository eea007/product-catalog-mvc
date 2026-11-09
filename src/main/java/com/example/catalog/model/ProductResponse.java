package com.example.catalog.model;

import java.math.BigDecimal;

/** Same JSON shape REACTIVE returns for its Product entity. */
public record ProductResponse(
    Long id,
    String name,
    String category,
    BigDecimal price,
    int stockQuantity
) {}
