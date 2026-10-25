package com.unique.ims.backend.messaging;

import com.unique.ims.backend.dto.JobMessageLogDto;
import com.unique.ims.backend.dto.JobScheduleDto;
import com.unique.ims.backend.dto.JobStatsDto;
import com.unique.ims.backend.messaging.request.AbstractJobMessage;
import com.unique.ims.backend.model.JobMessageLog;
import com.unique.ims.backend.repository.JobMessageLogRepository;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * JobRunr-backed implementation of the {@link MessageBroker}.
 *
 * <p>JobRunr acts as the message broker: it persists every published message in
 * the shared PostgreSQL database and a pool of background workers delivers it to
 * the bound {@code JobRequestHandler}. That gives at-least-once delivery,
 * automatic retries with exponential back-off and full visibility — all without
 * provisioning RabbitMQ, Kafka or Redis.</p>
 *
 * <p>Each publish also writes a {@link JobMessageLog} row so the application owns
 * a readable audit trail that the frontend console can render.</p>
 */
@Service
public class JobRunrMessageBroker implements MessageBroker {

    private static final Logger log = LoggerFactory.getLogger(JobRunrMessageBroker.class);

    private final JobRequestScheduler jobRequestScheduler;
    private final StorageProvider storageProvider;
    private final JobMessageLogRepository jobMessageLogRepository;
    private final String dashboardUrl;

    public JobRunrMessageBroker(JobRequestScheduler jobRequestScheduler,
                               StorageProvider storageProvider,
                               JobMessageLogRepository jobMessageLogRepository,
                               @Value("${app.messaging.dashboard-url:http://localhost:8000/dashboard}") String dashboardUrl) {
        this.jobRequestScheduler = jobRequestScheduler;
        this.storageProvider = storageProvider;
        this.jobMessageLogRepository = jobMessageLogRepository;
        this.dashboardUrl = dashboardUrl;
    }

    @Override
    @Transactional
    public String publish(AbstractJobMessage message) {
        JobMessageLog entry = prepare(message);
        JobId jobId = jobRequestScheduler.enqueue(message);
        recordJobId(entry, jobId);
        log.info("Published {} message [{}] — {}", message.getMessageType(), entry.getCorrelationId(), message.getReason());
        return entry.getCorrelationId();
    }

    @Override
    @Transactional
    public String schedule(AbstractJobMessage message, Instant when) {
        JobMessageLog entry = prepare(message);
        JobId jobId = jobRequestScheduler.schedule(when, message);
        recordJobId(entry, jobId);
        log.info("Scheduled {} message [{}] for {}", message.getMessageType(), entry.getCorrelationId(), when);
        return entry.getCorrelationId();
    }

    @Override
    @Transactional
    public String schedule(AbstractJobMessage message, Duration delay) {
        return schedule(message, Instant.now().plus(delay));
    }

    /**
     * Assigns a correlation id and stamps the audit row. Runs in the caller's
     * transaction so the audit row and the business change commit together.
     */
    private JobMessageLog prepare(AbstractJobMessage message) {
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        return jobMessageLogRepository.save(JobMessageLog.builder()
                .correlationId(correlationId)
                .messageType(message.getMessageType() == null ? "UNKNOWN" : message.getMessageType().name())
                .reason(message.getReason())
                .requestedBy(message.getRequestedBy())
                .status(JobMessageLog.JobMessageStatus.ENQUEUED)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** Stores the JobRunr job id so the dashboard entry can be correlated. */
    private void recordJobId(JobMessageLog entry, JobId jobId) {
        if (jobId == null) {
            return;
        }
        entry.setJobId(String.valueOf(jobId));
        jobMessageLogRepository.save(entry);
    }

    @Override
    public JobStatsDto getStats() {
        JobStats stats = storageProvider.getJobStats();
        JobStatsDto dto = new JobStatsDto();
        dto.setTimeStamp(stats.getTimeStamp());
        dto.setQueryDurationInMillis(stats.getQueryDurationInMillis());
        dto.setTotal(orZero(stats.getTotal()));
        dto.setAwaiting(orZero(stats.getAwaiting()));
        dto.setScheduled(orZero(stats.getScheduled()));
        dto.setEnqueued(orZero(stats.getEnqueued()));
        dto.setProcessing(orZero(stats.getProcessing()));
        dto.setFailed(orZero(stats.getFailed()));
        dto.setSucceeded(orZero(stats.getSucceeded()));
        dto.setAllTimeSucceeded(orZero(stats.getAllTimeSucceeded()));
        dto.setDeleted(orZero(stats.getDeleted()));
        dto.setRecurringJobs(stats.getRecurringJobs());
        dto.setBackgroundJobServers(stats.getBackgroundJobServers());
        dto.setPending(dto.getAwaiting() + dto.getScheduled() + dto.getEnqueued() + dto.getProcessing());
        dto.setDashboardUrl(dashboardUrl);
        return dto;
    }

    @Override
    public List<JobScheduleDto> getSchedules() {
        return JobSchedules.all();
    }

    @Override
    public List<JobMessageLogDto> getRecentMessages(int limit) {
        int capped = limit <= 0 ? 25 : Math.min(limit, 200);
        return jobMessageLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .limit(capped)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRunning(String correlationId) {
        update(correlationId, entry -> entry.setStatus(JobMessageLog.JobMessageStatus.RUNNING));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(String correlationId, String result) {
        update(correlationId, entry -> {
            entry.setStatus(JobMessageLog.JobMessageStatus.SUCCEEDED);
            entry.setResult(result);
            entry.setErrorMessage(null);
            entry.setProcessedAt(LocalDateTime.now());
            entry.setDurationMillis(durationMillis(entry));
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String correlationId, String errorMessage) {
        update(correlationId, entry -> {
            entry.setStatus(JobMessageLog.JobMessageStatus.FAILED);
            entry.setErrorMessage(errorMessage);
            entry.setProcessedAt(LocalDateTime.now());
            entry.setDurationMillis(durationMillis(entry));
        });
    }

    private void update(String correlationId, Consumer<JobMessageLog> mutation) {
        if (correlationId == null || correlationId.isBlank()) {
            return;
        }
        jobMessageLogRepository.findByCorrelationId(correlationId).ifPresent(entry -> {
            mutation.accept(entry);
            jobMessageLogRepository.save(entry);
        });
    }

    private Long durationMillis(JobMessageLog entry) {
        if (entry.getCreatedAt() == null || entry.getProcessedAt() == null) {
            return null;
        }
        return Duration.between(entry.getCreatedAt(), entry.getProcessedAt()).toMillis();
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private JobMessageLogDto mapToDto(JobMessageLog entry) {
        JobMessageLogDto dto = new JobMessageLogDto();
        dto.setId(entry.getId());
        dto.setCorrelationId(entry.getCorrelationId());
        dto.setMessageType(entry.getMessageType());
        dto.setJobId(entry.getJobId());
        dto.setReason(entry.getReason());
        dto.setRequestedBy(entry.getRequestedBy());
        dto.setStatus(entry.getStatus() == null ? null : entry.getStatus().name());
        dto.setResult(entry.getResult());
        dto.setErrorMessage(entry.getErrorMessage());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setProcessedAt(entry.getProcessedAt());
        dto.setDurationMillis(entry.getDurationMillis());
        return dto;
    }
}
