package com.example.catalog.service;

import com.example.catalog.domain.Product;
import com.example.catalog.model.CurrencyConversion;
import com.example.catalog.model.ProductRequest;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.ProductWithPrice;

/** Entity ↔ record mapping, kept out of controllers. */
final class ProductMapper {

    private ProductMapper() {}

    static Product toEntity(ProductRequest request) {
        return Product.builder()
            .name(request.name())
            .category(request.category())
            .price(request.price())
            .stockQuantity(request.stockQuantity())
            .build();
    }

    static void apply(ProductRequest request, Product existing) {
        existing.setName(request.name());
        existing.setCategory(request.category());
        existing.setPrice(request.price());
        existing.setStockQuantity(request.stockQuantity());
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getCategory(),
            product.getPrice(),
            product.getStockQuantity());
    }

    static ProductWithPrice toProductWithPrice(ProductResponse product, CurrencyConversion conversion) {
        return new ProductWithPrice(
            product.id(),
            product.name(),
            product.category(),
            product.price(),
            conversion.amount(),
            conversion.currency(),
            product.stockQuantity(),
            conversion.converted());
    }
}
