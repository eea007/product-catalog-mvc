package com.example.catalog.service;

import com.example.catalog.client.CurrencyClient;
import com.example.catalog.model.CurrencyConversion;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.ProductWithPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Deliberately not @Transactional: the product lookup runs in ProductService's
 * own read-only transaction (a separate bean, so no self-invocation), and the
 * currency-service call happens after that transaction has closed.
 */
@Service
@RequiredArgsConstructor
public class ProductPricingService {

    private final ProductService productService;
    private final CurrencyClient currencyClient;

    public ProductWithPrice getProductWithPrice(Long id, String currency) {
        ProductResponse product = productService.findById(id);

        // As in REACTIVE, USD skips the remote call. converted=false because no conversion happened.
        CurrencyConversion conversion = "USD".equalsIgnoreCase(currency)
            ? CurrencyConversion.unconverted(product.price())
            : currencyClient.convert(product.price(), currency);

        return ProductMapper.toProductWithPrice(product, conversion);
    }
}
