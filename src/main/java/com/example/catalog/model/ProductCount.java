package com.example.catalog.model;

/** Response for GET /api/v1/products/count, e.g. {"count": 42}. MVC-only; REACTIVE has no such endpoint. */
public record ProductCount(long count) {
}
