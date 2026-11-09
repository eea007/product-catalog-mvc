package com.example.catalog.client;

import com.example.catalog.model.CurrencyConversion;
import com.example.catalog.model.CurrencyConversionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrencyClient {

    @Qualifier("currencyRestClient")
    private final RestClient currencyRestClient;

    /**
     * Converts a USD amount via currency-service. Never throws for remote
     * failures.
     *
     * Fix vs REACTIVE: on failure or timeout REACTIVE returned the USD amount
     * labeled with the requested currency. Here the fallback is labeled "USD"
     * with converted=false.
     */
    public CurrencyConversion convert(BigDecimal amount, String toCurrency) {
        try {
            CurrencyConversionResponse response = currencyRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/currencies/convert")
                    .queryParam("from", "USD")
                    .queryParam("to", toCurrency)
                    .queryParam("amount", amount)
                    .build())
                .retrieve()
                .body(CurrencyConversionResponse.class);

            if (response == null || response.convertedAmount() == null) {
                log.warn("Currency conversion failed for {} {}: empty response — returning original amount",
                    amount, toCurrency);
                return CurrencyConversion.unconverted(amount);
            }
            return new CurrencyConversion(response.convertedAmount(), toCurrency.toUpperCase(), true);
        } catch (RestClientException ex) {
            log.warn("Currency conversion failed for {} {}: {} — returning original amount",
                amount, toCurrency, ex.getMessage());
            return CurrencyConversion.unconverted(amount);
        }
    }
}
