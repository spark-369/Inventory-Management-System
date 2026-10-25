package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.JobMessageLogDto;
import com.unique.ims.backend.dto.JobScheduleDto;
import com.unique.ims.backend.dto.JobStatsDto;
import com.unique.ims.backend.messaging.JobMessageType;

import java.util.List;

/**
 * Read/inspect facade over the JobRunr message broker, plus manual message
 * dispatch for the frontend "Background Jobs" console.
 */
public interface BackgroundJobService {

    /** Live broker counters (queue depth, failures, worker availability). */
    JobStatsDto getStats();

    /** The application's declared recurring schedules. */
    List<JobScheduleDto> getSchedules();

    /** Recent message history, newest first. */
    List<JobMessageLogDto> getRecentMessages(int limit);

    /**
     * Publishes a message on demand using the given routing key.
     *
     * @return the correlation id of the enqueued message
     */
    String dispatch(JobMessageType type, Long productId);
}
