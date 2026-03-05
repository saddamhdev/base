package snvn.rabbitmqservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import snvn.rabbitmqservice.model.RabbitMQEvent;
import snvn.rabbitmqservice.service.RabbitMQEventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rabbitmq/events")
public class RabbitMQEventController {

    @Autowired
    private RabbitMQEventService rabbitMQEventService;

    /**
     * Publish event to RabbitMQ
     */
    @PostMapping
    public ResponseEntity<RabbitMQEvent> publishEvent(@RequestBody Map<String, String> request) {
        String eventType = request.get("eventType");
        String payload = request.get("payload");
        String source = request.getOrDefault("source", "rabbitmq-service");

        RabbitMQEvent event = rabbitMQEventService.publishEvent(eventType, payload, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * Publish event with custom routing key
     */
    @PostMapping("/custom-routing")
    public ResponseEntity<RabbitMQEvent> publishEventWithCustomRouting(@RequestBody Map<String, String> request) {
        String eventType = request.get("eventType");
        String payload = request.get("payload");
        String source = request.getOrDefault("source", "rabbitmq-service");
        String routingKey = request.get("routingKey");

        RabbitMQEvent event = rabbitMQEventService.publishEventWithCustomRouting(eventType, payload, source, routingKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * Broadcast event to all consumers via fanout exchange.
     * The message is delivered to every queue bound to the fanout exchange
     * (notification, audit, analytics).
     */
    @PostMapping("/fanout")
    public ResponseEntity<RabbitMQEvent> publishFanoutEvent(@RequestBody Map<String, String> request) {
        String eventType = request.get("eventType");
        String payload = request.get("payload");
        String source = request.getOrDefault("source", "rabbitmq-service");

        RabbitMQEvent event = rabbitMQEventService.publishFanoutEvent(eventType, payload, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * Get all events
     */
    @GetMapping
    public ResponseEntity<List<RabbitMQEvent>> getAllEvents() {
        List<RabbitMQEvent> events = rabbitMQEventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RabbitMQEvent> getEventById(@PathVariable Long id) {
        return rabbitMQEventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get events by type
     */
    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<RabbitMQEvent>> getEventsByType(@PathVariable String eventType) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsByType(eventType);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by source
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<List<RabbitMQEvent>> getEventsBySource(@PathVariable String source) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsBySource(source);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<RabbitMQEvent>> getEventsByStatus(@PathVariable RabbitMQEvent.EventStatus status) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsByStatus(status);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by time period
     */
    @GetMapping("/period")
    public ResponseEntity<List<RabbitMQEvent>> getEventsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsByTimePeriod(start, end);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by exchange
     */
    @GetMapping("/exchange/{exchange}")
    public ResponseEntity<List<RabbitMQEvent>> getEventsByExchange(@PathVariable String exchange) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsByExchange(exchange);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by queue
     */
    @GetMapping("/queue/{queue}")
    public ResponseEntity<List<RabbitMQEvent>> getEventsByQueue(@PathVariable String queue) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsByQueue(queue);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by routing key
     */
    @GetMapping("/routing-key/{routingKey}")
    public ResponseEntity<List<RabbitMQEvent>> getEventsByRoutingKey(@PathVariable String routingKey) {
        List<RabbitMQEvent> events = rabbitMQEventService.getEventsByRoutingKey(routingKey);
        return ResponseEntity.ok(events);
    }

    /**
     * Delete event by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        rabbitMQEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}

