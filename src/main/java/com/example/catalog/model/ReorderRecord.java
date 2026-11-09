package com.example.catalog.model;

import java.math.BigDecimal;

/**
 * Mirrors the JSON shape returned by inventory-service's
 * GET /api/v1/inventory/{productId}/reorder-estimate (ReorderEstimate in
 * inventory-service) — there is no /reorders history endpoint there.
 */
public record ReorderRecord(
    Long productId,
    String productName,
    int reorderQuantity,
    BigDecimal unitPriceUsd,
    BigDecimal totalCostUsd,
    BigDecimal totalCostConverted,
    String currency
) {}
