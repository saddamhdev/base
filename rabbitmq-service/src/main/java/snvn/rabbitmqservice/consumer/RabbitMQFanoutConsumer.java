package snvn.rabbitmqservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import snvn.rabbitmqservice.model.EventMessage;

/**
 * Fanout consumer that listens on all fanout-bound queues.
 * A FanoutExchange broadcasts every message to ALL bound queues,
 * so each listener below receives a copy of the same message.
 */
@Service
public class RabbitMQFanoutConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQFanoutConsumer.class);

    // ==================== Notification Queue Consumer ====================

    @RabbitListener(queues = "${rabbitmq.fanout.queue.notification:rabbitmq-fanout-notification-queue}")
    public void consumeNotification(EventMessage eventMessage, Message message) {
        logger.info("[Fanout-Notification] Received event: type={}, source={}, exchange={}",
                eventMessage.getEventType(),
                eventMessage.getSource(),
                message.getMessageProperties().getReceivedExchange());

        try {
            processNotification(eventMessage);
            logger.info("[Fanout-Notification] Successfully processed event: {}", eventMessage.getEventType());
        } catch (Exception e) {
            logger.error("[Fanout-Notification] Error processing event: {}", eventMessage.getEventType(), e);
            throw e;
        }
    }

    // ==================== Audit Queue Consumer ====================

    @RabbitListener(queues = "${rabbitmq.fanout.queue.audit:rabbitmq-fanout-audit-queue}")
    public void consumeAudit(EventMessage eventMessage, Message message) {
        logger.info("[Fanout-Audit] Received event: type={}, source={}, exchange={}",
                eventMessage.getEventType(),
                eventMessage.getSource(),
                message.getMessageProperties().getReceivedExchange());

        try {
            processAudit(eventMessage);
            logger.info("[Fanout-Audit] Successfully processed event: {}", eventMessage.getEventType());
        } catch (Exception e) {
            logger.error("[Fanout-Audit] Error processing event: {}", eventMessage.getEventType(), e);
            throw e;
        }
    }

    // ==================== Analytics Queue Consumer ====================

    @RabbitListener(queues = "${rabbitmq.fanout.queue.analytics:rabbitmq-fanout-analytics-queue}")
    public void consumeAnalytics(EventMessage eventMessage, Message message) {
        logger.info("[Fanout-Analytics] Received event: type={}, source={}, exchange={}",
                eventMessage.getEventType(),
                eventMessage.getSource(),
                message.getMessageProperties().getReceivedExchange());

        try {
            processAnalytics(eventMessage);
            logger.info("[Fanout-Analytics] Successfully processed event: {}", eventMessage.getEventType());
        } catch (Exception e) {
            logger.error("[Fanout-Analytics] Error processing event: {}", eventMessage.getEventType(), e);
            throw e;
        }
    }

    // ==================== Processing Methods ====================

    private void processNotification(EventMessage eventMessage) {
        logger.info("[Fanout-Notification] Processing notification for event: {} | payload: {}",
                eventMessage.getEventType(), eventMessage.getPayload());
        // TODO: Send email, push notification, SMS, etc.
    }

    private void processAudit(EventMessage eventMessage) {
        logger.info("[Fanout-Audit] Auditing event: {} | source: {} | timestamp: {}",
                eventMessage.getEventType(), eventMessage.getSource(), eventMessage.getTimestamp());
        // TODO: Persist audit trail to database or external audit system
    }

    private void processAnalytics(EventMessage eventMessage) {
        logger.info("[Fanout-Analytics] Recording analytics for event: {} | source: {}",
                eventMessage.getEventType(), eventMessage.getSource());
        // TODO: Forward to analytics pipeline, update dashboards, etc.
    }
}

