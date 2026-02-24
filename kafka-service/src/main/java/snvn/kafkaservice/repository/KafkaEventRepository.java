package snvn.kafkaservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snvn.kafkaservice.model.KafkaEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KafkaEventRepository extends JpaRepository<KafkaEvent, Long> {

    List<KafkaEvent> findByEventType(String eventType);

    List<KafkaEvent> findBySource(String source);

    List<KafkaEvent> findByStatus(KafkaEvent.EventStatus status);

    List<KafkaEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<KafkaEvent> findByEventTypeAndStatus(String eventType, KafkaEvent.EventStatus status);

    List<KafkaEvent> findByTopic(String topic);

    List<KafkaEvent> findByTopicAndPartition(String topic, Integer partition);
}

