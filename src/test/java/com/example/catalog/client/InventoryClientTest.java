package com.example.catalog.client;

import com.example.catalog.model.ReorderRecord;
import com.example.catalog.model.StockLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InventoryClientTest {

    private static final String BASE_URL = "http://inventory.test";
    private static final String BEARER = "Bearer test-token";

    private MockRestServiceServer server;
    private InventoryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new InventoryClient(builder.build());
    }

    @Test
    void getStock_success_forwardsAuthorizationAndParsesBody() {
        server.expect(requestTo(BASE_URL + "/api/v1/inventory/1"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER))
            .andRespond(withSuccess("""
                {"id":7,"productId":1,"productName":"Laptop Pro 15","reorderThreshold":3,
                 "reorderQuantity":20,"status":"LOW_STOCK","lastUpdated":"2026-09-25T10:15:30"}
                """, MediaType.APPLICATION_JSON));

        Optional<StockLevel> result = client.getStock(1L, BEARER);

        assertThat(result).contains(new StockLevel(
            7L, 1L, "Laptop Pro 15", 3, 20, "LOW_STOCK", LocalDateTime.of(2026, 9, 25, 10, 15, 30)));
        server.verify();
    }

    @Test
    void getStock_serverError_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/api/v1/inventory/1")).andRespond(withServerError());

        assertThat(client.getStock(1L, BEARER)).isEmpty();
    }

    @Test
    void getStock_notFound_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/api/v1/inventory/1")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.getStock(1L, BEARER)).isEmpty();
    }

    @Test
    void getStock_timeout_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/api/v1/inventory/1"))
            .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(client.getStock(1L, BEARER)).isEmpty();
    }

    @Test
    void getReorderEstimate_success_parsesBody() {
        server.expect(requestTo(BASE_URL + "/api/v1/inventory/1/reorder-estimate"))
            .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER))
            .andRespond(withSuccess("""
                {"productId":1,"productName":"Laptop Pro 15","reorderQuantity":20,
                 "unitPriceUsd":1299.99,"totalCostUsd":25999.80,"totalCostConverted":25999.80,"currency":"USD"}
                """, MediaType.APPLICATION_JSON));

        Optional<ReorderRecord> result = client.getReorderEstimate(1L, BEARER);

        assertThat(result).isPresent();
        assertThat(result.get().reorderQuantity()).isEqualTo(20);
        assertThat(result.get().totalCostUsd()).isEqualByComparingTo(new BigDecimal("25999.80"));
        assertThat(result.get().currency()).isEqualTo("USD");
    }

    @Test
    void getReorderEstimate_timeout_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/api/v1/inventory/1/reorder-estimate"))
            .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(client.getReorderEstimate(1L, BEARER)).isEmpty();
    }
}
