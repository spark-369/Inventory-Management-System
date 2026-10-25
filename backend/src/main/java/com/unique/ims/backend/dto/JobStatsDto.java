package com.unique.ims.backend.dto;

import java.time.Instant;

/**
 * Snapshot of the JobRunr message broker state.
 *
 * <p>Surfaced to the frontend "Background Jobs" console so operators can watch
 * queue depth, failures and worker availability without opening the JobRunr
 * dashboard.</p>
 */
public class JobStatsDto {
    private Instant timeStamp;
    private long total;
    private long awaiting;
    private long scheduled;
    private long enqueued;
    private long processing;
    private long failed;
    private long succeeded;
    private long allTimeSucceeded;
    private long deleted;
    private int recurringJobs;
    private int backgroundJobServers;

    /** Awaiting + scheduled + enqueued + processing — i.e. work still in flight. */
    private long pending;

    private Long queryDurationInMillis;

    /** Deep link to the embedded JobRunr dashboard, if it is enabled. */
    private String dashboardUrl;

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Instant timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getAwaiting() {
        return awaiting;
    }

    public void setAwaiting(long awaiting) {
        this.awaiting = awaiting;
    }

    public long getScheduled() {
        return scheduled;
    }

    public void setScheduled(long scheduled) {
        this.scheduled = scheduled;
    }

    public long getEnqueued() {
        return enqueued;
    }

    public void setEnqueued(long enqueued) {
        this.enqueued = enqueued;
    }

    public long getProcessing() {
        return processing;
    }

    public void setProcessing(long processing) {
        this.processing = processing;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(long succeeded) {
        this.succeeded = succeeded;
    }

    public long getAllTimeSucceeded() {
        return allTimeSucceeded;
    }

    public void setAllTimeSucceeded(long allTimeSucceeded) {
        this.allTimeSucceeded = allTimeSucceeded;
    }

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public int getRecurringJobs() {
        return recurringJobs;
    }

    public void setRecurringJobs(int recurringJobs) {
        this.recurringJobs = recurringJobs;
    }

    public int getBackgroundJobServers() {
        return backgroundJobServers;
    }

    public void setBackgroundJobServers(int backgroundJobServers) {
        this.backgroundJobServers = backgroundJobServers;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public Long getQueryDurationInMillis() {
        return queryDurationInMillis;
    }

    public void setQueryDurationInMillis(Long queryDurationInMillis) {
        this.queryDurationInMillis = queryDurationInMillis;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;
    }
}
