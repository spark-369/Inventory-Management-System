package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.InventoryReportJobRequest;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.repository.ProductRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumer for {@code DAILY_INVENTORY_REPORT} messages.
 *
 * <p>Walks the catalogue to produce a valuation digest: total stock value plus the
 * number of products sitting at or below their reorder level.</p>
 */
@Component
public class InventoryReportJobConsumer extends AbstractJobConsumer
        implements JobRequestHandler<InventoryReportJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(InventoryReportJobConsumer.class);

    private final ProductRepository productRepository;

    public InventoryReportJobConsumer(MessageBroker broker, ProductRepository productRepository) {
        super(broker);
        this.productRepository = productRepository;
    }

    @Override
    @Job(name = "Daily inventory valuation report", retries = 2)
    public void run(InventoryReportJobRequest request) {
        handle(request, () -> {
            BigDecimal value = BigDecimal.ZERO;
            int lowStock = 0;
            int total = 0;

            for (Product product : productRepository.findAll()) {
                total++;
                int stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
                BigDecimal price = product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO;
                value = value.add(price.multiply(BigDecimal.valueOf(stock)));

                if (product.getReorderLevel() != null && stock <= product.getReorderLevel()) {
                    lowStock++;
                }
            }

            String summary = "Inventory digest (" + request.getLabel() + "): " + total + " product(s), value "
                    + value + ", " + lowStock + " at or below reorder level";
            log.info("{}", summary);
            return summary;
        });
    }
}
