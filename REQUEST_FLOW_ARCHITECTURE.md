# Request Flow Architecture

## Overview

This document describes the request flow architecture for the microservices system.

```
Client
   ↓
Gateway (Port 8080)
   ↓
main-service (Aggregator) (Port 3081)
   ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓
user-service (8089)
auth-service (8083)
account-service (8082)
transaction-service (8088)
notification-service (8087)
audit-service (8086)
kafka-service (8084)
rabbitmq-service (8085)
```

---

## 🔄 Request Flow Details

### 1️⃣ Client Layer
- **Description**: External clients (Web, Mobile, Desktop applications)
- **Communication**: HTTP/HTTPS requests
- **Entry Point**: Gateway Service at `http://localhost:8080`

### 2️⃣ Gateway Layer (API Gateway)
- **Service**: gateway-service
- **Port**: 8080
- **Technology**: Spring Cloud Gateway
- **Responsibilities**:
  - Single entry point for all client requests
  - Request routing to appropriate services
  - Load balancing
  - Authentication/Authorization (if configured)
  - Rate limiting (if configured)
  - Request/Response transformation

#### Gateway Route Configuration
Located in: `gateway-service/src/main/java/snvn/gatewayservice/config/GatewayConfiguration.java`

```java
Routes configured:
- /api/users/** → routes to http://localhost:8081 (but should route to main-service on port 3081)
- /config/** → routes to http://localhost:8888 (config-service)
- /** → routes to http://localhost:3081 (main-service - catch-all)
```

**⚠️ Note**: Gateway configuration needs update for proper main-service routing (port 3081).

### 3️⃣ Aggregator Layer (main-service)
- **Service**: main-service
- **Port**: 3081 (dev environment) / 8081 (production)
- **Pattern**: API Gateway Aggregation / Backend for Frontend (BFF)
- **Responsibilities**:
  - Aggregate data from multiple downstream services
  - Orchestrate business logic across services
  - Transform and combine responses
  - Handle complex queries requiring multiple service calls
  - Reduce chattiness between client and backend

#### main-service Configuration
- **Development Port**: 3081 (application-dev.yml)
- **Default Port**: 8081 (application.yml)
- **Application Name**: main-service
- **Database**: H2 in-memory (testdb)
- **Context Path**: /api

**Current State**: ⚠️ No controllers or services implemented yet
- Empty controller folder
- Empty service folder
- Empty repository folder

### 4️⃣ Downstream Microservices Layer

#### a) user-service
- **Port**: 8089
- **Database**: H2 in-memory (userdb)
- **Responsibilities**:
  - User profile management
  - User CRUD operations
  - User data storage
- **Message Broker**: Kafka (localhost:9092)
  - Consumer Group: user-event-group
- **H2 Console**: http://localhost:8089/h2-console

#### b) auth-service
- **Port**: 8083
- **Database**: H2 in-memory (authdb)
- **Responsibilities**:
  - Authentication
  - Authorization
  - JWT token generation and validation
  - User login/logout
- **JWT Configuration**:
  - Token expiration: 24 hours (86400000 ms)
  - Refresh token: 7 days (604800000 ms)
- **Message Broker**: Kafka (localhost:9092)
  - Consumer Group: auth-event-group
- **H2 Console**: http://localhost:8083/h2-console

#### c) account-service
- **Port**: 8082
- **Database**: H2 in-memory (accountdb)
- **Responsibilities**:
  - Account management
  - Account balance operations
  - Transaction handling
- **Message Broker**: Kafka (localhost:9092)
  - Consumer Group: account-event-group
- **H2 Console**: http://localhost:8082/h2-console

#### d) transaction-service
- **Port**: 8088
- **Database**: H2 in-memory (transactiondb)
- **Responsibilities**:
  - Transaction processing
  - Transaction history
  - Debit/Credit operations
  - Transaction validation
- **Message Broker**: Kafka (localhost:9092)
  - Publishes transaction events
- **H2 Console**: http://localhost:8088/h2-console

#### e) notification-service
- **Port**: 8087
- **Database**: H2 in-memory (notificationdb)
- **Responsibilities**:
  - Send email notifications
  - Send SMS notifications
  - Push notifications
  - Alert management
- **Message Broker**: RabbitMQ (localhost:5672) & Kafka (localhost:9092)
  - Consumes events from Kafka
  - Queues notifications in RabbitMQ

#### f) audit-service
- **Port**: 8086
- **Database**: H2 in-memory (auditdb)
- **Responsibilities**:
  - Audit logging
  - Track all system events
  - Compliance and monitoring
  - Event replay capability
- **Message Broker**: Kafka (localhost:9092)
  - Consumes all events for audit trail

#### g) kafka-service
- **Port**: 8084
- **Database**: H2 in-memory
- **Responsibilities**:
  - Kafka event management
  - Event publishing
  - Event consumption
  - Topic management
