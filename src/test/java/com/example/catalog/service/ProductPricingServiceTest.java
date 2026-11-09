package com.example.catalog.service;

import com.example.catalog.client.CurrencyClient;
import com.example.catalog.model.CurrencyConversion;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.ProductWithPrice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductPricingServiceTest {

    private static final BigDecimal USD_PRICE = new BigDecimal("99.99");

    @Mock
    private ProductService productService;

    @Mock
    private CurrencyClient currencyClient;

    @InjectMocks
    private ProductPricingService pricingService;

    private ProductResponse product() {
        return new ProductResponse(1L, "Test Product", "Electronics", USD_PRICE, 10);
    }

    @Test
    void usd_skipsCurrencyServiceAndIsNotConverted() {
        when(productService.findById(1L)).thenReturn(product());

        ProductWithPrice result = pricingService.getProductWithPrice(1L, "usd");

        assertThat(result.convertedPrice()).isEqualByComparingTo(USD_PRICE);
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.converted()).isFalse();
        verifyNoInteractions(currencyClient);
    }

    @Test
    void otherCurrency_usesCurrencyClientResult() {
        when(productService.findById(1L)).thenReturn(product());
        when(currencyClient.convert(USD_PRICE, "EUR"))
            .thenReturn(new CurrencyConversion(new BigDecimal("91.50"), "EUR", true));

        ProductWithPrice result = pricingService.getProductWithPrice(1L, "EUR");

        assertThat(result).isEqualTo(new ProductWithPrice(
            1L, "Test Product", "Electronics", USD_PRICE, new BigDecimal("91.50"), "EUR", 10, true));
    }

    @Test
    void currencyFallback_isLabeledUsdAndNotConverted() {
        when(productService.findById(1L)).thenReturn(product());
        when(currencyClient.convert(USD_PRICE, "EUR")).thenReturn(CurrencyConversion.unconverted(USD_PRICE));

        ProductWithPrice result = pricingService.getProductWithPrice(1L, "EUR");

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.convertedPrice()).isEqualByComparingTo(USD_PRICE);
        assertThat(result.converted()).isFalse();
    }

    @Test
    void unknownProduct_propagatesNotFoundWithoutCallingCurrencyService() {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        assertThatThrownBy(() -> pricingService.getProductWithPrice(99L, "EUR"))
            .isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(currencyClient);
    }
}
