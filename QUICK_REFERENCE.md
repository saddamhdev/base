# Quick Reference - Kafka & RabbitMQ Services

## 🚀 Start Everything

```bash
# 1. Start Kafka
cd D:\module project\base\kafka-service
docker-compose up -d

# 2. Start RabbitMQ  
cd D:\module project\base\rabbitmq-service
docker-compose up -d

# 3. Run Kafka Service (Terminal 1)
cd D:\module project\base\kafka-service
mvn spring-boot:run

# 4. Run RabbitMQ Service (Terminal 2)
cd D:\module project\base\rabbitmq-service
mvn spring-boot:run
```

## 📤 Publish Events

### Kafka
```bash
curl -X POST http://localhost:8084/api/kafka/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"USER_CREATED","payload":"{\"userId\":1}","source":"test"}'
```

### RabbitMQ
```bash
curl -X POST http://localhost:8085/api/rabbitmq/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER_CREATED","payload":"{\"orderId\":123}","source":"test"}'
```

## 🔍 Query Events

```bash
# Get all Kafka events
curl http://localhost:8084/api/kafka/events

# Get all RabbitMQ events
curl http://localhost:8085/api/rabbitmq/events

# Filter by type
curl http://localhost:8084/api/kafka/events/type/USER_CREATED
curl http://localhost:8085/api/rabbitmq/events/type/ORDER_CREATED
```

## 🌐 Management URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Kafka UI | http://localhost:8080 | - |
| RabbitMQ UI | http://localhost:15672 | guest/guest |
| Kafka Health | http://localhost:8084/actuator/health | - |
| RabbitMQ Health | http://localhost:8085/actuator/health | - |
| Kafka H2 | http://localhost:8084/h2-console | sa/(empty) |
| RabbitMQ H2 | http://localhost:8085/h2-console | sa/(empty) |

## 🛑 Stop Everything

```bash
# Stop Spring Boot services
# Press Ctrl+C in each terminal

# Stop Docker containers
cd D:\module project\base\kafka-service
docker-compose down

cd D:\module project\base\rabbitmq-service
docker-compose down
```

## 🔧 Rebuild

```bash
cd D:\module project\base\kafka-service
mvn clean install -DskipTests

cd D:\module project\base\rabbitmq-service
mvn clean install -DskipTests
```

## 📊 Ports

| Service | Port |
|---------|------|
| Kafka Service | 8084 |
| RabbitMQ Service | 8085 |
| Kafka Broker | 9092 |
| RabbitMQ Broker | 5672 |
| Zookeeper | 2181 |
| Kafka UI | 8080 |
| RabbitMQ UI | 15672 |

## 📁 Files

- `EVENT_SERVICES_README.md` - Full comparison guide
- `PROJECT_SUMMARY.md` - Complete project summary
- `kafka-service/README.md` - Kafka documentation
- `rabbitmq-service/README.md` - RabbitMQ documentation

