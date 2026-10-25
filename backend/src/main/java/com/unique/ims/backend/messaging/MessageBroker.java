package com.unique.ims.backend.messaging;

import com.unique.ims.backend.dto.JobMessageLogDto;
import com.unique.ims.backend.dto.JobScheduleDto;
import com.unique.ims.backend.dto.JobStatsDto;
import com.unique.ims.backend.messaging.request.AbstractJobMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Broker facade used by producers to publish messages and by the frontend
 * console to inspect the queue.
 *
 * <p>Implemented on top of JobRunr: {@link #publish(AbstractJobMessage)} writes
 * an audit row and enqueues a persistent job, {@link #schedule(AbstractJobMessage, Instant)}
 * defers it, and the inspection methods read straight from the JobRunr
 * {@code StorageProvider}.</p>
 */
public interface MessageBroker {

    /**
     * Persists an audit row and enqueues the message for immediate processing.
     *
     * @return the correlation id recorded on the message and the audit row
     */
    String publish(AbstractJobMessage message);

    /**
     * Persists an audit row and schedules the message for a future instant.
     *
     * @return the correlation id recorded on the message and the audit row
     */
    String schedule(AbstractJobMessage message, Instant when);

    /**
     * Persists an audit row and schedules the message after a delay.
     *
     * @return the correlation id recorded on the message and the audit row
     */
    String schedule(AbstractJobMessage message, Duration delay);

    /** Live counters read from the JobRunr {@code StorageProvider}. */
    JobStatsDto getStats();

    /** The application's declared recurring schedules. */
    List<JobScheduleDto> getSchedules();

    /** Most recent messages, newest first. */
    List<JobMessageLogDto> getRecentMessages(int limit);

    /** Marks an audit row as picked up by a worker. */
    void markRunning(String correlationId);

    /** Marks an audit row as finished, recording a short result summary. */
    void markSucceeded(String correlationId, String result);

    /** Marks an audit row as failed, recording the error message. */
    void markFailed(String correlationId, String errorMessage);
}
