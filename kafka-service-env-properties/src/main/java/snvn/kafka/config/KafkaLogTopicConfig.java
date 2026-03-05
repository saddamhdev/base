package snvn.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka infrastructure for the external-log pipeline.
 * <p>
 * Creates the log topic automatically if it doesn't exist.
 * <p>
 * All configuration values come from {@link KafkaLogProperties}
 * ({@code @ConfigurationProperties(prefix = "kafka-service-log")}).
 * <p>
 * Producer: {@link KafkaExternalLogServiceImplementation}
 * Consumer: {@link KafkaExternalLogConsumer}
 */
@Configuration
public class KafkaLogTopicConfig {

    private final KafkaLogProperties properties;

    public KafkaLogTopicConfig(KafkaLogProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NewTopic logTopic() {
        return TopicBuilder.name(properties.getTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}

