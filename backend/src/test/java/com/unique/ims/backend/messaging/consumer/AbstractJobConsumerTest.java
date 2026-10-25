package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for the audit-trail behaviour every consumer inherits.
 *
 * <p>This is the code that keeps the message history honest, so the assertions
 * are about ordering and failure handling: the row must be marked RUNNING before
 * the work starts, SUCCEEDED with a summary when it finishes, and FAILED when it
 * throws — with the exception still propagating so JobRunr can retry.</p>
 */
@ExtendWith(MockitoExtension.class)
class AbstractJobConsumerTest {

    @Mock
    private MessageBroker broker;

    /** Minimal concrete consumer so the protected template method can be exercised. */
    private static final class TestConsumer extends AbstractJobConsumer {

        TestConsumer(MessageBroker broker) {
            super(broker);
        }

        String runWith(AlertScanJobRequest message, ConsumerAction action) {
            return handle(message, action);
        }
    }

    private AlertScanJobRequest message() {
        AlertScanJobRequest request = AlertScanJobRequest.fullSweep("test reason", "unit-test");
        request.setCorrelationId("corr-123");
        return request;
    }

    @Test
    @DisplayName("marks the message RUNNING then SUCCEEDED, and returns the result summary")
    void marksRunningThenSucceeded() {
        TestConsumer consumer = new TestConsumer(broker);

        String result = consumer.runWith(message(), () -> "3 alerts created");

        assertThat(result).isEqualTo("3 alerts created");
        verify(broker).markRunning("corr-123");
        verify(broker).markSucceeded("corr-123", "3 alerts created");
        verifyNoMoreInteractions(broker);
    }

    @Test
    @DisplayName("marks the message FAILED and rethrows so JobRunr can retry it")
    void marksFailedAndRethrows() {
        TestConsumer consumer = new TestConsumer(broker);
        IllegalStateException boom = new IllegalStateException("database down");

        assertThatThrownBy(() -> consumer.runWith(message(), () -> {
            throw boom;
        }))
                .isSameAs(boom);

        verify(broker).markRunning("corr-123");
        verify(broker).markFailed("corr-123", "database down");
        verifyNoMoreInteractions(broker);
    }

    @Test
    @DisplayName("still records a failure when the consumer returns null-free but throws late")
    void failureWinsOverNothingRecorded() {
        TestConsumer consumer = new TestConsumer(broker);

        assertThatThrownBy(() -> consumer.runWith(message(), () -> {
            throw new IllegalArgumentException("bad payload");
        })).isInstanceOf(IllegalArgumentException.class);

        verify(broker).markFailed("corr-123", "bad payload");
    }
}
