package snvn.kafkaservice.model;

public class EventMessage {

    private String eventType;
    private String payload;
    private String source;
    private Long timestamp;

    public EventMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public EventMessage(String eventType, String payload, String source) {
        this();
        this.eventType = eventType;
        this.payload = payload;
        this.source = source;
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EventMessage{" +
                "eventType='" + eventType + '\'' +
                ", payload='" + payload + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

