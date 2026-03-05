package snvn.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import jakarta.annotation.Nullable;
import org.springframework.web.client.RestClient;
import snvn.log.LogFileServiceImplementation;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Consumes JSON log events published by {@link KafkaExternalLogServiceImplementation}.
 * <p>
 * The topic is declared by {@link KafkaLogTopicConfig}.
 * This consumer simply listens on the topic configured in properties.
 * <p>
 * Each incoming message is a flat JSON map with fields such as:
 * timestamp, level, message, service, traceId, spanId, correlationId, etc.
 */
public class KafkaExternalLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaExternalLogConsumer.class);

    private final KafkaLogProperties properties;
    private final ObjectMapper objectMapper;
    @Nullable
    private final RestClient restClient;
    @Nullable
    private final LogFileServiceImplementation logFileService;

    public KafkaExternalLogConsumer(KafkaLogProperties properties,
                                    @Nullable RestClient restClient,
                                    @Nullable LogFileServiceImplementation logFileService) {
        this.properties = properties;
        this.restClient = restClient;
        this.logFileService = logFileService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "${kafka-service-log.topic}", groupId = "${kafka-service-log.group-id:kafka-log-group}", autoStartup = "${kafka-service-log.auto-startup:true}")
    public void consumeLogEvent(Map<String, Object> context) {
        if (!properties.isEnabled()) {
            return;
        }

        // Restore MDC from consumed message so log lines carry traceId
        setMdcFromContext(context, "traceId");
        setMdcFromContext(context, "spanId");
        setMdcFromContext(context, "correlationId");
        setMdcFromContext(context, "jobId");

        try {
            String level = context.get("level") != null ? context.get("level").toString() : "INFO";
            String message = context.get("message") != null ? context.get("message").toString() : "";
            context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

            // --- Splunk ---
            if (properties.isSplunkEnabled()) {
                Map<String, Object> event = new HashMap<>();
                event.put("level", context.get("level"));
                event.put("message", context.get("message"));
                event.put("timestamp", Instant.now().toString());
                event.putAll(context);

                Map<String, Object> payload = new HashMap<>();
                payload.put("event", event);
                payload.put("index", properties.getIndex());
                payload.put("source", properties.getSource());
                payload.put("sourcetype", properties.getSourcetype());
                payload.put("time", Instant.now().getEpochSecond());

                CompletableFuture.runAsync(() -> {
                    if (restClient == null) {
                        log.info("[Kafka-Splunk] RestClient not available. Consumed log event: {}", payload.get("event"));
                        return;
                    }
                    try {
                        restClient.post()
                                .body(payload)
                                .retrieve()
                                .toBodilessEntity();
                    } catch (Exception e) {
                        log.warn("Failed to send log to Splunk: {}", e.getMessage());
                    }
                });
            }

            // --- Log File ---
            if (properties.isLogfileEnabled()) {
                if (logFileService == null) {
                    log.warn("[Kafka-Log] LogFileServiceImplementation is not available. Skipping log file write.");
                } else {
                    CompletableFuture.runAsync(() -> {
                        try {
                            System.out.println("[Kafka-Log] LogFileServiceImplementation is available." + context);
                            logFileService.sendLogFile(level, message, context);
                        } catch (Exception e) {
                            log.warn("Failed to write log to file: {}", e.getMessage());
                        }
                    });
                }
            }
        } finally {
            MDC.clear();
        }
    }

    private void setMdcFromContext(Map<String, Object> context, String key) {
        Object value = context.get(key);
        if (value != null && !value.toString().isEmpty()) {
            MDC.put(key, value.toString());
        }
    }

    private void handleErrorEvent(Map<String, Object> event) {
        String exception = getString(event, "exception", null);
        if (exception != null) {
            log.error("[Kafka-Log] Exception detail: {}", exception);
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}