- **Kafka Broker**: localhost:9092
- **Kafka UI**: http://localhost:8080 (if using kafka-ui docker)

#### h) rabbitmq-service
- **Port**: 8085
- **Database**: H2 in-memory
- **Responsibilities**:
  - RabbitMQ message management
  - Queue management
  - Message publishing
  - Message consumption
- **RabbitMQ Broker**: localhost:5672
- **RabbitMQ Management UI**: http://localhost:15672 (guest/guest)

---

## 📊 Complete Service Port Mapping

| Service | Port | Type | Database |
|---------|------|------|----------|
| gateway-service | 8080 | Gateway | - |
| config-service | 8888 | Config | - |
| main-service | 3081 (dev) / 8081 (prod) | Aggregator | H2 (testdb) |
| account-service | 8082 | Microservice | H2 (accountdb) |
| auth-service | 8083 | Microservice | H2 (authdb) |
| kafka-service | 8084 | Event Broker Service | H2 |
| rabbitmq-service | 8085 | Event Broker Service | H2 |
| audit-service | 8086 | Microservice | - |
| notification-service | 8087 | Microservice | - |
| transaction-service | 8088 | Microservice | - |
| user-service | 8089 | Microservice | H2 (userdb) |
| mcp-server | 8090 | MCP Server | - |

---

## 🔐 Example Request Flow Scenarios

### Scenario 1: User Login
```
1. Client → POST http://localhost:8080/api/auth/login
2. Gateway → Routes to main-service (http://localhost:3081/api/auth/login)
3. main-service → Calls auth-service (http://localhost:8083/auth/login)
4. auth-service → Validates credentials, generates JWT token
5. auth-service → Returns JWT to main-service
6. main-service → May enrich response with additional data
7. main-service → Returns to Gateway
8. Gateway → Returns to Client
```

### Scenario 2: Get User Profile with Account Details
```
1. Client → GET http://localhost:8080/api/user/profile/{userId}
2. Gateway → Routes to main-service (http://localhost:3081/api/user/profile/{userId})
3. main-service → Makes parallel calls:
   a) user-service (http://localhost:8089/users/{userId})
   b) account-service (http://localhost:8082/accounts/user/{userId})
4. main-service → Aggregates responses from both services
5. main-service → Returns combined data to Gateway
6. Gateway → Returns to Client
```

### Scenario 3: Create Account (with Authentication)
```
1. Client → POST http://localhost:8080/api/accounts (with JWT token)
2. Gateway → Validates token (optional), routes to main-service
3. main-service → Validates JWT with auth-service
4. main-service → Calls user-service to verify user exists
5. main-service → Calls account-service to create account
6. account-service → Creates account and publishes event to Kafka
7. main-service → Returns success response
8. Gateway → Returns to Client
```

### Scenario 4: Money Transfer (Complex Multi-Service Transaction)
```
1. Client → POST http://localhost:8080/api/transactions/transfer
   Body: { fromAccount: 1, toAccount: 2, amount: 1000 }
   Headers: Authorization: Bearer <JWT>

2. Gateway → Routes to main-service (http://localhost:3081/api/transactions/transfer)

3. main-service → Orchestrates the following steps:

   Step 3a: Validate Authentication
   - Calls auth-service (8083) → Validates JWT token
   - Returns: { valid: true, userId: 123 }

   Step 3b: Get User Information
   - Calls user-service (8089) → Get user details
   - Returns: { id: 123, name: "John Doe", email: "john@example.com" }

   Step 3c: Validate Accounts
   - Calls account-service (8082) → Check account ownership and balance
   - Verifies fromAccount belongs to user
   - Verifies sufficient balance
   - Returns: { fromAccount: {...}, toAccount: {...}, valid: true }

   Step 3d: Process Transaction
   - Calls transaction-service (8088) → Create transaction record
   - Returns: { transactionId: "TXN-12345", status: "PROCESSING" }

   Step 3e: Update Account Balances
   - Calls account-service (8082) → Debit from account 1, Credit to account 2
   - Returns: { fromBalance: 4000, toBalance: 11000 }

   Step 3f: Publish Events
   - Calls kafka-service (8084) → Publish transaction event
   - Event consumed by:
     * audit-service (8086) → Logs transaction for compliance
     * notification-service (8087) → Sends email/SMS to both users
   - Calls rabbitmq-service (8085) → Queue notification for batch processing

   Step 3g: Log Audit Trail
   - Calls audit-service (8086) → Create audit record
   - Returns: { auditId: "AUD-67890", logged: true }

4. main-service → Aggregates all responses and returns:
   {
     "transactionId": "TXN-12345",
     "status": "SUCCESS",
     "fromAccount": { id: 1, newBalance: 4000 },
     "toAccount": { id: 2, newBalance: 11000 },
     "amount": 1000,
     "timestamp": "2026-02-20T10:30:00Z",
     "auditId": "AUD-67890",
     "notificationSent": true
   }

5. Gateway → Returns to Client

Async Processing (happening in background):
- notification-service sends email: "You sent $1000 to account #2"
- notification-service sends email: "You received $1000 from account #1"
- audit-service indexes transaction for reporting
- rabbitmq-service queues for daily summary email
```

