package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.ExpiryScanJobRequest;
import com.unique.ims.backend.service.AlertService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer for {@code EXPIRY_SCAN} messages.
 *
 * <p>Bound to {@link ExpiryScanJobRequest} through its
 * {@code getJobRequestHandler()}. Re-runs the batch expiry sweep whenever a stock
 * batch is created or edited, so near-expiry stock is flagged without waiting for
 * the nightly recurring sweep.</p>
 */
@Component
public class ExpiryJobConsumer extends AbstractJobConsumer implements JobRequestHandler<ExpiryScanJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(ExpiryJobConsumer.class);

    private final AlertService alertService;

    public ExpiryJobConsumer(MessageBroker broker, AlertService alertService) {
        super(broker);
        this.alertService = alertService;
    }

    @Override
    @Job(name = "Stock batch expiry scan", retries = 3)
    public void run(ExpiryScanJobRequest request) {
        handle(request, () -> {
            int window = request.getWarningWindowDays() != null
                    ? request.getWarningWindowDays()
                    : AlertService.DEFAULT_EXPIRY_WARNING_WINDOW_DAYS;
            int created = alertService.scanExpiry(window);
            log.info("Expiry scan ({} day window) created {} alert(s)", window, created);
            return created + " expiry alert(s) created (window: " + window + " days)";
        });
    }
}
