package snvn.transactionservice.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import snvn.transactionservice.service.TransactionService;

/**
 * Kafka consumer for transaction-related events
 */
@Component
public class TransactionEventConsumer {

    @Autowired
    private TransactionService transactionService;

    /**
     * Listen to transaction validation events from Kafka
     */
    @KafkaListener(topics = "transaction-validation", groupId = "transaction-event-group")
    public void consumeValidationEvent(String message) {
        System.out.println("Received transaction validation event: " + message);
        // Process validation event
        // This is a placeholder - implement actual event processing logic
    }

    /**
     * Listen to account events that might affect transactions
     */
    @KafkaListener(topics = "account-events", groupId = "transaction-event-group")
    public void consumeAccountEvent(String message) {
        System.out.println("Received account event: " + message);
        // Process account event that might impact transactions
        // This is a placeholder - implement actual event processing logic
    }
}

