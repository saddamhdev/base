package snvn.splunkservice.model;

import java.time.Instant;
import java.util.Map;

/**
 * Request DTO for sending log events to Splunk
 */
public class LogEventRequest {

    private String message;
    private String level;
    private String source;
    private String sourceType;
    private String host;
    private Map<String, Object> additionalFields;
    private Long timestamp;

    // Default constructor
    public LogEventRequest() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

