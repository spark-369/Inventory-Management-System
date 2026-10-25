package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.InventorySyncJobRequest;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.repository.ProductRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer for {@code INVENTORY_SYNC} messages.
 *
 * <p>Recomputes the stock snapshot of a single product and reports whether it now
 * sits at or below its reorder level. Published after purchases are received,
 * returns are approved or manual stock movements are recorded.</p>
 */
@Component
public class InventoryJobConsumer extends AbstractJobConsumer
        implements JobRequestHandler<InventorySyncJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(InventoryJobConsumer.class);

    private final ProductRepository productRepository;

    public InventoryJobConsumer(MessageBroker broker, ProductRepository productRepository) {
        super(broker);
        this.productRepository = productRepository;
    }

    @Override
    @Job(name = "Inventory synchronisation", retries = 3)
    public void run(InventorySyncJobRequest request) {
        handle(request, () -> {
            Long productId = request.getProductId();
            if (productId == null) {
                throw new IllegalArgumentException("Inventory sync requires a product id.");
            }
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

            int stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            Integer reorderLevel = product.getReorderLevel();
            boolean low = reorderLevel != null && stock <= reorderLevel;

            String summary = "Product '" + product.getName() + "' (SKU " + product.getSku() + ") snapshot refreshed: "
                    + stock + " in stock" + (low ? " — at or below reorder level" : "");
            log.info("{}", summary);
            return summary;
        });
    }
}
