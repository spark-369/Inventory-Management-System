package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.AbstractJobMessage;

/**
 * Base class for every message consumer.
 *
 * <p>Wraps the consumer body so the message audit trail is always updated:
 * the row is flipped to {@code RUNNING} before the work starts and to
 * {@code SUCCEEDED}/{@code FAILED} afterwards. The exception is rethrown so
 * JobRunr can apply its retry and dead-letter policy.</p>
 */
public abstract class AbstractJobConsumer {

    protected final MessageBroker broker;

    protected AbstractJobConsumer(MessageBroker broker) {
        this.broker = broker;
    }

    /**
     * Runs {@code work}, recording start/finish on the message audit row.
     *
     * @return the short result summary reported by the consumer
     */
    protected String handle(AbstractJobMessage message, ConsumerAction work) {
        String correlationId = message.getCorrelationId();
        broker.markRunning(correlationId);
        try {
            String result = work.run();
            broker.markSucceeded(correlationId, result);
            return result;
        } catch (RuntimeException ex) {
            broker.markFailed(correlationId, ex.getMessage());
            throw ex;
        }
    }

    /** The unit of work executed by a consumer. */
    @FunctionalInterface
    protected interface ConsumerAction {
        String run();
    }
}
