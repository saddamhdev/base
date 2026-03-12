package snvn.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import snvn.common.logging.ExternalLogService;
import snvn.common.logging.NoOpExternalLogService;
import snvn.gatewayservice.config.GatewayServiceLogProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified ApplicationDetails GlobalFilter.
 * Generates traceId/spanId/correlationId ONCE per request,
 * then sends logs only to channels enabled via GatewayServiceLogProperties.
 */
@Component
public class ApplicationDetailsGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationDetailsGlobalFilter.class);

    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final ExternalLogService externalLogService;
    private final GatewayServiceLogProperties logProperties;

    @Autowired
    public ApplicationDetailsGlobalFilter(ObjectMapper objectMapper,
                                          Tracer tracer,
                                          GatewayServiceLogProperties logProperties,
                                          @Autowired(required = false) ExternalLogService externalLogService) {
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.logProperties = logProperties;
        this.externalLogService = externalLogService != null ? externalLogService : new NoOpExternalLogService();
        log.info("ApplicationDetailsGlobalFilter initialized with ExternalLogService: {}, channels: {}",
                this.externalLogService.getClass().getSimpleName(), logProperties);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {


        long startTime = System.currentTimeMillis();

        // =========================
        // 1️⃣ Correlation ID
        // =========================
        String incomingCorrelationId =
                exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

        final String correlationId =
                (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                        ? UUID.randomUUID().toString()
                        : incomingCorrelationId;

        // =========================
        // 2️⃣ Trace ID (Micrometer) - Create new span if none exists
        // =========================
        String traceId = null;
        String spanId = null;

        // First, try to get from incoming headers
        traceId = exchange.getRequest().getHeaders().getFirst("X-B3-TraceId");
        if (traceId == null || traceId.isBlank()) {
            String traceparent = exchange.getRequest().getHeaders().getFirst("traceparent");
            if (traceparent != null && !traceparent.isBlank()) {
                String[] parts = traceparent.split("-");
                if (parts.length >= 2) {
                    traceId = parts[1];
                }
            }
        }

        // If no trace ID from headers, try current span or create a new one
        if (traceId == null || traceId.isBlank()) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                traceId = currentSpan.context().traceId();
                spanId = currentSpan.context().spanId();
            }
        }

        // If still no trace ID, generate a new one
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // Generate spanId if not set
        if (spanId == null || spanId.isBlank()) {
            spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        final String resolvedTraceId = traceId;
        final String resolvedSpanId = spanId;

        // Set MDC for logging
        MDC.put("traceId", resolvedTraceId);
        MDC.put("spanId", resolvedSpanId);
        MDC.put("correlationId", correlationId);

        // =========================
        // 3️⃣ API Key Validation
        // =========================
        String apiKey = exchange.getRequest()
                .getHeaders()
                .getFirst("X-API-KEY");

        if (apiKey == null || !apiKey.equals("my-secret-key")) {
            return buildErrorResponse(exchange, correlationId, resolvedTraceId,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or missing API Key");
        }

        // =========================
        // 4️⃣ Inject Headers
        // =========================
        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header("X-Correlation-Id", correlationId)
                        .header("X-Trace-Id", resolvedTraceId)
                        .header("X-Span-Id", resolvedSpanId)
                        .header("X-B3-TraceId", resolvedTraceId)
                        .header("X-B3-SpanId", resolvedSpanId)
                        .header("X-App-Name", "gateway-service")
                        .header("X-App-Version", "v1")
                        .build();

        ServerWebExchange mutatedExchange =
                exchange.mutate().request(mutatedRequest).build();

        exchange.getResponse().getHeaders()
                .add("X-Correlation-Id", correlationId);
        exchange.getResponse().getHeaders()
                .add("X-Trace-Id", resolvedTraceId);
        exchange.getResponse().getHeaders()
                .add("X-Span-Id", resolvedSpanId);

//        log.info("Incoming request method={} path={} correlationId={} traceId={} spanId={}",
//                mutatedRequest.getMethod(),
//                mutatedRequest.getURI().getPath(),
//                correlationId,
//                resolvedTraceId,
//                resolvedSpanId);

        // Send incoming request log to ALL enabled channels
        Map<String, Object> incomingContext = new HashMap<>();
        incomingContext.put("traceId", resolvedTraceId);
        incomingContext.put("spanId", resolvedSpanId);
        incomingContext.put("correlationId", correlationId);
        incomingContext.put("method", mutatedRequest.getMethod().name());
        incomingContext.put("path", mutatedRequest.getURI().getPath());
        incomingContext.put("logger", log.getName() + ".filter()");
        incomingContext.put("service", "gateway-service");
        incomingContext.put("eventType", "INCOMING_REQUEST");
        sendToAllChannels("INFO", "Incoming request", incomingContext);

        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ctx
                        .put("traceId", resolvedTraceId)
                        .put("spanId", resolvedSpanId)
                        .put("correlationId", correlationId))
                .doOnEach(signal -> {
                    if (!signal.isOnComplete() && !signal.isOnError()) {
                        MDC.put("traceId", resolvedTraceId);
                        MDC.put("spanId", resolvedSpanId);
                        MDC.put("correlationId", correlationId);
                    }
                })
                .doFinally(signalType -> {
                    MDC.put("traceId", resolvedTraceId);
                    MDC.put("spanId", resolvedSpanId);
                    MDC.put("correlationId", correlationId);

                    long duration = System.currentTimeMillis() - startTime;

//                    log.info("Completed request path={} status={} durationMs={} correlationId={} traceId={} spanId={}",
//                            mutatedRequest.getURI().getPath(),
//                            exchange.getResponse().getStatusCode(),
//                            duration,
//                            correlationId,
//                            resolvedTraceId,
//                            resolvedSpanId);

                    // Send completed request log to ALL enabled channels
                    Map<String, Object> completedContext = new HashMap<>();
                    completedContext.put("traceId", resolvedTraceId);
                    completedContext.put("spanId", resolvedSpanId);
                    completedContext.put("correlationId", correlationId);
                    completedContext.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

                    completedContext.put("method", mutatedRequest.getMethod().name());
                    completedContext.put("path", mutatedRequest.getURI().getPath());
                    completedContext.put("status", exchange.getResponse().getStatusCode() != null ?
                            exchange.getResponse().getStatusCode().value() : 0);
                    completedContext.put("durationMs", duration);
                    completedContext.put("service", "gateway-service");
                    completedContext.put("eventType", "COMPLETED_REQUEST");
                    sendToAllChannels("INFO", "Completed request", completedContext);

                    MDC.clear();
                });
    }

    /**
     * Sends log only to channels enabled in gateway-service-log properties.
     */
    private void sendToAllChannels(String level, String message, Map<String, Object> context) {
        if (logProperties.isLogfileEnabled()) {
            externalLogService.sendLogFile(level, message, context);
        }
        if (logProperties.isSplunkEnabled()) {
            externalLogService.sendLogSplunk(level, message, context);
        }
        if (logProperties.isRabbitmqEnabled()) {
            externalLogService.sendLogRabbitMQ(level, message, context);
        }
        if (logProperties.isKafkaEnabled()) {
            externalLogService.sendLogKafka(level, message, context);
        }
    }

    /**
     * Sends error log only to channels enabled in gateway-service-log properties.
     */
    private void sendErrorToAllChannels(String message, String throwable, Map<String, Object> context) {
        if (logProperties.isLogfileEnabled()) {
            externalLogService.sendErrorLogFile(message, throwable, context);
        }
        if (logProperties.isSplunkEnabled()) {
            externalLogService.sendErrorLogSplunk(message, throwable, context);
        }
        if (logProperties.isRabbitmqEnabled()) {
            externalLogService.sendErrorLogRabbitMQ(message, throwable, context);
        }
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange,
                                          String correlationId,
                                          String traceId,
                                          HttpStatus status,
                                          String message) {

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", message);
            body.put("path", exchange.getRequest().getURI().getPath());
            body.put("method", exchange.getRequest().getMethod().name());
            body.put("logger", log.getName() + ".buildErrorResponse()");
            body.put("correlationId", correlationId);
            body.put("traceId", traceId);
            body.put("timestamp", Instant.now());

            byte[] bytes = objectMapper.writeValueAsBytes(body);

            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders()
                    .add("X-Correlation-Id", correlationId);
            exchange.getResponse().getHeaders()
                    .add("X-Trace-Id", traceId);

//            log.error("Blocked request path={} reason={} correlationId={} traceId={}",
//                    exchange.getRequest().getURI().getPath(),
//                    message,
//                    correlationId,
//                    traceId);

            // Send blocked request log to ALL enabled channels
            Map<String, Object> blockedContext = new HashMap<>();
            blockedContext.put("traceId", traceId);
            blockedContext.put("correlationId", correlationId);
            blockedContext.put("logger", log.getName() + ".buildErrorResponse()");
            blockedContext.put("method", exchange.getRequest().getMethod().name());
            blockedContext.put("path", exchange.getRequest().getURI().getPath());
            blockedContext.put("status", status.value());
            blockedContext.put("reason", message);
            blockedContext.put("service", "gateway-service");
            blockedContext.put("eventType", "BLOCKED_REQUEST");
            sendErrorToAllChannels("ERROR", "Blocked request", blockedContext);

            return exchange.getResponse()
                    .writeWith(Mono.just(
                            exchange.getResponse()
                                    .bufferFactory()
                                    .wrap(bytes)));

        } catch (Exception e) {
            log.error("Failed to build error response", e);
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return -100; // 🔥 runs before everything
    }
}

