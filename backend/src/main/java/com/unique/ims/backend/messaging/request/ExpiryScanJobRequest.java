package com.unique.ims.backend.messaging.request;

import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.consumer.ExpiryJobConsumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message requesting an expiry sweep over every stock batch.
 *
 * <p>Published with routing key {@code EXPIRY_SCAN} whenever a batch is created
 * or updated, so expired / near-expiry stock surfaces without waiting for the
 * nightly recurring run.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ExpiryScanJobRequest extends AbstractJobMessage {

    /** Batches expiring within this many days are flagged as warnings. */
    private Integer warningWindowDays;

    public ExpiryScanJobRequest(Integer warningWindowDays, String reason, String requestedBy) {
        super(JobMessageType.EXPIRY_SCAN, reason, requestedBy);
        this.warningWindowDays = warningWindowDays;
    }

    public static ExpiryScanJobRequest defaultWindow(String reason, String requestedBy) {
        return new ExpiryScanJobRequest(7, reason, requestedBy);
    }

    @Override
    public Class<ExpiryJobConsumer> getJobRequestHandler() {
        return ExpiryJobConsumer.class;
    }
}

