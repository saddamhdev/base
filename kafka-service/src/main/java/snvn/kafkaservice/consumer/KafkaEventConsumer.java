package snvn.kafkaservice.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import snvn.kafkaservice.model.EventMessage;

@Service
public class KafkaEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @KafkaListener(topics = "${kafka.topic.name:kafka-event-topic}",
                   groupId = "${spring.kafka.consumer.group-id:kafka-event-group}")
    public void consumeEvent(@Payload EventMessage eventMessage,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset,
                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        logger.info("Received event from Kafka - Topic: {}, Partition: {}, Offset: {}, Event: {}",
                    topic, partition, offset, eventMessage);

        try {
            processEvent(eventMessage);
            logger.info("Successfully processed event: {}", eventMessage.getEventType());
        } catch (Exception e) {
            logger.error("Error processing event: {} from partition: {}, offset: {}",
                        eventMessage.getEventType(), partition, offset, e);
        }
    }

    private void processEvent(EventMessage eventMessage) {
        logger.info("Processing event type: {} from source: {}",
                eventMessage.getEventType(),
                eventMessage.getSource());

        switch (eventMessage.getEventType()) {
            case "USER_CREATED":
                handleUserCreated(eventMessage);
                break;
            case "USER_UPDATED":
                handleUserUpdated(eventMessage);
                break;
            case "USER_DELETED":
                handleUserDeleted(eventMessage);
                break;
            case "ORDER_CREATED":
                handleOrderCreated(eventMessage);
                break;
            case "ORDER_UPDATED":
                handleOrderUpdated(eventMessage);
                break;
            case "PAYMENT_PROCESSED":
                handlePaymentProcessed(eventMessage);
                break;
            default:
                logger.warn("Unknown event type: {}", eventMessage.getEventType());
        }
    }

    private void handleUserCreated(EventMessage eventMessage) {
        logger.info("Handling USER_CREATED event: {}", eventMessage.getPayload());
    }

    private void handleUserUpdated(EventMessage eventMessage) {
        logger.info("Handling USER_UPDATED event: {}", eventMessage.getPayload());
    }

    private void handleUserDeleted(EventMessage eventMessage) {
        logger.info("Handling USER_DELETED event: {}", eventMessage.getPayload());
    }

    private void handleOrderCreated(EventMessage eventMessage) {
        logger.info("Handling ORDER_CREATED event: {}", eventMessage.getPayload());
    }

    private void handleOrderUpdated(EventMessage eventMessage) {
        logger.info("Handling ORDER_UPDATED event: {}", eventMessage.getPayload());
    }

    private void handlePaymentProcessed(EventMessage eventMessage) {
        logger.info("Handling PAYMENT_PROCESSED event: {}", eventMessage.getPayload());
    }
}

