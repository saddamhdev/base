# Event Services - Kafka & RabbitMQ

This directory contains two separate microservices for event-driven architecture:

1. **kafka-service** (Port 8084) - Apache Kafka based messaging
2. **rabbitmq-service** (Port 8085) - RabbitMQ based messaging

## Quick Comparison

| Feature | Kafka Service | RabbitMQ Service |
|---------|---------------|------------------|
| **Port** | 8084 | 8085 |
| **Message Broker** | Apache Kafka | RabbitMQ |
| **Architecture** | Distributed log | Message queue |
| **Routing** | Topics & Partitions | Exchanges & Routing Keys |
| **Ordering** | Per partition | Per queue |
| **Persistence** | Always | Optional |
| **Throughput** | Very High | High |
| **Latency** | Low | Very Low |
| **Use Case** | Event streaming, logs | Task queues, RPC |
| **DLQ Support** | Manual | Built-in |
| **Retention** | Time/size based | Until consumed |

## When to Use Which?

### Use Kafka Service When:
- You need **high throughput** event streaming
- You want to **replay events** (event sourcing)
- You need **partitioned parallelism**
- You're building **real-time analytics**
- You need **long-term event retention**
- You have **multiple consumers** reading the same events

**Examples:**
- User activity tracking
- System logs aggregation
- Real-time analytics
- Event sourcing
- Change Data Capture (CDC)

### Use RabbitMQ Service When:
- You need **guaranteed delivery** with acknowledgments
- You need **complex routing** patterns
- You need **RPC-style** request/response
- You need **priority queues**
- You need **built-in DLQ** for failed messages
- Your messages are **short-lived tasks**

**Examples:**
- Order processing
- Email notifications
- Task scheduling
- Payment processing
- Inventory updates
- Webhook callbacks

## Quick Start

### 1. Start Both Services with Docker

```bash
# Start Kafka
cd D:\module project\base\kafka-service
docker-compose up -d

# Start RabbitMQ
cd D:\module project\base\rabbitmq-service
docker-compose up -d
```

### 2. Build and Run

```bash
# Build all modules
cd D:\module project\base
mvn clean install

# Run Kafka Service
cd kafka-service
mvn spring-boot:run

# In another terminal, run RabbitMQ Service
cd rabbitmq-service
mvn spring-boot:run
```

### 3. Test Both Services

**Test Kafka:**
```bash
curl -X POST http://localhost:8084/api/kafka/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"USER_CREATED","payload":"{\"userId\":1}","source":"test"}'
```

**Test RabbitMQ:**
```bash
curl -X POST http://localhost:8085/api/rabbitmq/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER_CREATED","payload":"{\"orderId\":123}","source":"test"}'
```

## Management UIs

- **Kafka UI**: http://localhost:8080
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Kafka Service H2**: http://localhost:8084/h2-console
- **RabbitMQ Service H2**: http://localhost:8085/h2-console

## API Overview

### Kafka Service (Port 8084)

```
POST   /api/kafka/events                           - Publish event
GET    /api/kafka/events                           - Get all events
GET    /api/kafka/events/{id}                      - Get event by ID
GET    /api/kafka/events/type/{eventType}          - Get by type
GET    /api/kafka/events/topic/{topic}             - Get by topic
GET    /api/kafka/events/topic/{topic}/partition/{partition} - Get by partition
DELETE /api/kafka/events/{id}                      - Delete event
```

### RabbitMQ Service (Port 8085)

```
POST   /api/rabbitmq/events                        - Publish event
POST   /api/rabbitmq/events/custom-routing         - Publish with custom routing
GET    /api/rabbitmq/events                        - Get all events
GET    /api/rabbitmq/events/{id}                   - Get event by ID
GET    /api/rabbitmq/events/type/{eventType}       - Get by type
GET    /api/rabbitmq/events/exchange/{exchange}    - Get by exchange
GET    /api/rabbitmq/events/routing-key/{key}      - Get by routing key
DELETE /api/rabbitmq/events/{id}                   - Delete event
```

## Architecture Patterns

### Kafka Service - Event Streaming Pattern

```
Producer → Kafka Topic (Partitioned) → Multiple Consumers
                ↓
         Event Store (H2)
```

