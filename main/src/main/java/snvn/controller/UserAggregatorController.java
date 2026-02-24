package snvn.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import snvn.exception.ReactiveErrorHandler;
import snvn.dto.CreateUserRequest;
import snvn.dto.CreateUserResponse;
import snvn.service.UserAggregatorService;

import java.util.Map;

/**
 * Main controller for user aggregation operations
 * This controller orchestrates calls to multiple microservices
 */
@RestController
@RequestMapping("/api/users")
public class UserAggregatorController {

    private static final Logger logger = LoggerFactory.getLogger(UserAggregatorController.class);

    @Autowired
    private UserAggregatorService userAggregatorService;

    /**
     * Create a new user - orchestrates multiple services
     *
     * Flow:
     * 1. Create user in user-service
     * 2. Create auth credentials in auth-service
     * 3. Create default account in account-service
     * 4. Publish event to Kafka
     * 5. Send welcome notification (async)
     * 6. Log to audit service (async)
     */
    @PostMapping
    public Mono<ResponseEntity<CreateUserResponse>> createUser(@RequestBody CreateUserRequest request) {
        logger.info("Received request to create user: {}", request.getUsername());

        // Capture MDC context before entering reactive stream
        final Map<String, String> mdcContext = ReactiveErrorHandler.captureMdcContext();

        return userAggregatorService.createUser(request)
            .doOnSubscribe(subscription -> {
                // Restore MDC context when subscription starts
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
            })
            .map(response -> {
                // Restore MDC context for logging
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("User creation completed successfully for: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .onErrorMap(ReactiveErrorHandler.wrapWithTraceContext("user-orchestration"));
            // Let GlobalExceptionHandler handle the wrapped exception
    }

    /**
     * Get user profile with aggregated data
     */
    @GetMapping("/{id}/profile")
    public Mono<ResponseEntity<Map<String, Object>>> getUserProfile(@PathVariable Long id) {
        logger.info("Fetching user profile for ID: {}", id);

        final Map<String, String> mdcContext = ReactiveErrorHandler.captureMdcContext();

        return userAggregatorService.getUserProfile(id)
            .doOnSubscribe(subscription -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .map(profile -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                return ResponseEntity.ok(profile);
            })
            .onErrorResume(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Error fetching user profile for ID: {}", id, error);
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "main-service",
            "message", "User aggregator service is running"
        ));
    }
}

