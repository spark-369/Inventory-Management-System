package com.unique.ims.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail of every message published to the JobRunr broker.
 *
 * <p>The domain services persist a row here in the same transaction as the
 * business change, then hand the generated {@code correlationId} to the message
 * payload. Consumers look the row up again and flip it to {@code SUCCEEDED} or
 * {@code FAILED}, which gives the frontend console a readable message history
 * without having to parse the JobRunr tables.</p>
 */
@Entity
@Table(name = "job_message_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correlation id shared with the message payload. */
    @Column(nullable = false, unique = true)
    private String correlationId;

    /** Routing key ({@code JobMessageType} name). */
    @Column(nullable = false)
    private String messageType;

    /** JobRunr job id once the message has been enqueued. */
    private String jobId;

    /** Human-readable reason the message was produced. */
    private String reason;

    /** Informational actor label — this application has no authentication. */
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobMessageStatus status;

    /** Short result summary written by the consumer. */
    @Column(length = 1000)
    private String result;

    @Column(length = 2000)
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    /** Duration between publishing and the consumer finishing, in milliseconds. */
    private Long durationMillis;

    public enum JobMessageStatus {
        /** Persisted and enqueued; not yet picked up by a worker. */
        ENQUEUED,
        /** A worker is currently executing the consumer. */
        RUNNING,
        /** Consumer finished successfully. */
        SUCCEEDED,
        /** Consumer threw an exception (JobRunr may still retry it). */
        FAILED
    }
}
