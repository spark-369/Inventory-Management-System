package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.AlertDto;
import java.util.List;

public interface AlertService {

    /** Batches expiring within this many days raise an {@code EXPIRY_WARNING}. */
    int DEFAULT_EXPIRY_WARNING_WINDOW_DAYS = 7;

    List<AlertDto> getAllAlerts();
    List<AlertDto> getUnresolvedAlerts();
    List<AlertDto> getAlertsByProduct(Long productId);
    AlertDto resolveAlert(Long id);
    AlertDto createAlert(AlertDto alertDto);

    /**
     * Low-stock sweep across the whole catalogue.
     *
     * @return the number of alerts created
     */
    int scanLowStock();

    /**
     * Low-stock re-check for a single product, used by the asynchronous
     * {@code LOW_STOCK_SCAN} message consumer.
     *
     * @return 1 when an alert was created, otherwise 0
     */
    int scanLowStockForProduct(Long productId);

    /**
     * Expiry sweep across every stock batch.
     *
     * @param warningWindowDays batches expiring within this many days raise a warning
     * @return the number of alerts created
     */
    int scanExpiry(int warningWindowDays);
}
