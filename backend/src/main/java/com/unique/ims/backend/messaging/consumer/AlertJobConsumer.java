package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.service.AlertService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer for {@code ALERT_SCAN} and {@code LOW_STOCK_SCAN} messages.
 *
 * <p>JobRunr resolves this Spring bean through
 * {@link AlertScanJobRequest#getJobRequestHandler()} and invokes {@link #run} on a
 * background worker, off the HTTP request thread.</p>
 */
@Component
public class AlertJobConsumer extends AbstractJobConsumer implements JobRequestHandler<AlertScanJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(AlertJobConsumer.class);

    private final AlertService alertService;

    public AlertJobConsumer(MessageBroker broker, AlertService alertService) {
        super(broker);
        this.alertService = alertService;
    }

    @Override
    @Job(name = "Inventory alert scan", retries = 3)
    public void run(AlertScanJobRequest request) {
        handle(request, () -> {
            if (request.getProductId() != null) {
                int created = alertService.scanLowStockForProduct(request.getProductId());
                log.info("Low-stock scan for product {} created {} alert(s)", request.getProductId(), created);
                return created + " low-stock alert(s) created for product " + request.getProductId();
            }
            int lowStock = alertService.scanLowStock();
            log.info("Low-stock sweep created {} alert(s)", lowStock);
            return lowStock + " low-stock alert(s) created";
        });
    }
}
