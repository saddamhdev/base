package snvn.authservice.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for authentication events
 */
@Component
public class AuthEventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "auth-events";

    /**
     * Send user registration event
     */
    public void sendUserRegisteredEvent(String username, String email) {
        String message = String.format("User registered: %s (%s)", username, email);
        kafkaTemplate.send(TOPIC, username, message);
        System.out.println("Published user registration event: " + message);
    }

    /**
     * Send login event
     */
    public void sendLoginEvent(String username) {
        String message = String.format("User logged in: %s", username);
        kafkaTemplate.send(TOPIC, username, message);
        System.out.println("Published login event: " + message);
    }

    /**
     * Send logout event
     */
    public void sendLogoutEvent(String username) {
        String message = String.format("User logged out: %s", username);
        kafkaTemplate.send(TOPIC, username, message);
        System.out.println("Published logout event: " + message);
    }
}

