package snvn.splunkservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a Splunk HEC (HTTP Event Collector) event.
 * This class follows Splunk's HEC event format specification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SplunkEvent {

    /**
     * The event data - can be a string or a JSON object
     */
    private Object event;

    /**
     * Event timestamp in epoch time (seconds since 1970)
     */
    private Double time;

    /**
     * The host value to assign to the event data
     */
    private String host;

    /**
     * The source value to assign to the event data
     */
    private String source;

    /**
     * The sourcetype value to assign to the event data
     */
    @JsonProperty("sourcetype")
    private String sourceType;

    /**
     * The name of the index to store the event
     */
    private String index;

    /**
     * Specifies a JSON object containing explicit custom fields
     */
    private Map<String, Object> fields;

    // Default constructor
    public SplunkEvent() {
    }

    // Constructor with event data
    public SplunkEvent(Object event) {
        this.event = event;
        this.time = (double) System.currentTimeMillis() / 1000;
    }

    // Builder pattern
    public static SplunkEventBuilder builder() {
        return new SplunkEventBuilder();
    }

    // Getters and Setters
    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    public Double getTime() {
        return time;
    }

    public void setTime(Double time) {
        this.time = time;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    // Builder class
    public static class SplunkEventBuilder {
        private Object event;
        private Double time;
        private String host;
        private String source;
        private String sourceType;
        private String index;
        private Map<String, Object> fields;

        public SplunkEventBuilder event(Object event) {
            this.event = event;
            return this;
        }

        public SplunkEventBuilder time(Double time) {
            this.time = time;
            return this;
        }

        public SplunkEventBuilder host(String host) {
            this.host = host;
            return this;
        }

        public SplunkEventBuilder source(String source) {
            this.source = source;
            return this;
        }

        public SplunkEventBuilder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public SplunkEventBuilder index(String index) {
            this.index = index;
            return this;
        }

        public SplunkEventBuilder fields(Map<String, Object> fields) {
            this.fields = fields;
            return this;
        }

        public SplunkEvent build() {
            SplunkEvent splunkEvent = new SplunkEvent();
            splunkEvent.setEvent(this.event);
            splunkEvent.setTime(this.time != null ? this.time : (double) System.currentTimeMillis() / 1000);
            splunkEvent.setHost(this.host);
            splunkEvent.setSource(this.source);
            splunkEvent.setSourceType(this.sourceType);
            splunkEvent.setIndex(this.index);
            splunkEvent.setFields(this.fields);
            return splunkEvent;
        }
    }
}

