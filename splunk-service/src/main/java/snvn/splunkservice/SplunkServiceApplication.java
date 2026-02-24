package snvn.splunkservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"snvn.splunkservice", "snvn.common"})
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class SplunkServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SplunkServiceApplication.class, args);
    }
}


