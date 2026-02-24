# Kafka Service

A dedicated Spring Boot microservice for handling event-driven architecture using **Apache Kafka**.

## Features

- **Kafka Event Publishing**: REST API endpoints to publish events to Kafka topics
- **Kafka Event Consumption**: Automatic event consumers listening to Kafka topics
- **Event Persistence**: Stores event metadata in H2 database
- **Event Tracking**: Track event status, partition, and offset information
- **Idempotent Producer**: Ensures exactly-once semantics
- **Flexible Configuration**: Configurable topics, partitions, and consumer groups

## Architecture

### Components

1. **Controller**: REST API endpoints for Kafka event operations
2. **Producer**: `KafkaEventProducer` - Publishes events to Kafka topics
3. **Consumer**: `KafkaEventConsumer` - Listens to Kafka topics with partition/offset tracking
4. **Service Layer**: Business logic for event handling
5. **Repository**: JPA repository for event persistence
6. **Models**: KafkaEvent entity and EventMessage DTO

## Prerequisites

- Java 21
- Maven 3.x
- Apache Kafka server (default: localhost:9092)
- Zookeeper (required by Kafka)

## Running Kafka with Docker

```bash
cd D:\module project\base\kafka-service

# Start Kafka using docker-compose
docker-compose up -d
```

Or manually:

```bash
# Start Zookeeper
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.7

# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: kafka-event-group
    producer:
      acks: all
      retries: 3

kafka:
  topic:
    name: kafka-event-topic

server:
  port: 8084
```

## API Endpoints

### Publish Event to Kafka

```bash
POST http://localhost:8084/api/kafka/events
Content-Type: application/json

{
  "eventType": "USER_CREATED",
  "payload": "{\"userId\": 1, \"name\": \"John Doe\"}",
  "source": "user-service"
}
```

### Query Events

- `GET /api/kafka/events` - Get all events
- `GET /api/kafka/events/{id}` - Get event by ID
- `GET /api/kafka/events/type/{eventType}` - Get events by type
- `GET /api/kafka/events/source/{source}` - Get events by source
- `GET /api/kafka/events/status/{status}` - Get events by status
- `GET /api/kafka/events/topic/{topic}` - Get events by topic
- `GET /api/kafka/events/topic/{topic}/partition/{partition}` - Get events by topic and partition
- `GET /api/kafka/events/period?start={start}&end={end}` - Get events by time period
- `DELETE /api/kafka/events/{id}` - Delete event by ID

## Building and Running

### Build
```bash
cd D:\module project\base
mvn clean install
```

### Run
```bash
cd kafka-service
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/kafka-service-1.0-SNAPSHOT.jar
```

## Testing

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### Publish Test Event
```bash
curl -X POST http://localhost:8084/api/kafka/events \
  -H "Content-Type: application/json" \
  -d "{\"eventType\":\"TEST_EVENT\",\"payload\":\"test data\",\"source\":\"test\"}"
```

### H2 Database Console
- URL: http://localhost:8084/h2-console
- JDBC URL: `jdbc:h2:mem:kafkadb`
- Username: `sa`
- Password: (empty)

## Kafka Features

### Producer Features
- **Idempotence**: Enabled for exactly-once semantics
- **Acknowledgments**: Set to "all" for maximum durability
- **Retries**: 3 automatic retries on failure
- **Asynchronous**: Non-blocking with CompletableFuture

### Consumer Features
- **Auto-offset-reset**: Starts from earliest message
- **Group ID**: Supports consumer groups
- **Partition Tracking**: Logs partition and offset information
- **JSON Deserialization**: Automatic conversion from JSON

### Topic Configuration
- **Partitions**: 3 (configurable)
- **Replication**: 1 (adjust for production)
- **Auto-create**: Enabled

## Event Flow

1. **Publishing**: Client sends event via REST API
2. **Persistence**: Event saved to database with PENDING status
3. **Kafka Send**: Event sent to Kafka topic
4. **Callback**: Status updated to SENT with partition/offset
5. **Consumption**: Consumer receives event from Kafka
6. **Processing**: Business logic executed based on event type

## Monitoring

### Actuator Endpoints
- Health: http://localhost:8084/actuator/health
- Metrics: http://localhost:8084/actuator/metrics
- Kafka metrics: http://localhost:8084/actuator/metrics/kafka.*

### Logs
Events are logged with partition and offset information:
```
Event sent successfully to Kafka: USER_CREATED with offset: 42 on partition: 1
Received event from Kafka - Topic: kafka-event-topic, Partition: 1, Offset: 42
```

## Event Types

Supported event types (extend in consumer):
- USER_CREATED
- USER_UPDATED
- USER_DELETED
- ORDER_CREATED
- ORDER_UPDATED
- PAYMENT_PROCESSED

## Integration Example

### Publishing from Another Service

```java
@Autowired
private RestTemplate restTemplate;

public void publishEvent(String eventType, Object data) {
    Map<String, String> event = Map.of(
        "eventType", eventType,
        "payload", objectMapper.writeValueAsString(data),
        "source", "my-service"
    );
    
    restTemplate.postForEntity(
        "http://localhost:8084/api/kafka/events",
        event,
        KafkaEvent.class
    );
}
```

## Performance Tips

1. **Batch Publishing**: Use Kafka's batching capabilities
2. **Compression**: Enable compression for large payloads
3. **Partitioning**: Use appropriate partition keys
4. **Consumer Concurrency**: Adjust `concurrency` in listener
5. **Memory**: Tune JVM heap size for high throughput

## Troubleshooting

### Connection Issues
- Verify Kafka is running: `docker ps | grep kafka`
- Check logs: `docker logs kafka`
- Test connection: `telnet localhost 9092`

### Consumer Not Receiving Messages
- Check consumer group: Verify group ID
- Check offset: May be at end of topic
- Reset offset: Use `auto-offset-reset: earliest`

### Performance Issues
- Increase partitions for parallelism
- Tune batch size and linger settings
- Monitor lag with Kafka tools

## Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

## License

Part of the SNVN microservices project.

