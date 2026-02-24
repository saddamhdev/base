package snvn.kafkaservice.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import snvn.kafkaservice.model.EventMessage;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventProducer.class);

    @Autowired
    private KafkaTemplate<String, EventMessage> kafkaTemplate;

    @Value("${kafka.topic.name:kafka-event-topic}")
    private String topicName;

    public CompletableFuture<SendResult<String, EventMessage>> sendEvent(EventMessage eventMessage) {
        logger.info("Sending event to Kafka topic {}: {}", topicName, eventMessage);

        CompletableFuture<SendResult<String, EventMessage>> future =
                kafkaTemplate.send(topicName, eventMessage);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Event sent successfully to Kafka: {} with offset: {} on partition: {}",
                        eventMessage.getEventType(),
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send event to Kafka: {}", eventMessage.getEventType(), ex);
            }
        });

        return future;
    }

    public CompletableFuture<SendResult<String, EventMessage>> sendEvent(String eventType, String payload, String source) {
        EventMessage eventMessage = new EventMessage(eventType, payload, source);
        return sendEvent(eventMessage);
    }
}

