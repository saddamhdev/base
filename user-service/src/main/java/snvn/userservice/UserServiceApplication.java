package snvn.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan(basePackages = {"snvn.model", "snvn.rabbitmqservice.model"})
@ComponentScan(basePackages = {"snvn.userservice", "snvn.common", "snvn.rabbitmqservice.config", "snvn.splunk", "snvn.rabbitmq", "snvn.log"})
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
