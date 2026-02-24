package snvn.rabbitmqservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rabbitmq_events")
public class RabbitMQEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, length = 2000)
    private String payload;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private String exchange;

    private String routingKey;

    private String queue;

    public RabbitMQEvent() {
        this.timestamp = LocalDateTime.now();
        this.status = EventStatus.PENDING;
    }

    public RabbitMQEvent(String eventType, String payload, String source) {
        this();
        this.eventType = eventType;
        this.payload = payload;
        this.source = source;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public enum EventStatus {
        PENDING, SENT, FAILED, CONSUMED
    }
}

