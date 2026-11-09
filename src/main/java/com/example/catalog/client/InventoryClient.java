package com.example.catalog.client;

import com.example.catalog.model.ReorderRecord;
import com.example.catalog.model.StockLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * inventory-service calls used by the assistant tools. The caller's
 * Authorization header is forwarded so inventory-service sees the end user's JWT.
 *
 * Fix vs REACTIVE: calls are timed (see RestClientConfig) and failures return
 * Optional.empty() instead of throwing, so the tool can tell the model the data
 * is unavailable rather than failing the whole chat request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    @Qualifier("inventoryRestClient")
    private final RestClient inventoryRestClient;

    public Optional<StockLevel> getStock(Long productId, String authorizationHeader) {
        return get("/api/v1/inventory/{id}", productId, authorizationHeader, StockLevel.class);
    }

    public Optional<ReorderRecord> getReorderEstimate(Long productId, String authorizationHeader) {
        return get("/api/v1/inventory/{id}/reorder-estimate", productId, authorizationHeader, ReorderRecord.class);
    }

    private <T> Optional<T> get(String uri, Long productId, String authorizationHeader, Class<T> type) {
        try {
            return Optional.ofNullable(inventoryRestClient.get()
                .uri(uri, productId)
                .headers(headers -> {
                    if (authorizationHeader != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                    }
                })
                .retrieve()
                .body(type));
        } catch (RestClientException ex) {
            log.warn("Inventory call {} failed for product {}: {}", uri, productId, ex.getMessage());
            return Optional.empty();
        }
    }
}
