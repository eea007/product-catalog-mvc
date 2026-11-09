package com.example.catalog.repository;

import com.example.catalog.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
            product("Laptop Pro 15", "Electronics", "1299.99", 10),
            product("Wireless Mouse", "Electronics", "29.99", 0),
            product("Standing Desk", "Furniture", "549.00", 5)
        ));
    }

    private Product product(String name, String category, String price, int stock) {
        return Product.builder()
            .name(name)
            .category(category)
            .price(new BigDecimal(price))
            .stockQuantity(stock)
            .build();
    }

    private List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    @Test
    void save_assignsGeneratedId() {
        Product saved = repository.save(product("Monitor", "Electronics", "199.00", 3));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByCategory_returnsExactCategoryMatches() {
        assertThat(names(repository.findByCategory("Electronics")))
            .containsExactlyInAnyOrder("Laptop Pro 15", "Wireless Mouse");
        assertThat(repository.findByCategory("electronics")).isEmpty();
    }

    @Test
    void findByStockQuantityGreaterThan_excludesOutOfStock() {
        assertThat(names(repository.findByStockQuantityGreaterThan(0)))
            .containsExactlyInAnyOrder("Laptop Pro 15", "Standing Desk");
    }

    @Test
    void findByNameContainingIgnoreCase_matchesSubstringRegardlessOfCase() {
        assertThat(names(repository.findByNameContainingIgnoreCase("MOUSE")))
            .containsExactly("Wireless Mouse");
    }

    @Test
    void findByNameContainingIgnoreCase_emptyStringMatchesEverything() {
        // REACTIVE's /search defaults a missing name to "", which returns every product
        assertThat(repository.findByNameContainingIgnoreCase("")).hasSize(3);
    }
}
