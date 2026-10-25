package com.unique.ims.backend.messaging.request;

import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.consumer.AlertJobConsumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message requesting an inventory alert sweep.
 *
 * <p>Published with routing key {@code ALERT_SCAN} for a full sweep, or
 * {@code LOW_STOCK_SCAN} when {@code productId} is set to re-check a single
 * product. JobRunr serialises this payload as JSON and stores it in the
 * database until a worker picks it up, so every field must stay JSON-friendly
 * and the class needs a no-argument constructor.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AlertScanJobRequest extends AbstractJobMessage {

    /** When set, only this product is re-evaluated instead of the whole catalogue. */
    private Long productId;

    public AlertScanJobRequest(Long productId, JobMessageType messageType, String reason, String requestedBy) {
        super(messageType, reason, requestedBy);
        this.productId = productId;
    }

    /** Full catalogue sweep, routed as {@code ALERT_SCAN}. */
    public static AlertScanJobRequest fullSweep(String reason, String requestedBy) {
        return new AlertScanJobRequest(null, JobMessageType.ALERT_SCAN, reason, requestedBy);
    }

    /** Single-product re-check, routed as {@code LOW_STOCK_SCAN}. */
    public static AlertScanJobRequest forProduct(Long productId, String reason, String requestedBy) {
        return new AlertScanJobRequest(productId, JobMessageType.LOW_STOCK_SCAN, reason, requestedBy);
    }

    @Override
    public Class<AlertJobConsumer> getJobRequestHandler() {
        return AlertJobConsumer.class;
    }
}

