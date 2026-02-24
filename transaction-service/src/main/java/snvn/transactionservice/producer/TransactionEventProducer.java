package snvn.transactionservice.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import snvn.transactionservice.model.Transaction;

/**
 * Kafka producer for transaction events
 */
@Component
public class TransactionEventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    /**
     * Send transaction event to Kafka
     */
    public void sendTransactionEvent(Transaction transaction) {
        System.out.println("Publishing transaction event: " + transaction.getTransactionId());
        kafkaTemplate.send(TOPIC, transaction.getTransactionId(), transaction);
    }

    /**
     * Send transaction status update event
     */
    public void sendTransactionStatusUpdate(String transactionId, String status) {
        String message = String.format("Transaction %s status updated to %s", transactionId, status);
        kafkaTemplate.send(TOPIC, transactionId, message);
    }
}

