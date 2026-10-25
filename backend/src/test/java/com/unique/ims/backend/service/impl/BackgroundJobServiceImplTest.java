package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.AbstractJobMessage;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.messaging.request.InventorySyncJobRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the manual message dispatch used by the Background Jobs console.
 *
 * <p>Each routing key must publish the correct message type, and the two
 * identifiers that need a product must reject a missing id rather than enqueue a
 * message a consumer would later fail on.</p>
 */
@ExtendWith(MockitoExtension.class)
class BackgroundJobServiceImplTest {

    @Mock
    private MessageBroker broker;

    @InjectMocks
    private BackgroundJobServiceImpl service;

    @Test
    @DisplayName("ALERT_SCAN publishes a full sweep with no product id")
    void dispatchAlertScan() {
        when(broker.publish(any(AbstractJobMessage.class))).thenReturn("corr-1");

        String correlationId = service.dispatch(JobMessageType.ALERT_SCAN, null);

        assertThat(correlationId).isEqualTo("corr-1");
        AbstractJobMessage published = capturedMessage();
        assertThat(published.getMessageType()).isEqualTo(JobMessageType.ALERT_SCAN);
    }

    @Test
    @DisplayName("LOW_STOCK_SCAN carries the requested product id")
    void dispatchLowStockScanCarriesProductId() {
        when(broker.publish(any(AbstractJobMessage.class))).thenReturn("corr-2");

        service.dispatch(JobMessageType.LOW_STOCK_SCAN, 42L);

        AbstractJobMessage published = capturedMessage();
        assertThat(published).isInstanceOf(AlertScanJobRequest.class);
        assertThat(((AlertScanJobRequest) published).getProductId()).isEqualTo(42L);
        assertThat(published.getMessageType()).isEqualTo(JobMessageType.LOW_STOCK_SCAN);
    }

    @Test
    @DisplayName("EXPIRY_SCAN publishes the default warning window")
    void dispatchExpiryScan() {
        when(broker.publish(any(AbstractJobMessage.class))).thenReturn("corr-3");

        service.dispatch(JobMessageType.EXPIRY_SCAN, null);

        assertThat(capturedMessage().getMessageType()).isEqualTo(JobMessageType.EXPIRY_SCAN);
    }

    @Test
    @DisplayName("INVENTORY_SYNC requires a product id")
    void dispatchInventorySyncRequiresProduct() {
        when(broker.publish(any(AbstractJobMessage.class))).thenReturn("corr-4");

        service.dispatch(JobMessageType.INVENTORY_SYNC, 7L);

        AbstractJobMessage published = capturedMessage();
        assertThat(published).isInstanceOf(InventorySyncJobRequest.class);
        assertThat(((InventorySyncJobRequest) published).getProductId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("DAILY_INVENTORY_REPORT publishes without a product id")
    void dispatchDailyReport() {
        when(broker.publish(any(AbstractJobMessage.class))).thenReturn("corr-5");

        service.dispatch(JobMessageType.DAILY_INVENTORY_REPORT, null);

        assertThat(capturedMessage().getMessageType()).isEqualTo(JobMessageType.DAILY_INVENTORY_REPORT);
    }

    @Test
    @DisplayName("rejects a null message type")
    void rejectsNullType() {
        assertThatThrownBy(() -> service.dispatch(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message type is required");

        verify(broker, never()).publish(any());
    }

    @Test
    @DisplayName("rejects LOW_STOCK_SCAN without a product id")
    void rejectsLowStockScanWithoutProduct() {
        assertThatThrownBy(() -> service.dispatch(JobMessageType.LOW_STOCK_SCAN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a productId");

        verify(broker, never()).publish(any());
    }

    @Test
    @DisplayName("rejects INVENTORY_SYNC without a product id")
    void rejectsInventorySyncWithoutProduct() {
        assertThatThrownBy(() -> service.dispatch(JobMessageType.INVENTORY_SYNC, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a productId");
    }

    @Test
    @DisplayName("refuses to dispatch ORDER_CONFIRMATION, which only the sales flow may produce")
    void refusesOrderConfirmation() {
        assertThatThrownBy(() -> service.dispatch(JobMessageType.ORDER_CONFIRMATION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sales workflow");

        verify(broker, never()).publish(any());
    }

    private AbstractJobMessage capturedMessage() {
        ArgumentCaptor<AbstractJobMessage> captor = ArgumentCaptor.forClass(AbstractJobMessage.class);
        verify(broker).publish(captor.capture());
        return captor.getValue();
    }
}
