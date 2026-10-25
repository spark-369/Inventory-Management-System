package com.unique.ims.backend.messaging;

import com.unique.ims.backend.dto.JobScheduleDto;

import java.util.List;

/**
 * Single source of truth for the application's recurring message schedules.
 *
 * <p>The {@link com.unique.ims.backend.messaging.consumer.RecurringJobPublisher}
 * registers these with JobRunr at startup, and the frontend console lists them so
 * operators can see exactly what the broker runs on a timer.</p>
 */
public final class JobSchedules {

    /** Nightly low-stock + expiry sweep. */
    public static final String INVENTORY_ALERT_SWEEP_ID = "ims-inventory-alert-sweep";
    public static final String INVENTORY_ALERT_SWEEP_CRON = "0 0 2 * * *";

    /** Morning valuation digest. */
    public static final String INVENTORY_REPORT_ID = "ims-daily-inventory-report";
    public static final String INVENTORY_REPORT_CRON = "0 30 7 * * *";

    private JobSchedules() {
    }

    public static List<JobScheduleDto> all() {
        return List.of(
                new JobScheduleDto(
                        INVENTORY_ALERT_SWEEP_ID,
                        "Nightly inventory alert sweep",
                        INVENTORY_ALERT_SWEEP_CRON,
                        "Scans every product for low stock and every batch for expiry.",
                        JobMessageType.ALERT_SCAN.name()),
                new JobScheduleDto(
                        INVENTORY_REPORT_ID,
                        "Daily inventory report",
                        INVENTORY_REPORT_CRON,
                        "Computes total inventory value and the count of products at or below reorder level.",
                        JobMessageType.DAILY_INVENTORY_REPORT.name())
        );
    }
}
