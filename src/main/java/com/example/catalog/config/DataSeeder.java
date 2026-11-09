package com.example.catalog.config;

import com.example.catalog.domain.Product;
import com.example.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "data.seed.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner seedData(ProductRepository repository) {
        return args -> {
            long count = repository.count();

            if (count == 0) {
                repository.saveAll(List.of(
                    Product.builder()
                        .name("Laptop Pro 15")
                        .category("Electronics")
                        .price(new BigDecimal("1299.99"))
                        .stockQuantity(10)
                        .build(),
                    Product.builder()
                        .name("Wireless Mouse")
                        .category("Electronics")
                        .price(new BigDecimal("29.99"))
                        .stockQuantity(50)
                        .build(),
                    Product.builder()
                        .name("Standing Desk")
                        .category("Furniture")
                        .price(new BigDecimal("549.00"))
                        .stockQuantity(5)
                        .build()
                ));
                log.info("Database empty — seeded 3 products");
            } else {
                log.info("Database already has {} products — skipping seed", count);
            }
        };
    }
}
