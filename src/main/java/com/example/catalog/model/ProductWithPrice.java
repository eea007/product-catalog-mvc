package com.example.catalog.model;

import java.math.BigDecimal;

/**
 * Response for GET /api/v1/products/{id}/price. Same fields as REACTIVE plus
 * {@code converted} (the one intended contract difference): true only when
 * currency-service actually converted the price. When it is false,
 * convertedPrice is the USD price and currency is "USD".
 */
public record ProductWithPrice(
    Long id,
    String name,
    String category,
    BigDecimal originalPriceUsd,
    BigDecimal convertedPrice,
    String currency,
    int stockQuantity,
    boolean converted
) {}
