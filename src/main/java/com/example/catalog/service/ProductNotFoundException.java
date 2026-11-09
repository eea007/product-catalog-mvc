package com.example.catalog.service;

/** Replaces REACTIVE's switchIfEmpty(notFound()); mapped to 404 by GlobalExceptionHandler. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}
