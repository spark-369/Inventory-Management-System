package com.unique.ims.backend.messaging;

import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the after-commit publishing rule.
 *
 * <p>This is what prevents the classic dual-write bug: a message must not be
 * enqueued while the surrounding business transaction is still uncommitted, or a
 * worker could pick it up and read data that is about to be rolled back.</p>
 */
@ExtendWith(MockitoExtension.class)
class DomainMessagePublisherTest {

    @Mock
    private MessageBroker broker;

    private DomainMessagePublisher publisher;

    @AfterEach
    void clearSynchronization() {
        // The test that registers a synchronization must not leak it into others.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private AlertScanJobRequest message() {
        return AlertScanJobRequest.fullSweep("unit-test", "unit-test");
    }

    @Test
    @DisplayName("publishes immediately when there is no active transaction")
    void publishesImmediatelyWithoutTransaction() {
        publisher = new DomainMessagePublisher(broker);
        when(broker.publish(any(AlertScanJobRequest.class))).thenReturn("corr-1");

        String correlationId = publisher.publishAfterCommit(message());

        assertThat(correlationId).isEqualTo("corr-1");
        verify(broker).publish(any(AlertScanJobRequest.class));
    }

    @Test
    @DisplayName("defers the publish until the transaction commits")
    void defersPublishUntilCommit() {
        publisher = new DomainMessagePublisher(broker);
        TransactionSynchronizationManager.initSynchronization();

        String correlationId = publisher.publishAfterCommit(message());

        // Not published yet — the transaction is still open.
        assertThat(correlationId).isNull();
        verify(broker, never()).publish(any());

        // Simulate the commit: the registered callback must now publish.
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(broker).publish(any(AlertScanJobRequest.class));
    }

    @Test
    @DisplayName("defers a scheduled message until commit as well")
    void defersScheduleUntilCommit() {
        publisher = new DomainMessagePublisher(broker);
        TransactionSynchronizationManager.initSynchronization();

        publisher.scheduleAfterCommit(message(), Duration.ofSeconds(5));
        verify(broker, never()).schedule(any(), any(Duration.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(broker).schedule(any(AlertScanJobRequest.class), eq(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("schedules immediately when there is no active transaction")
    void schedulesImmediatelyWithoutTransaction() {
        publisher = new DomainMessagePublisher(broker);
        when(broker.schedule(any(AlertScanJobRequest.class), any(Duration.class))).thenReturn("corr-2");

        String correlationId = publisher.scheduleAfterCommit(message(), Duration.ofSeconds(5));

        assertThat(correlationId).isEqualTo("corr-2");
        verify(broker).schedule(any(AlertScanJobRequest.class), any(Duration.class));
    }
}
