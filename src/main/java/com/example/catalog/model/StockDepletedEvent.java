package com.example.catalog.model;

/**
 * Spring application event published by ProductService.update when a product's
 * stock is 0. StockDepletedEventListener forwards it to SNS after the
 * transaction commits.
 */
public record StockDepletedEvent(Long productId, String productName) {}
