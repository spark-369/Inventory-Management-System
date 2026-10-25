package com.unique.ims.backend.messaging.request;

import com.unique.ims.backend.messaging.JobMessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * Common envelope for every message published to the JobRunr broker.
 *
 * <p>Carries the metadata needed for an outbox-style audit trail:</p>
 * <ul>
 *   <li>{@code correlationId} — identifier of the {@code JobMessageLog} row the
 *       publisher created. Consumers use it to flip that row from
 *       {@code RUNNING} to {@code SUCCEEDED}/{@code FAILED}.</li>
 *   <li>{@code messageType} — the routing key, so a consumer can be dispatched
 *       and the dashboard shows a meaningful job name.</li>
 *   <li>{@code reason}/{@code requestedBy} — why the message was produced and by
 *       whom (there is no authentication in this application, so this is
 *       informational only).</li>
 * </ul>
 *
 * <p>Subclasses are serialised to JSON by JobRunr and stored in the database
 * until a worker picks them up, so they must expose a no-argument constructor
 * and keep every field JSON-friendly (no JPA entities, no collections of
 * proxies). They also bind themselves to a consumer by implementing
 * {@link JobRequest#getJobRequestHandler()}.</p>
 *
 * <p>Note: JobRunr's JSON mapper serialises <em>fields</em> only (getter
 * visibility is disabled), so the inherited {@code getJobRequestHandler()}
 * method is not treated as a serialisable property.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractJobMessage implements JobRequest {

    private String correlationId;
    private JobMessageType messageType;
    private String reason;
    private String requestedBy;

    protected AbstractJobMessage(JobMessageType messageType, String reason, String requestedBy) {
        this.messageType = messageType;
        this.reason = reason;
        this.requestedBy = requestedBy;
    }
}
