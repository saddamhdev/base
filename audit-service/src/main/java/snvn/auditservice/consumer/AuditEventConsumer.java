package snvn.auditservice.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import snvn.auditservice.service.AuditService;

/**
 * Kafka consumer for audit events
 */
@Component
public class AuditEventConsumer {

    @Autowired
    private AuditService auditService;

    /**
     * Listen to audit events from Kafka
     */
    @KafkaListener(topics = "audit-events", groupId = "audit-event-group")
    public void consumeAuditEvent(String message) {
        System.out.println("Received audit event: " + message);
        // Process audit event and create audit log
        // This is a placeholder - implement actual event processing logic
    }
}

