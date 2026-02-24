package snvn.kafkaservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import snvn.kafkaservice.model.EventMessage;
import snvn.kafkaservice.model.KafkaEvent;
import snvn.kafkaservice.producer.KafkaEventProducer;
import snvn.kafkaservice.repository.KafkaEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaEventService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventService.class);

    @Autowired
    private KafkaEventRepository kafkaEventRepository;

    @Autowired
    private KafkaEventProducer kafkaEventProducer;

    @Value("${kafka.topic.name:kafka-event-topic}")
    private String topicName;

    @Transactional
    public KafkaEvent publishEvent(String eventType, String payload, String source) {
        logger.info("Publishing event to Kafka: {}", eventType);

        KafkaEvent event = new KafkaEvent(eventType, payload, source);
        event.setTopic(topicName);
        event = kafkaEventRepository.save(event);

        KafkaEvent finalEvent = event;
        try {
            EventMessage eventMessage = new EventMessage(eventType, payload, source);
            CompletableFuture<SendResult<String, EventMessage>> future = kafkaEventProducer.sendEvent(eventMessage);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    finalEvent.setStatus(KafkaEvent.EventStatus.SENT);
                    finalEvent.setPartition(result.getRecordMetadata().partition());
                    finalEvent.setOffset(result.getRecordMetadata().offset());
                } else {
                    finalEvent.setStatus(KafkaEvent.EventStatus.FAILED);
                }
                kafkaEventRepository.save(finalEvent);
            });
        } catch (Exception e) {
            logger.error("Failed to publish event to Kafka", e);
            event.setStatus(KafkaEvent.EventStatus.FAILED);
            kafkaEventRepository.save(event);
            throw e;
        }

        return event;
    }

    public List<KafkaEvent> getAllEvents() {
        return kafkaEventRepository.findAll();
    }

    public Optional<KafkaEvent> getEventById(Long id) {
        return kafkaEventRepository.findById(id);
    }

    public List<KafkaEvent> getEventsByType(String eventType) {
        return kafkaEventRepository.findByEventType(eventType);
    }

    public List<KafkaEvent> getEventsBySource(String source) {
        return kafkaEventRepository.findBySource(source);
    }

    public List<KafkaEvent> getEventsByStatus(KafkaEvent.EventStatus status) {
        return kafkaEventRepository.findByStatus(status);
    }

    public List<KafkaEvent> getEventsByTimePeriod(LocalDateTime start, LocalDateTime end) {
        return kafkaEventRepository.findByTimestampBetween(start, end);
    }

    public List<KafkaEvent> getEventsByTopic(String topic) {
        return kafkaEventRepository.findByTopic(topic);
    }

    public List<KafkaEvent> getEventsByTopicAndPartition(String topic, Integer partition) {
        return kafkaEventRepository.findByTopicAndPartition(topic, partition);
    }

    @Transactional
    public void deleteEvent(Long id) {
        kafkaEventRepository.deleteById(id);
    }
}

