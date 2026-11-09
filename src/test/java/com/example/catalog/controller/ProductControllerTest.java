package com.example.catalog.controller;

import com.example.catalog.config.SecurityConfig;
import com.example.catalog.model.ProductRequest;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.ProductWithPrice;
import com.example.catalog.service.ProductImageService;
import com.example.catalog.service.ProductNotFoundException;
import com.example.catalog.service.ProductPricingService;
import com.example.catalog.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Mirrors REACTIVE's ProductHandlerTest. Unlike that test (bound straight to
 * the router, so security never ran), the real SecurityConfig filter chain is
 * active here, and requests authenticate with a mock JWT.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductPricingService productPricingService;

    @MockitoBean
    private ProductImageService productImageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private ProductResponse product(long id) {
        return new ProductResponse(id, "Test Product", "Electronics", new BigDecimal("99.99"), 10);
    }

    private ProductRequest request() {
        return new ProductRequest("Test Product", "Electronics", new BigDecimal("99.99"), 10);
    }

    // ── Mirrors of REACTIVE's 9 handler tests ─────────────────────────────

    @Test
    void getAll_returns200WithListOfProducts() throws Exception {
        when(productService.findAll()).thenReturn(List.of(product(1L), product(2L)));

        mockMvc.perform(get("/api/v1/products").with(jwt()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_returns200WithProductWhenFound() throws Exception {
        when(productService.findById(1L)).thenReturn(product(1L));

        mockMvc.perform(get("/api/v1/products/1").with(jwt()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Product"))
            .andExpect(jsonPath("$.category").value("Electronics"))
            .andExpect(jsonPath("$.price").value(99.99))
            .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/v1/products/99").with(jwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getByCategory_returns200WithFilteredList() throws Exception {
        when(productService.findByCategory("Electronics")).thenReturn(List.of(product(1L)));

        mockMvc.perform(get("/api/v1/products/category/Electronics").with(jwt()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void create_returns201WithCreatedProduct() throws Exception {
        when(productService.create(any())).thenReturn(product(1L));

        mockMvc.perform(post("/api/v1/products").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void update_returns200WithUpdatedProduct() throws Exception {
        when(productService.update(eq(1L), any())).thenReturn(product(1L));

        mockMvc.perform(put("/api/v1/products/1").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(productService.update(eq(99L), any())).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/api/v1/products/99").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1").with(jwt()))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        doThrow(new ProductNotFoundException(99L)).when(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99").with(jwt()))
            .andExpect(status().isNotFound());
    }

    // ── MVC-only endpoints ────────────────────────────────────────────────

    @Test
    void count_returns200WithTotalAndDoesNotHitIdMapping() throws Exception {
        when(productService.countProducts()).thenReturn(42L);

        mockMvc.perform(get("/api/v1/products/count").with(jwt()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"count\":42}", true));

        verify(productService, never()).findById(any());
    }

    @Test
    void count_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/products/count"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    // ── Security ──────────────────────────────────────────────────────────

    @Test
    void getAll_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        // Actuator endpoints are not part of this slice, so a permitted request 404s; it must not 401.
        mockMvc.perform(get("/actuator/health"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void corsPreflight_isPermittedWithoutTokenForAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    // ── Validation and the price contract ─────────────────────────────────

    @Test
    void create_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/v1/products").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"Electronics\",\"price\":9.99,\"stockQuantity\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(productService);
    }

    @Test
    void getPrice_returnsProductWithPriceIncludingConvertedFlag() throws Exception {
        when(productPricingService.getProductWithPrice(1L, "EUR")).thenReturn(new ProductWithPrice(
            1L, "Test Product", "Electronics", new BigDecimal("99.99"), new BigDecimal("91.50"), "EUR", 10, true));

        mockMvc.perform(get("/api/v1/products/1/price").param("currency", "EUR").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.originalPriceUsd").value(99.99))
            .andExpect(jsonPath("$.convertedPrice").value(91.50))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.converted").value(true));
    }
}
