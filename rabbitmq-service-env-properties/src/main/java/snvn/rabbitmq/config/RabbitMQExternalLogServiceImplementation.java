package snvn.rabbitmq.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import snvn.common.logging.AbstractExternalLogService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RabbitMQExternalLogServiceImplementation extends AbstractExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQExternalLogServiceImplementation.class);

    private final RabbitMQLogProperties properties;
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQExternalLogServiceImplementation(RabbitMQLogProperties properties, RabbitTemplate rabbitTemplate) {
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendLogRabbitMQ(String level, String message, Map<String, Object> context) {
        System.out.println("sendLogRabbitMQ"+context);
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
                rabbitTemplate.convertAndSend(
                        properties.getExchange(),
                        "",
                        event
                );
                log.debug("Log event sent to RabbitMQ successfully ");
            } catch (Exception e) {
                log.warn("Failed to send log event to RabbitMQ: {}", e.getMessage());
            } finally {
                MDC.clear();
            }
        });
    }

    @Override
    public void sendErrorLogRabbitMQ(String message, String throwable, Map<String, Object> context) {
        Map<String, Object> errorContext = new LinkedHashMap<>();
        if (context != null) {
            errorContext.putAll(context);
        }
        errorContext.put("exception", throwable);
        sendLogRabbitMQ("ERROR", message, errorContext);
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
