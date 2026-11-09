package com.example.catalog.service;

import com.example.catalog.domain.Product;
import com.example.catalog.model.ProductRequest;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.StockDepletedEvent;
import com.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Mirrors REACTIVE's ProductServiceTest; the SNS publisher is replaced by the application event. */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService service;

    private Product product(long id, int stock) {
        return Product.builder()
            .id(id)
            .name("Test Product")
            .category("Electronics")
            .price(new BigDecimal("99.99"))
            .stockQuantity(stock)
            .build();
    }

    private ProductRequest request(int stock) {
        return new ProductRequest("Test Product", "Electronics", new BigDecimal("99.99"), stock);
    }

    private ProductResponse response(long id, int stock) {
        return new ProductResponse(id, "Test Product", "Electronics", new BigDecimal("99.99"), stock);
    }

    @Test
    void findAll_returnsListOfProducts() {
        when(repository.findAll()).thenReturn(List.of(product(1L, 10), product(2L, 5)));

        assertThat(service.findAll()).containsExactly(response(1L, 10), response(2L, 5));
    }

    @Test
    void countProducts_returnsRepositoryCount() {
        when(repository.count()).thenReturn(42L);

        assertThat(service.countProducts()).isEqualTo(42L);
    }

    @Test
    void findById_returnsProductWhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1L, 10)));

        assertThat(service.findById(1L)).isEqualTo(response(1L, 10));
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("Product not found: 99");
    }

    @Test
    void create_savesAndReturnsProduct() {
        when(repository.save(any())).thenReturn(product(1L, 10));

        assertThat(service.create(request(10))).isEqualTo(response(1L, 10));

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getName()).isEqualTo("Test Product");
    }

    @Test
    void update_updatesExistingProductFieldsAndSaves() {
        Product existing = product(1L, 10);
        ProductRequest updated = new ProductRequest("Updated Name", "Furniture", new BigDecimal("199.99"), 5);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = service.update(1L, updated);

        assertThat(result).isEqualTo(
            new ProductResponse(1L, "Updated Name", "Furniture", new BigDecimal("199.99"), 5));
        verify(repository).save(existing);
    }

    @Test
    void update_throwsWhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request(5)))
            .isInstanceOf(ProductNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_publishesStockDepletedEventWhenStockIsZero() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1L, 10)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = service.update(1L, request(0));

        assertThat(result.stockQuantity()).isZero();
        verify(eventPublisher).publishEvent(new StockDepletedEvent(1L, "Test Product"));
    }

    @Test
    void update_doesNotPublishEventWhenStockIsAboveZero() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1L, 10)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(1L, request(5));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void delete_deletesWhenProductExists() {
        Product p = product(1L, 10);
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        service.delete(1L);

        verify(repository).delete(p);
    }

    @Test
    void delete_throwsWhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(ProductNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    // ── Beyond REACTIVE's tests ───────────────────────────────────────────

    @Test
    void create_neverPublishesStockEventEvenWithZeroStock() {
        when(repository.save(any())).thenReturn(product(1L, 0));

        service.create(request(0));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void findByCategory_mapsRepositoryResults() {
        when(repository.findByCategory("Electronics")).thenReturn(List.of(product(1L, 10)));

        assertThat(service.findByCategory("Electronics")).containsExactly(response(1L, 10));
    }

    @Test
    void findByName_usesCaseInsensitiveContainsQuery() {
        when(repository.findByNameContainingIgnoreCase("test")).thenReturn(List.of(product(1L, 10)));

        assertThat(service.findByName("test")).containsExactly(response(1L, 10));
    }
}
