# RabbitMQ Service

A dedicated Spring Boot microservice for handling event-driven architecture using **RabbitMQ**.

## Features

- **RabbitMQ Event Publishing**: REST API endpoints to publish events to RabbitMQ exchanges
- **RabbitMQ Event Consumption**: Automatic event consumers listening to RabbitMQ queues
- **Event Persistence**: Stores event metadata in H2 database
- **Event Tracking**: Track event status, exchange, routing key, and queue information
- **Dead Letter Queue (DLQ)**: Automatic handling of failed messages
- **Retry Mechanism**: Configurable retry logic with exponential backoff
- **Flexible Routing**: Support for custom routing keys

## Architecture

### Components

1. **Controller**: REST API endpoints for RabbitMQ event operations
2. **Producer**: `RabbitMQEventProducer` - Publishes events to RabbitMQ exchanges
3. **Consumer**: `RabbitMQEventConsumer` - Listens to RabbitMQ queues with DLQ support
4. **Service Layer**: Business logic for event handling
5. **Repository**: JPA repository for event persistence
6. **Models**: RabbitMQEvent entity and EventMessage DTO
7. **Config**: Exchange, queue, and binding configuration with DLQ

## Prerequisites

- Java 21
- Maven 3.x
- RabbitMQ server (default: localhost:5672)

## Running RabbitMQ with Docker

```bash
# Start RabbitMQ with Management UI
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.13-management
```

Access Management UI: http://localhost:15672
- Username: `guest`
- Password: `guest`

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

rabbitmq:
  queue:
    name: rabbitmq-event-queue
  exchange:
    name: rabbitmq-event-exchange
  routing:
    key: rabbitmq-event-routing-key
  dlq:
    queue:
      name: rabbitmq-event-dlq
    exchange:
      name: rabbitmq-event-dlq-exchange

server:
  port: 8085
```

## API Endpoints

### Publish Event to RabbitMQ

```bash
POST http://localhost:8085/api/rabbitmq/events
Content-Type: application/json

{
  "eventType": "ORDER_CREATED",
  "payload": "{\"orderId\": 123, \"amount\": 99.99}",
  "source": "order-service"
}
```

### Publish with Custom Routing Key

```bash
POST http://localhost:8085/api/rabbitmq/events/custom-routing
Content-Type: application/json

{
  "eventType": "PRIORITY_ORDER",
  "payload": "{\"orderId\": 456}",
  "source": "order-service",
  "routingKey": "order.priority.high"
}
```

### Query Events

- `GET /api/rabbitmq/events` - Get all events
- `GET /api/rabbitmq/events/{id}` - Get event by ID
- `GET /api/rabbitmq/events/type/{eventType}` - Get events by type
- `GET /api/rabbitmq/events/source/{source}` - Get events by source
- `GET /api/rabbitmq/events/status/{status}` - Get events by status
- `GET /api/rabbitmq/events/exchange/{exchange}` - Get events by exchange
- `GET /api/rabbitmq/events/queue/{queue}` - Get events by queue
- `GET /api/rabbitmq/events/routing-key/{routingKey}` - Get events by routing key
- `GET /api/rabbitmq/events/period?start={start}&end={end}` - Get events by time period
- `DELETE /api/rabbitmq/events/{id}` - Delete event by ID

## Building and Running

### Build
```bash
cd D:\module project\base
mvn clean install
```

### Run
```bash
cd rabbitmq-service
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/rabbitmq-service-1.0-SNAPSHOT.jar
```

## Testing

### Health Check
```bash
curl http://localhost:8085/actuator/health
```

### Publish Test Event
```bash
curl -X POST http://localhost:8085/api/rabbitmq/events \
  -H "Content-Type: application/json" \
  -d "{\"eventType\":\"TEST_EVENT\",\"payload\":\"test data\",\"source\":\"test\"}"
