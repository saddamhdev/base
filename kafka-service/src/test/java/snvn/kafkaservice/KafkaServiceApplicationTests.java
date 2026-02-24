package snvn.kafkaservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092"
})
class KafkaServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}

