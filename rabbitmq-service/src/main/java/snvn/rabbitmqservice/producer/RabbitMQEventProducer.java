package snvn.rabbitmqservice.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import snvn.rabbitmqservice.model.EventMessage;

@Service
public class RabbitMQEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name:rabbitmq-event-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key:rabbitmq-event-routing-key}")
    private String routingKey;

    @Value("${rabbitmq.fanout.exchange.name:rabbitmq-fanout-exchange}")
    private String fanoutExchangeName;

    public void sendEvent(EventMessage eventMessage) {
        logger.info("Sending event to RabbitMQ exchange {}: {}", exchangeName, eventMessage);

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, eventMessage);
            logger.info("Event sent successfully to RabbitMQ: {} via exchange: {} with routing key: {}",
                    eventMessage.getEventType(), exchangeName, routingKey);
        } catch (Exception e) {
            logger.error("Failed to send event to RabbitMQ: {}", eventMessage.getEventType(), e);
            throw e;
        }
    }

    public void sendEvent(String eventType, String payload, String source) {
        EventMessage eventMessage = new EventMessage(eventType, payload, source);
        sendEvent(eventMessage);
    }

    public void sendEventWithCustomRouting(EventMessage eventMessage, String customRoutingKey) {
        logger.info("Sending event to RabbitMQ with custom routing key {}: {}", customRoutingKey, eventMessage);

        try {
            rabbitTemplate.convertAndSend(exchangeName, customRoutingKey, eventMessage);
            logger.info("Event sent successfully to RabbitMQ: {} via exchange: {} with routing key: {}",
                    eventMessage.getEventType(), exchangeName, customRoutingKey);
        } catch (Exception e) {
            logger.error("Failed to send event to RabbitMQ: {}", eventMessage.getEventType(), e);
            throw e;
        }
    }

    /**
     * Send event to fanout exchange — broadcasts to ALL bound queues.
     * Routing key is ignored by FanoutExchange, so we pass empty string.
     */
    public void sendFanoutEvent(EventMessage eventMessage) {
        logger.info("Broadcasting fanout event to exchange {}: {}", fanoutExchangeName, eventMessage);

        try {
            rabbitTemplate.convertAndSend(fanoutExchangeName, "", eventMessage);
            logger.info("Fanout event broadcast successfully: {} via exchange: {}",
                    eventMessage.getEventType(), fanoutExchangeName);
        } catch (Exception e) {
            logger.error("Failed to broadcast fanout event: {}", eventMessage.getEventType(), e);
            throw e;
        }
    }

    public void sendFanoutEvent(String eventType, String payload, String source) {
        EventMessage eventMessage = new EventMessage(eventType, payload, source);
        sendFanoutEvent(eventMessage);
    }
}

