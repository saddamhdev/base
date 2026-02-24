package snvn.rabbitmqservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost"
})
class RabbitMQServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}

