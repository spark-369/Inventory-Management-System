package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.JobSchedules;
import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.messaging.request.InventoryReportJobRequest;
import org.jobrunr.jobs.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registers the application's recurring message schedules with JobRunr.
 *
 * <p>JobRunr only keeps a recurring job alive while at least one
 * {@code BackgroundJobServer} runs, and schedules must be registered at startup.
 * Each {@code @Recurring} method simply publishes a message through the broker,
 * so the actual work still happens on a worker through the normal consumer path —
 * the recurring methods themselves stay trivial and fast.</p>
 *
 * <p>This replaces the previous in-process {@code @Scheduled} alert scan: the
 * schedule now survives restarts, is visible in the dashboard, and can be
 * triggered on demand from the frontend console.</p>
 */
@Component
public class RecurringJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(RecurringJobPublisher.class);

    private final MessageBroker broker;

    public RecurringJobPublisher(MessageBroker broker) {
        this.broker = broker;
    }

    /** Nightly low-stock sweep across the whole catalogue. */
    @Recurring(id = JobSchedules.INVENTORY_ALERT_SWEEP_ID, cron = JobSchedules.INVENTORY_ALERT_SWEEP_CRON)
    public void publishInventoryAlertSweep() {
        String correlationId = broker.publish(AlertScanJobRequest.fullSweep("Nightly schedule", "scheduler"));
        log.info("Recurring alert sweep published as [{}]", correlationId);
    }

    /** Morning inventory valuation digest. */
    @Recurring(id = JobSchedules.INVENTORY_REPORT_ID, cron = JobSchedules.INVENTORY_REPORT_CRON)
    public void publishInventoryReport() {
        String correlationId = broker.publish(InventoryReportJobRequest.daily("scheduler"));
        log.info("Recurring inventory report published as [{}]", correlationId);
    }
}
