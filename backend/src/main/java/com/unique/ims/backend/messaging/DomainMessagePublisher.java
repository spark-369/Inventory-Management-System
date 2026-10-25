package com.unique.ims.backend.messaging;

import com.unique.ims.backend.messaging.request.AbstractJobMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

/**
 * Publishes messages only after the surrounding database transaction commits.
 *
 * <p>Without this, a message could be enqueued and picked up by a worker before
 * the business change is visible — the classic dual-write problem. Domain services
 * call these methods from inside their {@code @Transactional} methods; the actual
 * {@code broker.publish(...)} runs in the after-commit phase, and immediately when
 * no transaction is active.</p>
 */
@Component
public class DomainMessagePublisher {

    private final MessageBroker broker;

    public DomainMessagePublisher(MessageBroker broker) {
        this.broker = broker;
    }

    /**
     * Publishes the message after the current transaction commits, or immediately
     * when called outside a transaction.
     *
     * @return the correlation id when published immediately, otherwise {@code null}
     *         because the id is only assigned once the commit succeeds
     */
    public String publishAfterCommit(AbstractJobMessage message) {
        return afterCommit(() -> broker.publish(message));
    }

    /**
     * Schedules the message after the current transaction commits, or immediately
     * when called outside a transaction. Useful to debounce a burst of related
     * changes into a single delayed message.
     *
     * @return the correlation id when scheduled immediately, otherwise {@code null}
     */
    public String scheduleAfterCommit(AbstractJobMessage message, Duration delay) {
        return afterCommit(() -> broker.schedule(message, delay));
    }

    private String afterCommit(java.util.function.Supplier<String> action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.get();
                }
            });
            return null;
        }
        return action.get();
    }
}
