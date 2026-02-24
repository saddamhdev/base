# Architecture Summary - All Services

## Complete System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                            │
│          (Web Browser, Mobile App, Desktop App, APIs)                   │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 │ HTTPS/HTTP
                                 ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         GATEWAY LAYER                                    │
│                      gateway-service (8080)                              │
│                     Spring Cloud Gateway                                 │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                    ┌────────────┼────────────┐
                    ↓            ↓            ↓
            ┌───────────┐  ┌──────────┐  Other routes
            │   main    │  │  config  │
            │  service  │  │  service │
            │  (3081)   │  │  (8888)  │
            └─────┬─────┘  └──────────┘
                  │
┌─────────────────┴──────────────────────────────────────────────────────┐
│                     AGGREGATOR LAYER                                    │
│                    main-service (3081)                                  │
│                Backend for Frontend (BFF)                               │
│  • Aggregates data from multiple services                              │
│  • Orchestrates complex workflows                                      │
│  • Reduces client-server chattiness                                    │
└─┬────┬────┬────┬────┬────┬────┬────┬────────────────────────────────────┘
  │    │    │    │    │    │    │    │
  │    │    │    │    │    │    │    │
  ↓    ↓    ↓    ↓    ↓    ↓    ↓    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    MICROSERVICES LAYER                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │ user-service │  │ auth-service │  │account-service│                 │
│  │   (8089)     │  │   (8083)     │  │   (8082)     │                 │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤                 │
│  │• User CRUD   │  │• Login       │  │• Accounts    │                 │
│  │• Profiles    │  │• JWT Tokens  │  │• Balances    │                 │
│  │• User Data   │  │• Auth        │  │• Ownership   │                 │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                 │
│         │                  │                  │                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │ transaction- │  │notification- │  │ audit-service│                 │
│  │  service     │  │  service     │  │   (8086)     │                 │
│  │   (8088)     │  │   (8087)     │  ├──────────────┤                 │
│  ├──────────────┤  ├──────────────┤  │• Audit Logs  │                 │
│  │• Transfers   │  │• Email/SMS   │  │• Compliance  │                 │
│  │• History     │  │• Push Notif  │  │• Monitoring  │                 │
│  │• Debit/Credit│  │• Alerts      │  │• Event Track │                 │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                 │
│         │                  │                  │                         │
│         │                  │                  │                         │
└─────────┼──────────────────┼──────────────────┼─────────────────────────┘
          │                  │                  │
          ↓                  ↓                  ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                  EVENT-DRIVEN / MESSAGING LAYER                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────┐  ┌─────────────────────────────────┐ │
│  │    kafka-service (8084)     │  │  rabbitmq-service (8085)        │ │
│  ├─────────────────────────────┤  ├─────────────────────────────────┤ │
│  │ • Event Publishing          │  │ • Message Queuing               │ │
│  │ • Event Consumption         │  │ • Async Processing              │ │
│  │ • Topic Management          │  │ • Queue Management              │ │
│  │                             │  │                                 │ │
│  │ Kafka Broker: 9092          │  │ RabbitMQ Broker: 5672          │ │
│  │ Kafka UI: 8080              │  │ Management UI: 15672            │ │
│  └─────────────────────────────┘  └─────────────────────────────────┘ │
│                                                                         │
│  Event Topics:                    Queues:                              │
│  • user-events                    • notification-queue                 │
│  • auth-events                    • email-queue                        │
│  • account-events                 • sms-queue                          │
│  • transaction-events             • batch-processing-queue             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
          │                                          │
          ↓                                          ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                       DATA PERSISTENCE LAYER                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Each microservice has its own H2 in-memory database:                  │
│                                                                         │
│  • user-service       → H2: userdb                                     │
│  • auth-service       → H2: authdb                                     │
│  • account-service    → H2: accountdb                                  │
│  • transaction-service→ H2: transactiondb                              │
│  • notification-service→ H2: notificationdb                            │
│  • audit-service      → H2: auditdb                                    │
│  • kafka-service      → H2: kafkadb                                    │
│  • rabbitmq-service   → H2: rabbitmqdb                                 │
│  • main-service       → H2: testdb                                     │
│                                                                         │
│  All H2 consoles accessible at: http://localhost:[PORT]/h2-console     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Service Communication Patterns

### Pattern 1: Gateway → Aggregator → Microservices
```
Client Request
     ↓
gateway-service (8080)
     ↓
main-service (3081)
     ↓ ↓ ↓
[user-service] [auth-service] [account-service]
```

### Pattern 2: Event-Driven Communication
```
Service Action (e.g., Create Transaction)
     ↓
transaction-service (8088)
     ↓
kafka-service (8084) publishes event
     ↓ ↓ ↓
[audit-service] [notification-service] [rabbitmq-service]
     ↓                    ↓                    ↓
 Log to DB          Send Email/SMS      Queue for batch
```

