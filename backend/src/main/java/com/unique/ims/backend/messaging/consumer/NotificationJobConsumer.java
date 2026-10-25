package com.unique.ims.backend.messaging.consumer;

import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.OrderConfirmationJobRequest;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer for {@code ORDER_CONFIRMATION} messages.
 *
 * <p>Demonstrates the notification branch of the message flow. The actual
 * outbound channel (SMTP, webhook, …) is left pluggable: this consumer performs
 * the templating and logging that would otherwise block the sale request.</p>
 */
@Component
public class NotificationJobConsumer extends AbstractJobConsumer
        implements JobRequestHandler<OrderConfirmationJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(NotificationJobConsumer.class);

    public NotificationJobConsumer(MessageBroker broker) {
        super(broker);
    }

    @Override
    @Job(name = "Order confirmation notification", retries = 5)
    public void run(OrderConfirmationJobRequest request) {
        handle(request, () -> {
            String recipient = request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()
                    ? request.getCustomerEmail()
                    : request.getCustomerName();

            String summary = "Order confirmation queued for " + (recipient == null ? "walk-in customer" : recipient)
                    + " (invoice " + request.getInvoiceNumber()
                    + ", " + request.getItemCount() + " item(s), total " + request.getGrandTotal() + ")";

            log.info("{}", summary);
            return summary;
        });
    }
}
