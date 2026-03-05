package snvn.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ infrastructure for the external-log pipeline.
 * <p>
 * Uses a {@link FanoutExchange} so every log event is broadcast to
 * <b>all</b> bound queues — routing key is ignored.
 * <p>
 * All configuration values come from {@link RabbitMQLogProperties}
 * ({@code @ConfigurationProperties(prefix = "rabbitmq-service-log")}).
 * <p>
 * Producer: {@link RabbitMQExternalLogServiceImplementation}
 * Consumer: {@link RabbitMQExternalLogConsumer}
 */
@Configuration
public class LogExchangeConfig {

    private final RabbitMQLogProperties properties;

    public LogExchangeConfig(RabbitMQLogProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FanoutExchange logExchange() {
        return new FanoutExchange(properties.getExchange());
    }

    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(properties.getQueue()).build();
    }

    @Bean
    public Binding logBinding() {
        return BindingBuilder
                .bind(logQueue())
                .to(logExchange());
    }
}
