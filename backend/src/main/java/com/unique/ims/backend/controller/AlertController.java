package com.unique.ims.backend.controller;

import com.unique.ims.backend.dto.AlertDto;
import com.unique.ims.backend.messaging.JobMessageType;
import com.unique.ims.backend.service.AlertService;
import com.unique.ims.backend.service.BackgroundJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final BackgroundJobService backgroundJobService;

    public AlertController(AlertService alertService, BackgroundJobService backgroundJobService) {
        this.alertService = alertService;
        this.backgroundJobService = backgroundJobService;
    }

    @GetMapping
    public ResponseEntity<List<AlertDto>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/unresolved")
    public ResponseEntity<List<AlertDto>> getUnresolvedAlerts() {
        return ResponseEntity.ok(alertService.getUnresolvedAlerts());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<AlertDto>> getAlertsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(alertService.getAlertsByProduct(productId));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }

    @PostMapping
    public ResponseEntity<AlertDto> createAlert(@RequestBody AlertDto alertDto) {
        return ResponseEntity.ok(alertService.createAlert(alertDto));
    }

    /**
     * Requests an alert scan.
     *
     * <p>Returns {@code 202 Accepted} immediately and publishes an
     * {@code ALERT_SCAN} message: the sweep itself runs on a background worker, so
     * a large catalogue no longer blocks the HTTP request.</p>
     */
    @PostMapping("/generate")
    public ResponseEntity<Void> generateAlerts() {
        backgroundJobService.dispatch(JobMessageType.ALERT_SCAN, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}

