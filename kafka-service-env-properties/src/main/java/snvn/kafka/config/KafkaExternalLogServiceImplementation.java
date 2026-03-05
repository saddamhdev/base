package snvn.kafka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import snvn.common.logging.AbstractExternalLogService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KafkaExternalLogServiceImplementation extends AbstractExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(KafkaExternalLogServiceImplementation.class);

    private final KafkaLogProperties properties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaExternalLogServiceImplementation(KafkaLogProperties properties,
                                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void sendLogKafka(String level, String message, Map<String, Object> context) {
        System.out.println("sendLogKafka" + context);
        if (!properties.isEnabled()) {
            return;
        }

        Map<String, Object> event = buildEvent(level, message, context);
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            try {
                kafkaTemplate.send(
                        properties.getTopic(),
                        event
                );
                log.debug("Log event sent to Kafka successfully");
            } catch (Exception e) {
                log.warn("Failed to send log event to Kafka: {}", e.getMessage());
            } finally {
                MDC.clear();
            }
        });
    }

    @Override
    public void sendErrorLogKafka(String message, String throwable, Map<String, Object> context) {
        Map<String, Object> errorContext = new LinkedHashMap<>();
        if (context != null) {
            errorContext.putAll(context);
        }
        errorContext.put("exception", throwable);
        sendLogKafka("ERROR", message, errorContext);
    }

    private Map<String, Object> buildEvent(String level, String message, Map<String, Object> context) {
        Map<String, Object> event = new LinkedHashMap<>();

        // Core fields
        event.put("timestamp", Instant.now().toString());
        event.put("level", level);
        event.put("message", message);
        event.put("service", properties.getSource());

        // MDC trace context
        addIfPresent(event, "traceId", MDC.get("traceId"));
        addIfPresent(event, "spanId", MDC.get("spanId"));
        addIfPresent(event, "correlationId", MDC.get("correlationId"));
        addIfPresent(event, "jobId", MDC.get("jobId"));

        // Caller-provided context fields
        if (context != null) {
            context.forEach((key, value) -> {
                if (value != null) {
                    event.putIfAbsent(key, value);
                }
            });
        }

        return event;
    }

    private void addIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }
}

