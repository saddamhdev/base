package snvn.splunkservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents Splunk HEC response
 */
public class SplunkResponse {

    @JsonProperty("text")
    private String text;

    @JsonProperty("code")
    private int code;

    @JsonProperty("invalid-event-number")
    private Integer invalidEventNumber;

    @JsonProperty("ackId")
    private Long ackId;

    // Default constructor
    public SplunkResponse() {
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Integer getInvalidEventNumber() {
        return invalidEventNumber;
    }

    public void setInvalidEventNumber(Integer invalidEventNumber) {
        this.invalidEventNumber = invalidEventNumber;
    }

    public Long getAckId() {
        return ackId;
    }

    public void setAckId(Long ackId) {
        this.ackId = ackId;
    }

    public boolean isSuccess() {
        return code == 0 || "Success".equalsIgnoreCase(text);
    }

    @Override
    public String toString() {
        return "SplunkResponse{" +
                "text='" + text + '\'' +
                ", code=" + code +
                ", invalidEventNumber=" + invalidEventNumber +
                ", ackId=" + ackId +
                '}';
    }
}

