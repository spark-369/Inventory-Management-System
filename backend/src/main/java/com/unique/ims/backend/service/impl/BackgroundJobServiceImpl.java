package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.dto.JobMessageLogDto;
import com.unique.ims.backend.dto.JobScheduleDto;
import com.unique.ims.backend.dto.JobStatsDto;
import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.messaging.MessageBroker;
import com.unique.ims.backend.messaging.request.AlertScanJobRequest;
import com.unique.ims.backend.messaging.request.ExpiryScanJobRequest;
import com.unique.ims.backend.messaging.request.InventoryReportJobRequest;
import com.unique.ims.backend.messaging.request.InventorySyncJobRequest;
import com.unique.ims.backend.service.BackgroundJobService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges the REST layer to the {@link MessageBroker}. Keeping the dispatch
 * switch here means new {@link JobMessageType} values become callable from the
 * console with a single line.
 */
@Service
public class BackgroundJobServiceImpl implements BackgroundJobService {

    private final MessageBroker broker;

    public BackgroundJobServiceImpl(MessageBroker broker) {
        this.broker = broker;
    }

    @Override
    public JobStatsDto getStats() {
        return broker.getStats();
    }

    @Override
    public List<JobScheduleDto> getSchedules() {
        return broker.getSchedules();
    }

    @Override
    public List<JobMessageLogDto> getRecentMessages(int limit) {
        return broker.getRecentMessages(limit);
    }

    @Override
    public String dispatch(JobMessageType type, Long productId) {
        if (type == null) {
            throw new IllegalArgumentException("Message type is required.");
        }
        return switch (type) {
            case ALERT_SCAN -> broker.publish(AlertScanJobRequest.fullSweep("Manual trigger", "console"));
            case LOW_STOCK_SCAN -> broker.publish(AlertScanJobRequest.forProduct(requireProductId(type, productId),
                    "Manual trigger", "console"));
            case EXPIRY_SCAN -> broker.publish(ExpiryScanJobRequest.defaultWindow("Manual trigger", "console"));
            case INVENTORY_SYNC -> broker.publish(InventorySyncJobRequest.forProduct(requireProductId(type, productId),
                    "Manual trigger", "console"));
            case ORDER_CONFIRMATION -> throw new IllegalArgumentException(
                    "ORDER_CONFIRMATION messages are produced by the sales workflow, not the console.");
            case DAILY_INVENTORY_REPORT -> broker.publish(InventoryReportJobRequest.daily("console"));
        };
    }

    private Long requireProductId(JobMessageType type, Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException(type.name() + " requires a productId.");
        }
        return productId;
    }
}
