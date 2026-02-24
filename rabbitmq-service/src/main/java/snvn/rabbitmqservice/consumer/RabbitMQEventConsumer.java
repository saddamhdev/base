package snvn.rabbitmqservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import snvn.rabbitmqservice.model.EventMessage;

@Service
public class RabbitMQEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventConsumer.class);

    @RabbitListener(queues = "${rabbitmq.queue.name:rabbitmq-event-queue}")
    public void consumeEvent(EventMessage eventMessage, Message message) {
        logger.info("Received event from RabbitMQ - Exchange: {}, Routing Key: {}, Event: {}",
                    message.getMessageProperties().getReceivedExchange(),
                    message.getMessageProperties().getReceivedRoutingKey(),
                    eventMessage);

        try {
            processEvent(eventMessage);
            logger.info("Successfully processed event: {}", eventMessage.getEventType());
        } catch (Exception e) {
            logger.error("Error processing event: {} from exchange: {}, routing key: {}",
                        eventMessage.getEventType(),
                        message.getMessageProperties().getReceivedExchange(),
                        message.getMessageProperties().getReceivedRoutingKey(), e);
            throw e; // This will send the message to DLQ if configured
        }
    }

    @RabbitListener(queues = "${rabbitmq.dlq.queue.name:rabbitmq-event-dlq}")
    public void consumeDeadLetterEvent(EventMessage eventMessage, Message message) {
        logger.warn("Received event from Dead Letter Queue - Event: {}", eventMessage);
        logger.warn("Original Exchange: {}, Original Routing Key: {}",
                   message.getMessageProperties().getHeader("x-first-death-exchange"),
                   message.getMessageProperties().getHeader("x-first-death-routing-key"));

        // Handle dead letter messages (e.g., store in database, send alert, etc.)
    }

    private void processEvent(EventMessage eventMessage) {
        logger.info("Processing event type: {} from source: {}",
                eventMessage.getEventType(),
                eventMessage.getSource());

        switch (eventMessage.getEventType()) {
            case "ORDER_CREATED":
                handleOrderCreated(eventMessage);
                break;
            case "ORDER_UPDATED":
                handleOrderUpdated(eventMessage);
                break;
            case "ORDER_CANCELLED":
                handleOrderCancelled(eventMessage);
                break;
            case "PAYMENT_PROCESSED":
                handlePaymentProcessed(eventMessage);
                break;
            case "INVENTORY_UPDATED":
                handleInventoryUpdated(eventMessage);
                break;
            case "NOTIFICATION_SENT":
                handleNotificationSent(eventMessage);
                break;
            default:
                logger.warn("Unknown event type: {}", eventMessage.getEventType());
        }
    }

    private void handleOrderCreated(EventMessage eventMessage) {
        logger.info("Handling ORDER_CREATED event: {}", eventMessage.getPayload());
    }

    private void handleOrderUpdated(EventMessage eventMessage) {
        logger.info("Handling ORDER_UPDATED event: {}", eventMessage.getPayload());
    }

    private void handleOrderCancelled(EventMessage eventMessage) {
        logger.info("Handling ORDER_CANCELLED event: {}", eventMessage.getPayload());
    }

    private void handlePaymentProcessed(EventMessage eventMessage) {
        logger.info("Handling PAYMENT_PROCESSED event: {}", eventMessage.getPayload());
    }

    private void handleInventoryUpdated(EventMessage eventMessage) {
        logger.info("Handling INVENTORY_UPDATED event: {}", eventMessage.getPayload());
    }

    private void handleNotificationSent(EventMessage eventMessage) {
        logger.info("Handling NOTIFICATION_SENT event: {}", eventMessage.getPayload());
    }
}

