package com.unique.ims.backend.messaging.request;

import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.consumer.InventoryJobConsumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message requesting a stock-level refresh for a single product.
 *
 * <p>Published with routing key {@code INVENTORY_SYNC} after a purchase is
 * received, a return is approved or a manual movement is recorded. The consumer
 * recomputes the stock snapshot and emits follow-up alerts if the product has
 * dropped to or below its reorder level.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class InventorySyncJobRequest extends AbstractJobMessage {

    private Long productId;

    public InventorySyncJobRequest(Long productId, String reason, String requestedBy) {
        super(JobMessageType.INVENTORY_SYNC, reason, requestedBy);
        this.productId = productId;
    }

    public static InventorySyncJobRequest forProduct(Long productId, String reason, String requestedBy) {
        return new InventorySyncJobRequest(productId, reason, requestedBy);
    }

    @Override
    public Class<InventoryJobConsumer> getJobRequestHandler() {
        return InventoryJobConsumer.class;
    }
}