### Pattern 3: Orchestrated Workflow
```
main-service orchestrates:
     │
     ├─→ Step 1: auth-service → validate token
     │
     ├─→ Step 2: account-service → check balance
     │
     ├─→ Step 3: transaction-service → create transaction
     │
     ├─→ Step 4: account-service → update balances
     │
     ├─→ Step 5: kafka-service → publish event
     │
     └─→ Step 6: audit-service → log transaction
```

---

## Port Allocation

| Service | Port | Type | Entry Point |
|---------|------|------|-------------|
| gateway-service | 8080 | API Gateway | ✅ Public |
| main-service | 3081 (dev) / 8081 (prod) | Aggregator | via Gateway |
| account-service | 8082 | Microservice | Internal |
| auth-service | 8083 | Microservice | Internal |
| kafka-service | 8084 | Event Service | Internal |
| rabbitmq-service | 8085 | Queue Service | Internal |
| audit-service | 8086 | Microservice | Internal |
| notification-service | 8087 | Microservice | Internal |
| transaction-service | 8088 | Microservice | Internal |
| user-service | 8089 | Microservice | Internal |
| mcp-server | 8090 | MCP Server | Internal |
| config-service | 8888 | Config Server | Internal |

**External Ports**:
- Kafka Broker: 9092
- RabbitMQ Broker: 5672
- RabbitMQ Management: 15672

---

## Technology Stack

### Framework & Language
- **Java**: 21
- **Spring Boot**: 4.0.0
- **Spring Cloud**: 2023.0.3
- **Build Tool**: Maven

### API Gateway
- **Spring Cloud Gateway**: Request routing, load balancing

### Databases
- **H2**: In-memory databases (development)
- **Database per Service**: Each microservice has isolated database

### Message Brokers
- **Apache Kafka**: Event streaming (Port 9092)
- **RabbitMQ**: Message queuing (Port 5672)

### Communication
- **REST/HTTP**: Synchronous communication
- **WebClient**: Reactive HTTP client
- **Kafka**: Asynchronous event-driven
- **RabbitMQ**: Asynchronous message queuing

### Future Enhancements
- **Circuit Breaker**: Resilience4j (planned)
- **Service Discovery**: Eureka/Consul (planned)
- **Distributed Tracing**: Sleuth + Zipkin (planned)
- **Monitoring**: Prometheus + Grafana (planned)

---

## Service Categories

### 1. Infrastructure Services
- **gateway-service**: Entry point, routing
- **config-service**: Centralized configuration
- **mcp-server**: MCP protocol server

### 2. Core Business Services
- **user-service**: User management
- **auth-service**: Authentication & authorization
- **account-service**: Account management
- **transaction-service**: Transaction processing

### 3. Support Services
- **notification-service**: Notifications (email, SMS, push)
- **audit-service**: Audit logging & compliance

### 4. Event & Message Services
- **kafka-service**: Event streaming
- **rabbitmq-service**: Message queuing

### 5. Aggregation Service
- **main-service**: BFF, orchestration, aggregation

---

## Data Flow Examples

### Example 1: User Registration
```
1. Client → Gateway (8080)
2. Gateway → main-service (3081)
3. main-service orchestrates:
   a. user-service (8089) → Create user
   b. auth-service (8083) → Create credentials
   c. account-service (8082) → Create default account
   d. kafka-service (8084) → Publish user-created event
   e. notification-service (8087) → Send welcome email (async)
   f. audit-service (8086) → Log registration (async)
4. main-service → Gateway → Client (success response)
```

### Example 2: User Login
```
1. Client → Gateway (8080)
2. Gateway → main-service (3081)
3. main-service:
   a. auth-service (8083) → Validate credentials
   b. auth-service (8083) → Generate JWT token
   c. audit-service (8086) → Log login attempt
4. main-service → Gateway → Client (JWT token)
```

### Example 3: Get Dashboard
```
1. Client → Gateway (8080) [with JWT]
2. Gateway → main-service (3081)
3. main-service parallel calls:
   a. auth-service (8083) → Validate JWT
   b. user-service (8089) → Get user profile
   c. account-service (8082) → Get accounts & balances
   d. transaction-service (8088) → Get recent transactions
4. main-service → Aggregate all data
5. main-service → Gateway → Client (dashboard data)
```

### Example 4: Money Transfer
```
1. Client → Gateway (8080) [with JWT]
2. Gateway → main-service (3081)
3. main-service orchestrates:
   a. auth-service (8083) → Validate JWT ✓
   b. user-service (8089) → Get user details ✓
   c. account-service (8082) → Validate accounts ✓
   d. account-service (8082) → Check balance ✓
   e. transaction-service (8088) → Create transaction ✓
   f. account-service (8082) → Update balances ✓
   g. kafka-service (8084) → Publish transaction-event ✓
   
   Async events:
   h. audit-service (8086) → Log transaction
   i. notification-service (8087) → Send confirmations
   j. rabbitmq-service (8085) → Queue for reports

4. main-service → Gateway → Client (transaction result)
```

---

## Key Architectural Decisions

### 1. API Gateway Pattern
**Decision**: Use Spring Cloud Gateway as single entry point
**Benefits**:
- Centralized routing
- Security enforcement
- Rate limiting
- Request/response transformation

