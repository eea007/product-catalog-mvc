package com.example.catalog.messaging;

import com.example.catalog.config.AwsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The SNS message must keep REACTIVE's STOCK_DEPLETED shape so inventory-service can consume either service. */
class StockEventPublisherTest {

    private static final String TOPIC = "arn:aws:sns:us-east-2:325879634980:stock-depleted-topic";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SnsClient snsClient;
    private StockEventPublisher publisher;

    @BeforeEach
    void setUp() {
        snsClient = mock(SnsClient.class);
        AwsProperties awsProperties = new AwsProperties("us-east-2",
            new AwsProperties.S3("bucket"), new AwsProperties.Sns(TOPIC));
        publisher = new StockEventPublisher(snsClient, awsProperties, objectMapper);
    }

    @Test
    void publishStockDepleted_sendsReactiveCompatibleMessage() throws Exception {
        when(snsClient.publish(any(PublishRequest.class)))
            .thenReturn(PublishResponse.builder().messageId("m-1").build());

        publisher.publishStockDepleted(1L, "Desk \"Pro\"");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();

        assertThat(request.topicArn()).isEqualTo(TOPIC);
        assertThat(request.subject()).isEqualTo("STOCK_DEPLETED");
        assertThat(request.messageAttributes()).isEmpty();

        JsonNode body = objectMapper.readTree(request.message());
        List<String> fields = objectMapper.convertValue(body.fieldNames(), List.class);
        assertThat(fields).containsExactly("event", "productId", "productName", "timestamp");
        assertThat(body.get("event").asText()).isEqualTo("STOCK_DEPLETED");
        assertThat(body.get("productId").isNumber()).isTrue();
        assertThat(body.get("productId").asLong()).isEqualTo(1L);
        assertThat(body.get("productName").asText()).isEqualTo("Desk \"Pro\"");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void publishStockDepleted_rethrowsSnsFailure() {
        when(snsClient.publish(any(PublishRequest.class)))
            .thenThrow(SnsException.builder().message("boom").build());

        assertThatThrownBy(() -> publisher.publishStockDepleted(1L, "Desk"))
            .isInstanceOf(SnsException.class);
    }

    @Test
    void listener_swallowsSnsFailureAfterCommit() {
        when(snsClient.publish(any(PublishRequest.class)))
            .thenThrow(SnsException.builder().message("boom").build());
        StockDepletedEventListener listener = new StockDepletedEventListener(publisher);

        listener.onStockDepleted(new com.example.catalog.model.StockDepletedEvent(1L, "Desk"));

        verify(snsClient).publish(any(PublishRequest.class));
    }
}
