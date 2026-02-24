package snvn.splunkservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import snvn.splunkservice.model.ApiResponse;
import snvn.splunkservice.model.LogEventRequest;
import snvn.splunkservice.model.SplunkEvent;
import snvn.splunkservice.model.SplunkResponse;
import snvn.splunkservice.service.SplunkHecService;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Splunk HEC operations
 */
@RestController
@RequestMapping("/api/splunk")
public class SplunkController {

    private static final Logger logger = LoggerFactory.getLogger(SplunkController.class);

    private final SplunkHecService splunkHecService;

    public SplunkController(SplunkHecService splunkHecService) {
        this.splunkHecService = splunkHecService;
    }

    /**
     * Send a simple event to Splunk
     */
    @PostMapping("/event")
    public Mono<ResponseEntity<ApiResponse<SplunkResponse>>> sendEvent(@RequestBody Map<String, Object> eventData) {
        logger.info("Received request to send event to Splunk");

        return splunkHecService.sendEvent(eventData)
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(ApiResponse.success("Event sent successfully", response));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.<SplunkResponse>error("Failed to send event", response));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error sending event to Splunk: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ApiResponse.error("Error: " + error.getMessage())));
                });
    }

    /**
     * Send a log event to Splunk
     */
    @PostMapping("/log")
    public Mono<ResponseEntity<ApiResponse<SplunkResponse>>> sendLogEvent(@RequestBody LogEventRequest request) {
        logger.info("Received request to send log event to Splunk: {}", request.getMessage());

        return splunkHecService.sendLogEvent(request)
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(ApiResponse.success("Log event sent successfully", response));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.<SplunkResponse>error("Failed to send log event", response));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error sending log event to Splunk: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ApiResponse.error("Error: " + error.getMessage())));
                });
    }

    /**
     * Send a batch of events to Splunk
     */
    @PostMapping("/batch")
    public Mono<ResponseEntity<ApiResponse<SplunkResponse>>> sendBatch(@RequestBody List<Map<String, Object>> events) {
        logger.info("Received request to send batch of {} events to Splunk", events.size());

        List<SplunkEvent> splunkEvents = events.stream()
                .map(SplunkEvent::new)
                .toList();

        return splunkHecService.sendBatch(splunkEvents)
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(ApiResponse.success("Batch sent successfully", response));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.<SplunkResponse>error("Failed to send batch", response));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error sending batch to Splunk: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ApiResponse.error("Error: " + error.getMessage())));
                });
    }

    /**
     * Queue an event for batch processing
     */
    @PostMapping("/queue")
    public ResponseEntity<ApiResponse<Void>> queueEvent(@RequestBody Map<String, Object> eventData) {
        logger.info("Received request to queue event for Splunk");

        SplunkEvent event = new SplunkEvent(eventData);
        splunkHecService.queueEvent(event);

        return ResponseEntity.ok(ApiResponse.success("Event queued successfully"));
    }

    /**
     * Flush the event queue
     */
    @PostMapping("/flush")
    public ResponseEntity<ApiResponse<Void>> flushQueue() {
        logger.info("Received request to flush event queue");

        splunkHecService.flushQueue();

        return ResponseEntity.ok(ApiResponse.success("Queue flush initiated"));
    }

    /**
     * Send a metric to Splunk
     */
    @PostMapping("/metric")
    public Mono<ResponseEntity<ApiResponse<SplunkResponse>>> sendMetric(
            @RequestParam String name,
            @RequestParam Number value,
            @RequestBody(required = false) Map<String, String> dimensions) {
        logger.info("Received request to send metric to Splunk: {}={}", name, value);

        return splunkHecService.sendMetric(name, value, dimensions)
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(ApiResponse.success("Metric sent successfully", response));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.<SplunkResponse>error("Failed to send metric", response));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error sending metric to Splunk: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ApiResponse.error("Error: " + error.getMessage())));
                });
    }

    /**
     * Health check endpoint for Splunk HEC connectivity
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<ApiResponse<Boolean>>> healthCheck() {
        logger.info("Received request for Splunk HEC health check");

        return splunkHecService.healthCheck()
                .map(healthy -> {
                    if (healthy) {
                        return ResponseEntity.ok(ApiResponse.success("Splunk HEC is healthy", true));
                    } else {
                        return ResponseEntity.ok(ApiResponse.<Boolean>error("Splunk HEC is not healthy", false));
                    }
                });
    }
}

