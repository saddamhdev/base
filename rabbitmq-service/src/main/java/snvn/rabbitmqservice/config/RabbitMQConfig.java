package snvn.rabbitmqservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.name:rabbitmq-event-queue}")
    private String queueName;

    @Value("${rabbitmq.exchange.name:rabbitmq-event-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key:rabbitmq-event-routing-key}")
    private String routingKey;

    // Dead Letter Queue Configuration
    @Value("${rabbitmq.dlq.queue.name:rabbitmq-event-dlq}")
    private String dlqName;

    @Value("${rabbitmq.dlq.exchange.name:rabbitmq-event-dlq-exchange}")
    private String dlqExchangeName;

    @Value("${rabbitmq.dlq.routing.key:rabbitmq-event-dlq-routing-key}")
    private String dlqRoutingKey;

    // Fanout Exchange Configuration
    @Value("${rabbitmq.fanout.exchange.name:rabbitmq-fanout-exchange}")
    private String fanoutExchangeName;

    @Value("${rabbitmq.fanout.queue.notification:rabbitmq-fanout-notification-queue}")
    private String fanoutNotificationQueue;

    @Value("${rabbitmq.fanout.queue.audit:rabbitmq-fanout-audit-queue}")
    private String fanoutAuditQueue;

    @Value("${rabbitmq.fanout.queue.analytics:rabbitmq-fanout-analytics-queue}")
    private String fanoutAnalyticsQueue;

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlqExchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(dlqName, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlqExchangeName);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(queue())
                .to(exchange())
                .with(routingKey);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(dlqRoutingKey);
    }

    // ==================== Fanout Exchange Configuration ====================

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(fanoutExchangeName);
    }

    @Bean
    public Queue fanoutNotificationQueue() {
        return QueueBuilder.durable(fanoutNotificationQueue).build();
    }

    @Bean
    public Queue fanoutAuditQueue() {
        return QueueBuilder.durable(fanoutAuditQueue).build();
    }

    @Bean
    public Queue fanoutAnalyticsQueue() {
        return QueueBuilder.durable(fanoutAnalyticsQueue).build();
    }

    @Bean
    public Binding fanoutNotificationBinding() {
        return BindingBuilder
                .bind(fanoutNotificationQueue())
                .to(fanoutExchange());
    }

    @Bean
    public Binding fanoutAuditBinding() {
        return BindingBuilder
                .bind(fanoutAuditQueue())
                .to(fanoutExchange());
    }

    @Bean
    public Binding fanoutAnalyticsBinding() {
        return BindingBuilder
                .bind(fanoutAnalyticsQueue())
                .to(fanoutExchange());
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public BeanPostProcessor rabbitTemplateBeanPostProcessor(MessageConverter messageConverter) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RabbitTemplate rabbitTemplate) {
                    rabbitTemplate.setMessageConverter(messageConverter);
                }
                return bean;
            }
        };
    }
}