---

## 🏗️ Architecture Patterns Used

### 1. API Gateway Pattern
- **Implementation**: Spring Cloud Gateway (gateway-service)
- **Benefits**:
  - Single entry point
  - Centralized authentication/authorization
  - Request routing
  - Protocol translation

### 2. Aggregator Pattern (Backend for Frontend)
- **Implementation**: main-service
- **Benefits**:
  - Reduces client-server round trips
  - Simplified client logic
  - Server-side composition
  - Better performance

### 3. Database per Service Pattern
- Each microservice has its own database (H2)
- **user-service**: userdb
- **auth-service**: authdb
- **account-service**: accountdb
- **main-service**: testdb

### 4. Event-Driven Architecture
- **Message Broker**: Apache Kafka (localhost:9092)
- Services publish and consume events asynchronously
- Event groups:
  - user-event-group
  - auth-event-group
  - account-event-group

---

## 🔧 Configuration Files Reference

### Gateway Service
- `gateway-service/src/main/resources/application.yml`
- `gateway-service-env-properties/src/main/resources/application-dev.yml`

### Main Service (Aggregator)
- `main/src/main/resources/application.yml` (port 8081)
- `main-env-properties/src/main/resources/application-dev.yml` (port 3081)

### Downstream Services
- `user-service/src/main/resources/application.yml`
- `auth-service/src/main/resources/application.yml`
- `account-service/src/main/resources/application.yml`

---

## ⚠️ Current Issues & Recommendations

### 1. Gateway Route Configuration Mismatch
**Issue**: Gateway routes `/api/users/**` to port 8081, but should route to main-service on port 3081 (dev) or 8081 (prod).

**Recommendation**: Update `GatewayConfiguration.java` to use correct ports.

### 2. main-service Implementation
**Status**: Empty controllers, services, and repositories

**Recommendation**: Implement aggregation logic in main-service:
- Create `UserAggregatorController`
- Create `AccountAggregatorController`
- Create `AuthAggregatorController`
- Add RestTemplate or WebClient for service-to-service communication
- Implement circuit breaker pattern (Resilience4j)

### 3. Service Discovery
**Current**: Using hardcoded URLs (localhost:PORT)

**Recommendation**: Consider implementing Eureka or Consul for service discovery.

### 4. Inter-Service Communication
**Missing**: 
- RestTemplate or WebClient configuration
- Circuit breakers
- Retry logic
- Fallback mechanisms

**Recommendation**: Add Spring Cloud dependencies for resilient communication.

---

## 🚀 Next Steps to Implement Request Flow

### Step 1: Update Gateway Configuration
```java
// Update GatewayConfiguration.java
.route("main-service", r -> r
    .path("/api/**")
    .uri("http://localhost:3081"))  // Use 3081 for dev
```

### Step 2: Implement main-service Aggregator
Create controllers that aggregate from downstream services:
- `MainController` - Health checks, info
- `UserAggregatorController` - User profile with accounts
- `AccountAggregatorController` - Account operations
- `AuthAggregatorController` - Login/logout orchestration

### Step 3: Add Service Communication
Add to main-service pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Step 4: Configure WebClient Beans
```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient userServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8089")
            .build();
    }
    
    @Bean
    public WebClient authServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8083")
            .build();
    }
    
    @Bean
    public WebClient accountServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8082")
            .build();
    }
}
```

### Step 5: Test the Flow
```bash
# 1. Start all services
# 2. Test via Gateway
curl http://localhost:8080/api/test

# Should route to main-service on port 3081
# main-service can then aggregate from other services
```

---

## 📚 Related Documentation
- [PORT_CONFIGURATION_SUMMARY.md](./PORT_CONFIGURATION_SUMMARY.md) - Port allocation
- [PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md) - Overall project structure
- [EVENT_SERVICES_README.md](./EVENT_SERVICES_README.md) - Kafka & RabbitMQ
- [BUILD_GUIDE.md](./BUILD_GUIDE.md) - Build instructions

---

## 🔗 Technology Stack

- **Framework**: Spring Boot 4.0.0
- **Language**: Java 21
- **Gateway**: Spring Cloud Gateway
- **Database**: H2 (in-memory)
- **Message Broker**: Apache Kafka
- **Build Tool**: Maven
- **Architecture**: Microservices with API Gateway and Aggregator patterns




