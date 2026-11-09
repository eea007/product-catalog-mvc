package com.example.catalog.model;

import java.time.LocalDateTime;

/**
 * Mirrors the JSON shape returned by inventory-service's
 * GET /api/v1/inventory/{productId} (see InventoryItem in inventory-service).
 */
public record StockLevel(
    Long id,
    Long productId,
    String productName,
    int reorderThreshold,
    int reorderQuantity,
    String status,
    LocalDateTime lastUpdated
) {}
