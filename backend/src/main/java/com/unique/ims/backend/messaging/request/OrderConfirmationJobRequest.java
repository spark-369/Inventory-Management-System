package com.unique.ims.backend.messaging.request;

import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.consumer.NotificationJobConsumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Message requesting an order-confirmation notification for a sale.
 *
 * <p>Published with routing key {@code ORDER_CONFIRMATION}. Only identifiers and
 * a denormalised snapshot travel through the broker — never JPA entities — so
 * the message stays small and remains valid even if the sale is edited later.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderConfirmationJobRequest extends AbstractJobMessage {

    private Long saleId;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal grandTotal;
    private Integer itemCount;
    /** ISO-8601 representation of the sale date. */
    private String saleDate;

    public OrderConfirmationJobRequest(Long saleId, String invoiceNumber, Long customerId, String customerName,
                                       String customerEmail, BigDecimal grandTotal, Integer itemCount, String saleDate) {
        super(JobMessageType.ORDER_CONFIRMATION, "Sale " + invoiceNumber + " registered", "sales-api");
        this.saleId = saleId;
        this.invoiceNumber = invoiceNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.grandTotal = grandTotal;
        this.itemCount = itemCount;
        this.saleDate = saleDate;
    }

    @Override
    public Class<NotificationJobConsumer> getJobRequestHandler() {
        return NotificationJobConsumer.class;
    }
}