```

### H2 Database Console
- URL: http://localhost:8085/h2-console
- JDBC URL: `jdbc:h2:mem:rabbitmqdb`
- Username: `sa`
- Password: (empty)

## RabbitMQ Features

### Exchange Configuration
- **Type**: Topic Exchange
- **Durable**: Yes
- **Auto-delete**: No

### Queue Configuration
- **Durable**: Yes
- **Dead Letter Exchange**: Configured
- **Prefetch Count**: 1 (prevents overwhelming consumers)

### Dead Letter Queue (DLQ)
- Automatically receives failed messages
- Separate consumer logs DLQ messages
- Tracks original exchange and routing key

### Retry Mechanism
- **Enabled**: Yes
- **Initial Interval**: 2 seconds
- **Max Attempts**: 3
- **Multiplier**: 1.5 (exponential backoff)

## Event Flow

1. **Publishing**: Client sends event via REST API
2. **Persistence**: Event saved to database with metadata
3. **RabbitMQ Send**: Event sent to exchange with routing key
4. **Routing**: Exchange routes to queue based on binding
5. **Consumption**: Consumer receives and processes event
6. **Error Handling**: Failed messages sent to DLQ after retries

## Monitoring

### RabbitMQ Management UI
Navigate to http://localhost:15672

**Queues Tab**: View message rates, consumers, and queue depth
**Exchanges Tab**: View bindings and message routing
**Connections Tab**: Monitor active connections

### Actuator Endpoints
- Health: http://localhost:8085/actuator/health
- Metrics: http://localhost:8085/actuator/metrics

### Logs
Events are logged with exchange and routing key:
```
Event sent successfully to RabbitMQ: ORDER_CREATED via exchange: rabbitmq-event-exchange
Received event from RabbitMQ - Exchange: rabbitmq-event-exchange, Routing Key: rabbitmq-event-routing-key
```

## Event Types

Supported event types (extend in consumer):
- ORDER_CREATED
- ORDER_UPDATED
- ORDER_CANCELLED
- PAYMENT_PROCESSED
- INVENTORY_UPDATED
- NOTIFICATION_SENT

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
        "http://localhost:8085/api/rabbitmq/events",
        event,
        RabbitMQEvent.class
    );
}
```

## Advanced Routing Patterns

### Topic Exchange Patterns
```
order.*           - All order events
order.created     - Only order created
*.high.priority   - All high priority events
#.notification    - All notification events
```

### Custom Routing Example
```bash
curl -X POST http://localhost:8085/api/rabbitmq/events/custom-routing \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "ORDER_CREATED",
    "payload": "{\"orderId\": 789}",
    "source": "order-service",
    "routingKey": "order.created.high.priority"
  }'
```

## Performance Tips

1. **Prefetch Count**: Tune based on processing time
2. **Connection Pooling**: Use connection factory pooling
3. **Message Persistence**: Consider non-durable messages for high throughput
4. **Batch Processing**: Process messages in batches
5. **Lazy Queues**: Use for large queues

## Troubleshooting

### Connection Issues
- Verify RabbitMQ is running: `docker ps | grep rabbitmq`
- Check logs: `docker logs rabbitmq`
- Test connection: `telnet localhost 5672`

### Messages Not Being Consumed
- Check queue bindings in Management UI
- Verify routing key matches pattern
- Check for consumer errors in logs

### Dead Letter Queue Filling Up
- Review DLQ messages in Management UI
- Check consumer error logs
- Fix underlying processing issues

## Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

## Comparison with Kafka Service

| Feature | RabbitMQ Service | Kafka Service |
|---------|------------------|---------------|
| Port | 8085 | 8084 |
| Message Broker | RabbitMQ | Apache Kafka |
| Routing | Exchange/Routing Key | Topic/Partition |
| DLQ | Built-in | Manual |
| Ordering | Queue-based | Partition-based |
| Best For | Task queues, RPC | Event streaming, logs |

## License

Part of the SNVN microservices project.

