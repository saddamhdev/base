package snvn.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import snvn.common.logging.AbstractExternalLogService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class LogFileServiceImplementation extends AbstractExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(LogFileServiceImplementation.class);

    private final LogProperties properties;
    private final ObjectMapper objectMapper;
    private final PrintWriter writer;

    public LogFileServiceImplementation(LogProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.writer = initWriter(properties.getSplunkFile());
    }

    private PrintWriter initWriter(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            return new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)), true);
        } catch (IOException e) {
            log.error("Failed to open Splunk log file '{}': {}", filePath, e.getMessage());
            return null;
        }
    }

    @Override
    public void sendLogFile(String level, String message, Map<String, Object> context) {
        System.out.println("Check log");

        if (!properties.isEnabled() || writer == null) {
            return;
        }
        //System.out.println("Check log");
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

        Map<String, Object> event = buildFlatEvent(level, message, context);
        try {
            String json = objectMapper.writeValueAsString(event);
            System.out.println("Check log"+json);

            synchronized (writer) {
                writer.println(json);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize log event to JSON: {}", e.getMessage());
        }
    }

    @Override
    public void sendErrorLogFile(String message, String throwable, Map<String, Object> context) {
        Map<String, Object> errorContext = new LinkedHashMap<>();
        if (context != null) {
            errorContext.putAll(context);
        }
        errorContext.put("exception", throwable);
        sendLogFile("ERROR", message, errorContext);
    }


    /**
     * Builds a flat JSON event for Splunk field extraction.
     * Each top-level key becomes a searchable field in Splunk.
     *
     * Output (one line per event):
     * {"timestamp":"2026-03-02T12:00:00Z","level":"INFO","message":"Completed request",
     *  "service":"user-service","traceId":"abc123","correlationId":"corr-001",
     *  "method":"POST","path":"/api/users","status":201,"durationMs":1623,
     *  "eventType":"COMPLETED_REQUEST","logger":"snvn.userservice.filter..."}
     */
    private Map<String, Object> buildFlatEvent(String level, String message, Map<String, Object> context) {
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