- Events are **immutable** and **retained**
- Consumers can **replay** from any offset
- **Horizontal scaling** via partitions
- **Consumer groups** for load balancing

### RabbitMQ Service - Message Queue Pattern

```
Producer → Exchange → Queue → Consumer
                ↓       ↓
         Event Store   DLQ (on failure)
```

- Messages are **consumed and removed**
- **Acknowledgments** ensure delivery
- **Dead Letter Queue** for failures
- **Retry mechanism** with backoff

## Event Types

Both services support these event types (extendable):

### Kafka Service
- USER_CREATED
- USER_UPDATED
- USER_DELETED
- ORDER_CREATED
- ORDER_UPDATED
- PAYMENT_PROCESSED

### RabbitMQ Service
- ORDER_CREATED
- ORDER_UPDATED
- ORDER_CANCELLED
- PAYMENT_PROCESSED
- INVENTORY_UPDATED
- NOTIFICATION_SENT

## Integration Examples

### Publishing to Both Services

```java
@Service
public class EventPublisher {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public void publishToBoth(String eventType, Object payload) {
        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        Map<String, String> event = Map.of(
            "eventType", eventType,
            "payload", jsonPayload,
            "source", "integration-service"
        );
        
        // Publish to Kafka
        restTemplate.postForEntity(
            "http://localhost:8084/api/kafka/events",
            event,
            Object.class
        );
        
        // Publish to RabbitMQ
        restTemplate.postForEntity(
            "http://localhost:8085/api/rabbitmq/events",
            event,
            Object.class
        );
    }
}
```

### Consuming Events

**Kafka Consumer:**
```java
@KafkaListener(topics = "kafka-event-topic", groupId = "my-service")
public void handleKafkaEvent(EventMessage event) {
    // Process event
}
```

**RabbitMQ Consumer:**
```java
@RabbitListener(queues = "rabbitmq-event-queue")
public void handleRabbitMQEvent(EventMessage event) {
    // Process event
}
```

## Configuration Files

### Kafka Service - application.yml
```yaml
server:
  port: 8084

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: kafka-event-group

kafka:
  topic:
    name: kafka-event-topic
```

### RabbitMQ Service - application.yml
```yaml
server:
  port: 8085

spring:
  rabbitmq:
    host: localhost
    port: 5672

rabbitmq:
  queue:
    name: rabbitmq-event-queue
  exchange:
    name: rabbitmq-event-exchange
```

## Monitoring & Health Checks

### Health Endpoints
- Kafka: http://localhost:8084/actuator/health
- RabbitMQ: http://localhost:8085/actuator/health

### Metrics
- Kafka: http://localhost:8084/actuator/metrics
- RabbitMQ: http://localhost:8085/actuator/metrics

## Stop Services

### Stop Docker Containers
```bash
# Stop Kafka
cd D:\module project\base\kafka-service
docker-compose down

# Stop RabbitMQ
cd D:\module project\base\rabbitmq-service
docker-compose down
```

### Stop Spring Boot Services
Press `Ctrl+C` in each terminal

## Troubleshooting

### Kafka Issues
- **Connection refused**: Check if Kafka is running on port 9092
- **No leader**: Wait for Kafka to elect leader (startup delay)
- **Consumer lag**: Check consumer group in Kafka UI

### RabbitMQ Issues
- **Connection refused**: Check if RabbitMQ is running on port 5672
- **Queue not found**: Check queue/exchange bindings in Management UI
- **Messages in DLQ**: Check consumer logs for errors

## Production Considerations

### Kafka Service
1. **Replication**: Set replication factor > 1
2. **Partitions**: Increase for higher throughput
3. **Retention**: Configure based on storage capacity
4. **Consumer Groups**: Use for horizontal scaling
5. **Monitoring**: Use Prometheus + Grafana

### RabbitMQ Service
1. **Clustering**: Set up RabbitMQ cluster for HA
2. **Persistence**: Use durable queues/exchanges
3. **Prefetch**: Tune prefetch count for performance
4. **Memory Limits**: Configure memory high watermark
5. **Monitoring**: Enable RabbitMQ Prometheus plugin

## Further Documentation

- [Kafka Service README](kafka-service/README.md)
- [RabbitMQ Service README](rabbitmq-service/README.md)

## License

Part of the SNVN microservices project.

