package snvn.kafkaservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import snvn.kafkaservice.model.KafkaEvent;
import snvn.kafkaservice.service.KafkaEventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kafka/events")
public class KafkaEventController {

    @Autowired
    private KafkaEventService kafkaEventService;

    /**
     * Publish event to Kafka
     */
    @PostMapping
    public ResponseEntity<KafkaEvent> publishEvent(@RequestBody Map<String, String> request) {
        String eventType = request.get("eventType");
        String payload = request.get("payload");
        String source = request.getOrDefault("source", "kafka-service");

        KafkaEvent event = kafkaEventService.publishEvent(eventType, payload, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * Get all events
     */
    @GetMapping
    public ResponseEntity<List<KafkaEvent>> getAllEvents() {
        List<KafkaEvent> events = kafkaEventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<KafkaEvent> getEventById(@PathVariable Long id) {
        return kafkaEventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get events by type
     */
    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<KafkaEvent>> getEventsByType(@PathVariable String eventType) {
        List<KafkaEvent> events = kafkaEventService.getEventsByType(eventType);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by source
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<List<KafkaEvent>> getEventsBySource(@PathVariable String source) {
        List<KafkaEvent> events = kafkaEventService.getEventsBySource(source);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<KafkaEvent>> getEventsByStatus(@PathVariable KafkaEvent.EventStatus status) {
        List<KafkaEvent> events = kafkaEventService.getEventsByStatus(status);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by time period
     */
    @GetMapping("/period")
    public ResponseEntity<List<KafkaEvent>> getEventsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<KafkaEvent> events = kafkaEventService.getEventsByTimePeriod(start, end);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by topic
     */
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<KafkaEvent>> getEventsByTopic(@PathVariable String topic) {
        List<KafkaEvent> events = kafkaEventService.getEventsByTopic(topic);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by topic and partition
     */
    @GetMapping("/topic/{topic}/partition/{partition}")
    public ResponseEntity<List<KafkaEvent>> getEventsByTopicAndPartition(
            @PathVariable String topic,
            @PathVariable Integer partition) {
        List<KafkaEvent> events = kafkaEventService.getEventsByTopicAndPartition(topic, partition);
        return ResponseEntity.ok(events);
    }

    /**
     * Delete event by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        kafkaEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}

