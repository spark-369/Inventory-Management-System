package com.unique.ims.backend.controller;

import com.unique.ims.backend.dto.JobMessageLogDto;
import com.unique.ims.backend.dto.JobScheduleDto;
import com.unique.ims.backend.dto.JobStatsDto;
import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.service.BackgroundJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Observability and manual-dispatch API for the JobRunr message broker.
 *
 * <p>Backs the frontend "Background Jobs" console: it exposes live queue
 * statistics, the declared recurring schedules and the message audit trail, and
 * lets an operator re-publish a message on demand.</p>
 */
@RestController
@RequestMapping("/api/jobs")
public class BackgroundJobController {

    private final BackgroundJobService backgroundJobService;

    public BackgroundJobController(BackgroundJobService backgroundJobService) {
        this.backgroundJobService = backgroundJobService;
    }

    @GetMapping("/stats")
    public ResponseEntity<JobStatsDto> getStats() {
        return ResponseEntity.ok(backgroundJobService.getStats());
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<JobScheduleDto>> getSchedules() {
        return ResponseEntity.ok(backgroundJobService.getSchedules());
    }

    @GetMapping("/messages")
    public ResponseEntity<List<JobMessageLogDto>> getMessages(
            @RequestParam(name = "limit", defaultValue = "25") int limit) {
        return ResponseEntity.ok(backgroundJobService.getRecentMessages(limit));
    }

    /**
     * Message types known to the application, with display metadata.
     *
     * <p>{@code dispatchable} is false for messages that only a domain workflow may
     * produce (for example {@code ORDER_CONFIRMATION}), so the console can render
     * them as read-only entries instead of offering a button that would fail.</p>
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getMessageTypes() {
        List<Map<String, Object>> types = java.util.Arrays.stream(JobMessageType.values())
                .map(type -> Map.<String, Object>of(
                        "value", type.name(),
                        "label", type.getLabel(),
                        "description", type.getDescription(),
                        "dispatchable", type.isDispatchable()))
                .toList();
        return ResponseEntity.ok(types);
    }

    @PostMapping("/dispatch")
    public ResponseEntity<Map<String, String>> dispatch(@RequestBody DispatchRequest request) {
        JobMessageType type;
        try {
            type = JobMessageType.valueOf(request.type());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unsupported message type: " + request.type());
        }
        String correlationId = backgroundJobService.dispatch(type, request.productId());
        return ResponseEntity.ok(Map.of(
                "correlationId", correlationId == null ? "" : correlationId,
                "messageType", type.name(),
                "status", "ENQUEUED"));
    }

    /** Request body for {@link #dispatch(DispatchRequest)}. */
    public record DispatchRequest(String type, Long productId) {
    }
}
