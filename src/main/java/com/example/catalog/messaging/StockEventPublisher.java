package com.example.catalog.messaging;

import com.example.catalog.config.AwsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventPublisher {

    private final SnsClient snsClient;
    private final AwsProperties awsProperties;
    private final ObjectMapper objectMapper;

    /**
     * Publishes the same STOCK_DEPLETED message as REACTIVE: subject
     * "STOCK_DEPLETED", no message attributes, body fields event / productId /
     * productName / timestamp in that order. The body is serialized with
     * Jackson rather than String.formatted so productName is properly escaped.
     */
    public void publishStockDepleted(Long productId, String productName) {
        PublishRequest request = PublishRequest.builder()
            .topicArn(awsProperties.sns().stockDepletedTopicArn())
            .message(body(productId, productName))
            .subject("STOCK_DEPLETED")
            .build();

        try {
            PublishResponse response = snsClient.publish(request);
            log.info("Published StockDepleted event for product {} - messageId: {}",
                productId, response.messageId());
        } catch (SdkException e) {
            log.error("Failed to publish StockDepleted event for product {}: {}",
                productId, e.getMessage());
            throw e;
        }
    }

    private String body(Long productId, String productName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "STOCK_DEPLETED");
        body.put("productId", productId);
        body.put("productName", productName);
        body.put("timestamp", Instant.now().toString());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize STOCK_DEPLETED event", e);
        }
    }
}
