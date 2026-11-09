package com.example.catalog.messaging;

import com.example.catalog.model.StockDepletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Deliberate difference from REACTIVE: REACTIVE published to SNS inside the
 * update chain right after save, so an SNS failure turned an already-saved
 * update into a 500. Here ProductService publishes a StockDepletedEvent and
 * the SNS call happens only after the transaction commits. An SNS failure is
 * logged and does not change the caller's response.
 */
@Component
@RequiredArgsConstructor
public class StockDepletedEventListener {

    private final StockEventPublisher stockEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockDepleted(StockDepletedEvent event) {
        try {
            stockEventPublisher.publishStockDepleted(event.productId(), event.productName());
        } catch (SdkException e) {
            // Already logged by StockEventPublisher; the update is committed, so there is nothing to roll back.
        }
    }
}
