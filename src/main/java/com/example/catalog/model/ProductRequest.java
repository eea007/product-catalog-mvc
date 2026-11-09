package com.example.catalog.model;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Body for POST and PUT /api/v1/products. Deliberately has no id field, so a
 * caller can neither choose nor overwrite a product id (an "id" in the JSON is
 * ignored). The constraints only reject what REACTIVE would fail on anyway
 * (NOT NULL columns); REACTIVE accepts blank strings and negative numbers, so
 * those are not rejected here either.
 */
public record ProductRequest(
    @NotNull String name,
    @NotNull String category,
    @NotNull BigDecimal price,
    int stockQuantity
) {}
