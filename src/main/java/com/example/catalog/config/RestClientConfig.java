package com.example.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient beans for currency-service and inventory-service.
 *
 * Fix vs REACTIVE: REACTIVE's 8 s "response timeout" filter wrapped an
 * already-received response, so it could never fire. Here the timeouts live in
 * the request factory (connect 5 s / read 3 s) and are the only ones.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient currencyRestClient(
            RestClient.Builder builder,
            @Value("${currency.service.url:http://localhost:8083}") String currencyServiceUrl,
            @Value("${api-key}") String apiKey,
            @Value("${currency.service.connect-timeout:5s}") Duration connectTimeout,
            @Value("${currency.service.read-timeout:3s}") Duration readTimeout) {
        return builder
            .baseUrl(currencyServiceUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-API-Key", apiKey)
            .requestFactory(requestFactory(connectTimeout, readTimeout))
            .build();
    }

    /**
     * Fix vs REACTIVE: the assistant's inventory calls had no timeout at all.
     */
    @Bean
    public RestClient inventoryRestClient(
            RestClient.Builder builder,
            @Value("${inventory.service.url:http://localhost:8082}") String inventoryServiceUrl,
            @Value("${inventory.service.connect-timeout:5s}") Duration connectTimeout,
            @Value("${inventory.service.read-timeout:3s}") Duration readTimeout) {
        return builder
            .baseUrl(inventoryServiceUrl)
            .requestFactory(requestFactory(connectTimeout, readTimeout))
            .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
