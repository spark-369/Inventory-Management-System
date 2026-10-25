package com.unique.ims.backend.messaging.request;

import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.consumer.InventoryReportJobConsumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message requesting a daily inventory valuation digest.
 *
 * <p>Published with routing key {@code DAILY_INVENTORY_REPORT} by the recurring
 * schedule. Carries no domain identifiers because the consumer reads the whole
 * catalogue.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class InventoryReportJobRequest extends AbstractJobMessage {

    private String label;

    public InventoryReportJobRequest(String label, String requestedBy) {
        super(JobMessageType.DAILY_INVENTORY_REPORT, "Scheduled inventory digest", requestedBy);
        this.label = label;
    }

    public static InventoryReportJobRequest daily(String requestedBy) {
        return new InventoryReportJobRequest("Daily inventory digest", requestedBy);
    }

    @Override
    public Class<InventoryReportJobConsumer> getJobRequestHandler() {
        return InventoryReportJobConsumer.class;
    }
}