### 2. Backend for Frontend (BFF)
**Decision**: main-service acts as aggregator
**Benefits**:
- Reduced client complexity
- Fewer network calls
- Server-side composition
- Optimized responses for clients

### 3. Database per Service
**Decision**: Each microservice has its own database
**Benefits**:
- Service independence
- Technology flexibility
- Easier scaling
- Fault isolation

### 4. Event-Driven Architecture
**Decision**: Use Kafka for event streaming
**Benefits**:
- Loose coupling
- Asynchronous processing
- Event sourcing capability
- Scalability

### 5. Message Queue Pattern
**Decision**: Use RabbitMQ for async tasks
**Benefits**:
- Reliable delivery
- Load leveling
- Batch processing
- Retry mechanisms

---

## Development Workflow

### Starting All Services

```powershell
# 1. Start message brokers
cd "D:\module project\base\kafka-service"
docker-compose up -d

cd "D:\module project\base\rabbitmq-service"
docker-compose up -d

# 2. Start config service (if using)
cd "D:\module project\base\config-service"
mvn spring-boot:run

# 3. Start gateway service
cd "D:\module project\base\gateway-service"
mvn spring-boot:run

# 4. Start main service (aggregator)
cd "D:\module project\base\main"
mvn spring-boot:run

# 5. Start microservices (can run in parallel)
# Open separate terminals for each:
cd "D:\module project\base\user-service"
mvn spring-boot:run

cd "D:\module project\base\auth-service"
mvn spring-boot:run

cd "D:\module project\base\account-service"
mvn spring-boot:run

cd "D:\module project\base\transaction-service"
mvn spring-boot:run

cd "D:\module project\base\notification-service"
mvn spring-boot:run

cd "D:\module project\base\audit-service"
mvn spring-boot:run

cd "D:\module project\base\kafka-service"
mvn spring-boot:run

cd "D:\module project\base\rabbitmq-service"
mvn spring-boot:run
```

### Build All Services

```powershell
# From base directory
cd "D:\module project\base"
mvn clean install -DskipTests
```

---

## Testing the Architecture

### Health Checks
```bash
# Gateway
curl http://localhost:8080/actuator/health

# Main Service
curl http://localhost:3081/actuator/health

# All Microservices
curl http://localhost:8089/actuator/health  # user
curl http://localhost:8083/actuator/health  # auth
curl http://localhost:8082/actuator/health  # account
curl http://localhost:8088/actuator/health  # transaction
curl http://localhost:8087/actuator/health  # notification
curl http://localhost:8086/actuator/health  # audit
```

### Test Flow Through Gateway
```bash
# All requests go through gateway (8080)
curl http://localhost:8080/api/users
curl http://localhost:8080/api/auth/login
curl http://localhost:8080/api/accounts
curl http://localhost:8080/api/transactions
```

---

## Monitoring & Management URLs

| Service | Health Check | H2 Console | Management UI |
|---------|-------------|------------|---------------|
| gateway | :8080/actuator/health | - | - |
| main | :3081/actuator/health | :3081/h2-console | - |
| user | :8089/actuator/health | :8089/h2-console | - |
| auth | :8083/actuator/health | :8083/h2-console | - |
| account | :8082/actuator/health | :8082/h2-console | - |
| transaction | :8088/actuator/health | :8088/h2-console | - |
| notification | :8087/actuator/health | :8087/h2-console | - |
| audit | :8086/actuator/health | :8086/h2-console | - |
| kafka | :8084/actuator/health | :8084/h2-console | :8080 (Kafka UI) |
| rabbitmq | :8085/actuator/health | :8085/h2-console | :15672 |

---

## Summary

**Total Services**: 12
- 1 Gateway
- 1 Aggregator (BFF)
- 6 Core Microservices
- 2 Event/Message Services
- 2 Infrastructure Services

**Communication**:
- Synchronous: HTTP/REST via WebClient
- Asynchronous: Kafka events + RabbitMQ queues

**Architecture Patterns**:
- API Gateway
- Backend for Frontend (BFF)
- Microservices
- Event-Driven Architecture
- Database per Service
- Circuit Breaker (planned)
- Service Discovery (planned)

**Development Status**:
- ✅ All services created
- ✅ Ports configured
- ⚠️ main-service needs implementation
- ⚠️ Gateway routes need update
- ⚠️ Service-to-service communication needs setup

---

## Related Documentation
- [REQUEST_FLOW_ARCHITECTURE.md](./REQUEST_FLOW_ARCHITECTURE.md) - Detailed architecture
- [REQUEST_FLOW_DIAGRAM.md](./REQUEST_FLOW_DIAGRAM.md) - Visual flow diagrams
- [MAIN_SERVICE_INTEGRATION.md](./MAIN_SERVICE_INTEGRATION.md) - Integration guide
- [PORT_CONFIGURATION_SUMMARY.md](./PORT_CONFIGURATION_SUMMARY.md) - Port details
- [PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md) - Project overview

