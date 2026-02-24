package snvn.kafkaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"snvn.kafkaservice", "snvn.common"})
public class KafkaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaServiceApplication.class, args);
    }
}

