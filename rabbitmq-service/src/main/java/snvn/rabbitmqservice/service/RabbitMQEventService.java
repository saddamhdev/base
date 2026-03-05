package snvn.rabbitmqservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import snvn.rabbitmqservice.model.EventMessage;
import snvn.rabbitmqservice.model.RabbitMQEvent;
import snvn.rabbitmqservice.producer.RabbitMQEventProducer;
import snvn.rabbitmqservice.repository.RabbitMQEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RabbitMQEventService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventService.class);

    @Autowired
    private RabbitMQEventRepository rabbitMQEventRepository;

    @Autowired
    private RabbitMQEventProducer rabbitMQEventProducer;

    @Value("${rabbitmq.exchange.name:rabbitmq-event-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key:rabbitmq-event-routing-key}")
    private String routingKey;

    @Value("${rabbitmq.queue.name:rabbitmq-event-queue}")
    private String queueName;

    @Value("${rabbitmq.fanout.exchange.name:rabbitmq-fanout-exchange}")
    private String fanoutExchangeName;

    @Transactional
    public RabbitMQEvent publishEvent(String eventType, String payload, String source) {
        logger.info("Publishing event to RabbitMQ: {}", eventType);

        RabbitMQEvent event = new RabbitMQEvent(eventType, payload, source);
        event.setExchange(exchangeName);
        event.setRoutingKey(routingKey);
        event.setQueue(queueName);
        event = rabbitMQEventRepository.save(event);

        try {
            EventMessage eventMessage = new EventMessage(eventType, payload, source);
            rabbitMQEventProducer.sendEvent(eventMessage);
            event.setStatus(RabbitMQEvent.EventStatus.SENT);
        } catch (Exception e) {
            logger.error("Failed to publish event to RabbitMQ", e);
            event.setStatus(RabbitMQEvent.EventStatus.FAILED);
            throw e;
        } finally {
            rabbitMQEventRepository.save(event);
        }

        return event;
    }

    @Transactional
    public RabbitMQEvent publishEventWithCustomRouting(String eventType, String payload, String source, String customRoutingKey) {
        logger.info("Publishing event to RabbitMQ with custom routing key {}: {}", customRoutingKey, eventType);

        RabbitMQEvent event = new RabbitMQEvent(eventType, payload, source);
        event.setExchange(exchangeName);
        event.setRoutingKey(customRoutingKey);
        event.setQueue(queueName);
        event = rabbitMQEventRepository.save(event);

        try {
            EventMessage eventMessage = new EventMessage(eventType, payload, source);
            rabbitMQEventProducer.sendEventWithCustomRouting(eventMessage, customRoutingKey);
            event.setStatus(RabbitMQEvent.EventStatus.SENT);
        } catch (Exception e) {
            logger.error("Failed to publish event to RabbitMQ", e);
            event.setStatus(RabbitMQEvent.EventStatus.FAILED);
            throw e;
        } finally {
            rabbitMQEventRepository.save(event);
        }

        return event;
    }

    /**
     * Publish event to fanout exchange — broadcasts to all bound queues
     * (notification, audit, analytics).
     */
    @Transactional
    public RabbitMQEvent publishFanoutEvent(String eventType, String payload, String source) {
        logger.info("Broadcasting fanout event to RabbitMQ: {}", eventType);

        RabbitMQEvent event = new RabbitMQEvent(eventType, payload, source);
        event.setExchange(fanoutExchangeName);
        event.setRoutingKey(""); // Fanout ignores routing key
        event.setQueue("fanout-all");
        event = rabbitMQEventRepository.save(event);

        try {
            EventMessage eventMessage = new EventMessage(eventType, payload, source);
            rabbitMQEventProducer.sendFanoutEvent(eventMessage);
            event.setStatus(RabbitMQEvent.EventStatus.SENT);
        } catch (Exception e) {
            logger.error("Failed to broadcast fanout event to RabbitMQ", e);
            event.setStatus(RabbitMQEvent.EventStatus.FAILED);
            throw e;
        } finally {
            rabbitMQEventRepository.save(event);
        }

        return event;
    }

    public List<RabbitMQEvent> getAllEvents() {
        return rabbitMQEventRepository.findAll();
    }

    public Optional<RabbitMQEvent> getEventById(Long id) {
        return rabbitMQEventRepository.findById(id);
    }

    public List<RabbitMQEvent> getEventsByType(String eventType) {
        return rabbitMQEventRepository.findByEventType(eventType);
    }

    public List<RabbitMQEvent> getEventsBySource(String source) {
        return rabbitMQEventRepository.findBySource(source);
    }

    public List<RabbitMQEvent> getEventsByStatus(RabbitMQEvent.EventStatus status) {
        return rabbitMQEventRepository.findByStatus(status);
    }

    public List<RabbitMQEvent> getEventsByTimePeriod(LocalDateTime start, LocalDateTime end) {
        return rabbitMQEventRepository.findByTimestampBetween(start, end);
    }

    public List<RabbitMQEvent> getEventsByExchange(String exchange) {
        return rabbitMQEventRepository.findByExchange(exchange);
    }

    public List<RabbitMQEvent> getEventsByQueue(String queue) {
        return rabbitMQEventRepository.findByQueue(queue);
    }

    public List<RabbitMQEvent> getEventsByRoutingKey(String routingKey) {
        return rabbitMQEventRepository.findByRoutingKey(routingKey);
    }

    @Transactional
    public void deleteEvent(Long id) {
        rabbitMQEventRepository.deleteById(id);
    }
}

