package com.example.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** cors.allowed-origins: browser origins allowed to call the API (local dev UIs). */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
