package com.example.catalog.service;

import com.example.catalog.domain.Product;
import com.example.catalog.model.ProductRequest;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.StockDepletedEvent;
import com.example.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Product CRUD and search. Makes no remote calls, so every method can be transactional. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ProductResponse> findAll() {
        List<ProductResponse> products = repository.findAll().stream()
            .map(ProductMapper::toResponse)
            .toList();
        log.info("Completed fetching all products: {}", products.size());
        return products;
    }

    public ProductResponse findById(Long id) {
        Product product = repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product not found: {}", id);
                return new ProductNotFoundException(id);
            });
        log.info("Found product: {}", product.getName());
        return ProductMapper.toResponse(product);
    }

    public long countProducts() {
        long count = repository.count();
        log.info("Counted {} products", count);
        return count;
    }

    public boolean exists(Long id) {
        return repository.existsById(id);
    }

    public List<ProductResponse> findByCategory(String category) {
        List<ProductResponse> products = repository.findByCategory(category).stream()
            .map(ProductMapper::toResponse)
            .toList();
        log.info("Found {} products in category {}", products.size(), category);
        return products;
    }

    public List<ProductResponse> findByName(String name) {
        List<ProductResponse> products = repository.findByNameContainingIgnoreCase(name).stream()
            .map(ProductMapper::toResponse)
            .toList();
        log.info("Found {} products matching name {}", products.size(), name);
        return products;
    }

    /**
     * REACTIVE rejected a caller-supplied id with IllegalArgumentException
     * ("ID must not be provided when creating a product"). ProductRequest has
     * no id field, so there is nothing to check here: the entity always starts
     * with a null id and the database assigns one.
     */
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product saved = repository.save(ProductMapper.toEntity(request));
        log.info("Created product: {}", saved.getName());
        return ProductMapper.toResponse(saved);
    }

    /**
     * Publishes a StockDepletedEvent whenever the saved stock is 0 (like
     * REACTIVE, even if it was already 0). The SNS call itself happens after
     * commit, in messaging.StockDepletedEventListener.
     */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product existing = repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Cannot update — product not found: {}", id);
                return new ProductNotFoundException(id);
            });
        ProductMapper.apply(request, existing);
        Product saved = repository.save(existing);

        if (saved.getStockQuantity() == 0) {
            eventPublisher.publishEvent(new StockDepletedEvent(saved.getId(), saved.getName()));
        }
        log.info("Updated product: {}", id);
        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Product product = repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Cannot delete — product not found: {}", id);
                return new ProductNotFoundException(id);
            });
        repository.delete(product);
        log.info("Deleted product: {}", id);
    }
}
