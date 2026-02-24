package snvn.rabbitmqservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snvn.rabbitmqservice.model.RabbitMQEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RabbitMQEventRepository extends JpaRepository<RabbitMQEvent, Long> {

    List<RabbitMQEvent> findByEventType(String eventType);

    List<RabbitMQEvent> findBySource(String source);

    List<RabbitMQEvent> findByStatus(RabbitMQEvent.EventStatus status);

    List<RabbitMQEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<RabbitMQEvent> findByEventTypeAndStatus(String eventType, RabbitMQEvent.EventStatus status);

    List<RabbitMQEvent> findByExchange(String exchange);

    List<RabbitMQEvent> findByQueue(String queue);

    List<RabbitMQEvent> findByRoutingKey(String routingKey);
}

