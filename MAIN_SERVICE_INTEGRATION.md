# Complete Service Architecture - main-service Integration

## All Services Connected to main-service

This document provides a comprehensive view of all microservices that main-service (aggregator) can connect to.

---

## 📊 Service Integration Map

```
                        ┌─────────────────────────────────┐
                        │       main-service              │
                        │    (Aggregator/BFF)             │
                        │    Port: 3081 (dev)/8081 (prod) │
                        └───────────────┬─────────────────┘
                                        │
                ┌───────────────────────┼───────────────────────┐
                │                       │                       │
    ┌───────────┴──────────┐ ┌─────────┴─────────┐ ┌──────────┴─────────┐
    │                      │ │                   │ │                    │
    ↓                      ↓ ↓                   ↓ ↓                    ↓
┌──────────┐         ┌──────────┐         ┌──────────┐         ┌──────────┐
│  user-   │         │  auth-   │         │ account- │         │trans-    │
│ service  │         │ service  │         │ service  │         │action-   │
│  (8089)  │         │  (8083)  │         │  (8082)  │         │service   │
│          │         │          │         │          │         │  (8088)  │
└──────────┘         └──────────┘         └──────────┘         └──────────┘
    ↓                      ↓                   ↓                     ↓
┌──────────┐         ┌──────────┐         ┌──────────┐         ┌──────────┐
│notifi-   │         │  audit-  │         │  kafka-  │         │rabbitmq- │
│cation-   │         │ service  │         │ service  │         │ service  │
│service   │         │  (8086)  │         │  (8084)  │         │  (8085)  │
│  (8087)  │         │          │         │          │         │          │
└──────────┘         └──────────┘         └──────────┘         └──────────┘
```

---

## 🎯 Service Roles and Responsibilities

### Core Business Services

#### 1. user-service (Port 8089)
**Role**: User Management
- User CRUD operations
- User profile management
- User authentication data
- User preferences

**main-service Integration**:
- Get user details
- Create/Update user profiles
- Validate user existence
- Retrieve user preferences

**Technologies**:
- Database: H2 (userdb)
- Message Broker: Kafka (user-events)
- H2 Console: http://localhost:8089/h2-console

---

#### 2. auth-service (Port 8083)
**Role**: Authentication & Authorization
- User login/logout
- JWT token generation
- Token validation
- Session management
- Password management

**main-service Integration**:
- Validate JWT tokens
- Authenticate users
- Generate access tokens
- Refresh tokens
- Authorize operations

**Technologies**:
- Database: H2 (authdb)
- Message Broker: Kafka (auth-events)
- JWT: 24-hour expiry, 7-day refresh
- H2 Console: http://localhost:8083/h2-console

---

#### 3. account-service (Port 8082)
**Role**: Account Management
- Account CRUD operations
- Balance management
- Account ownership
- Account types (checking, savings, etc.)

**main-service Integration**:
- Get user accounts
- Check account balances
- Validate account ownership
- Update balances
- Transfer funds

**Technologies**:
- Database: H2 (accountdb)
- Message Broker: Kafka (account-events)
- H2 Console: http://localhost:8082/h2-console

---

#### 4. transaction-service (Port 8088)
**Role**: Transaction Processing
- Transaction creation
- Debit/Credit operations
- Transaction history
- Transaction validation
- Transaction status tracking

**main-service Integration**:
- Create transactions
- Get transaction history
- Validate transactions
- Process transfers
- Get transaction details

**Technologies**:
- Database: H2 (transactiondb)
- Message Broker: Kafka (transaction-events)
- H2 Console: http://localhost:8088/h2-console

---

### Support Services

#### 5. notification-service (Port 8087)
**Role**: Notification Management
- Email notifications
- SMS notifications
- Push notifications
- Alert management
- Notification templates

**main-service Integration**:
- Send transaction alerts
- Send welcome emails
- Send password reset emails
- Send account notifications
- Send promotional messages

**Technologies**:
- Database: H2 (notificationdb)
- Message Brokers: Kafka & RabbitMQ
- Consumes events from other services

---

#### 6. audit-service (Port 8086)
**Role**: Audit & Compliance
- Audit logging
- Event tracking
- Compliance monitoring
- Event replay
- System monitoring

**main-service Integration**:
- Log all operations
- Track user actions
- Record system events
- Compliance reporting
- Audit trail creation

**Technologies**:
- Database: H2 (auditdb)
- Message Broker: Kafka (consumes all events)
- Event sourcing capability

---

### Messaging & Event Services

#### 7. kafka-service (Port 8084)
**Role**: Kafka Event Management
- Event publishing
- Event consumption
- Topic management
- Event streaming
- Event persistence

**main-service Integration**:
- Publish business events
- Subscribe to events
- Manage event topics
- Event history retrieval

**Technologies**:
- Kafka Broker: localhost:9092
- Database: H2 (event storage)
- Kafka UI: http://localhost:8080 (if configured)

**Event Topics**:
- user-events
- auth-events
- account-events
- transaction-events

---

#### 8. rabbitmq-service (Port 8085)
**Role**: RabbitMQ Message Queue Management
- Message queuing
- Message publishing
- Queue management
- Message consumption
- Asynchronous processing

**main-service Integration**:
- Queue background tasks
- Batch processing
- Delayed notifications
- Message persistence

**Technologies**:
- RabbitMQ Broker: localhost:5672
- Database: H2 (message storage)
- Management UI: http://localhost:15672 (guest/guest)

**Queue Types**:
- notification-queue
- batch-processing-queue
- email-queue
- sms-queue

---

## 🔄 Integration Patterns

### Pattern 1: Simple Request-Response
```
main-service → user-service
            ← user data
```
**Use Case**: Get user profile

---

### Pattern 2: Aggregation Pattern
```
main-service → user-service
             → account-service
             → auth-service
            ← aggregated response
```
**Use Case**: Get user dashboard (profile + accounts + session)

---

### Pattern 3: Orchestration Pattern
```
main-service → Step 1: auth-service (validate)
             → Step 2: account-service (check balance)
             → Step 3: transaction-service (create txn)
             → Step 4: account-service (update balance)
             → Step 5: kafka-service (publish event)
             → Step 6: audit-service (log)
            ← orchestrated response
```
**Use Case**: Money transfer transaction

---

### Pattern 4: Event-Driven Pattern
```
main-service → transaction-service (create transaction)
             → kafka-service (publish event)
             
Async (background):
kafka-service → notification-service (send email)
              → audit-service (log event)
              → rabbitmq-service (queue for batch)
```
**Use Case**: Transaction with notifications

---

## 📋 Complete API Endpoints (from main-service perspective)

### User Management APIs
```
GET    /api/users/{id}                    → user-service
POST   /api/users                         → user-service
PUT    /api/users/{id}                    → user-service
DELETE /api/users/{id}                    → user-service
GET    /api/users/{id}/profile            → user-service + account-service (aggregated)
```

### Authentication APIs
```
POST   /api/auth/login                    → auth-service
POST   /api/auth/logout                   → auth-service
POST   /api/auth/refresh                  → auth-service
POST   /api/auth/validate                 → auth-service
POST   /api/auth/password-reset           → auth-service + notification-service
```

### Account Management APIs
```
GET    /api/accounts/{id}                 → account-service
GET    /api/accounts/user/{userId}        → account-service
POST   /api/accounts                      → account-service + kafka-service
PUT    /api/accounts/{id}                 → account-service
DELETE /api/accounts/{id}                 → account-service + audit-service
GET    /api/accounts/{id}/balance         → account-service
```

### Transaction APIs
```
POST   /api/transactions/transfer         → auth + account + transaction + kafka + audit + notification
GET    /api/transactions/{id}             → transaction-service
GET    /api/transactions/user/{userId}    → transaction-service
GET    /api/transactions/account/{accountId} → transaction-service
POST   /api/transactions/deposit          → account + transaction + kafka
POST   /api/transactions/withdraw         → account + transaction + kafka
```

### Notification APIs
```
POST   /api/notifications/send            → notification-service
GET    /api/notifications/user/{userId}   → notification-service
PUT    /api/notifications/{id}/read       → notification-service
```

### Audit APIs
```
GET    /api/audit/logs                    → audit-service
GET    /api/audit/logs/user/{userId}      → audit-service
GET    /api/audit/logs/entity/{entityId}  → audit-service
```

### Event Management APIs
```
POST   /api/events/publish                → kafka-service
GET    /api/events/topic/{topicName}      → kafka-service
POST   /api/queue/publish                 → rabbitmq-service
GET    /api/queue/messages                → rabbitmq-service
```

---

## 🔧 main-service Implementation Checklist

### Required Dependencies
- [ ] Spring Boot Starter Web
- [ ] Spring Boot Starter WebFlux (for reactive HTTP client)
- [ ] Spring Cloud Circuit Breaker (Resilience4j)
- [ ] Spring Cloud LoadBalancer
- [ ] Spring Kafka (for event publishing)
- [ ] Spring AMQP (for RabbitMQ integration)

### Service Clients to Implement
```java
// In main-service
@Configuration
public class ServiceClientConfiguration {
    
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
    
    @Bean
    public WebClient transactionServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8088")
            .build();
    }
    
    @Bean
    public WebClient notificationServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8087")
            .build();
    }
    
    @Bean
    public WebClient auditServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8086")
            .build();
    }
    
    @Bean
    public WebClient kafkaServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8084")
            .build();
    }
    
    @Bean
    public WebClient rabbitmqServiceClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8085")
            .build();
    }
}
```

### Controllers to Implement
- [ ] UserAggregatorController
- [ ] AuthAggregatorController
- [ ] AccountAggregatorController
- [ ] TransactionAggregatorController
- [ ] NotificationAggregatorController
- [ ] AuditAggregatorController

### Services to Implement
- [ ] UserAggregatorService
- [ ] AuthAggregatorService
- [ ] AccountAggregatorService
- [ ] TransactionOrchestratorService
- [ ] EventPublisherService
- [ ] AuditLoggerService

---

## 🚀 Quick Start Integration Examples

### Example 1: Call user-service from main-service
```java
@Service
public class UserAggregatorService {
    
    @Autowired
    private WebClient userServiceClient;
    
    public Mono<UserDTO> getUserProfile(Long userId) {
        return userServiceClient.get()
            .uri("/api/users/" + userId)
            .retrieve()
            .bodyToMono(UserDTO.class);
    }
}
```

### Example 2: Aggregate data from multiple services
```java
@Service
public class DashboardService {
    
    @Autowired
    private WebClient userServiceClient;
    
    @Autowired
    private WebClient accountServiceClient;
    
    public Mono<DashboardDTO> getUserDashboard(Long userId) {
        Mono<UserDTO> userMono = userServiceClient.get()
            .uri("/api/users/" + userId)
            .retrieve()
            .bodyToMono(UserDTO.class);
            
        Mono<List<AccountDTO>> accountsMono = accountServiceClient.get()
            .uri("/api/accounts/user/" + userId)
            .retrieve()
            .bodyToFlux(AccountDTO.class)
            .collectList();
            
        return Mono.zip(userMono, accountsMono)
            .map(tuple -> {
                DashboardDTO dashboard = new DashboardDTO();
                dashboard.setUser(tuple.getT1());
                dashboard.setAccounts(tuple.getT2());
                dashboard.setTotalBalance(calculateTotal(tuple.getT2()));
                return dashboard;
            });
    }
}
```

### Example 3: Orchestrate complex transaction
```java
@Service
public class TransactionOrchestratorService {
    
    @Autowired
    private WebClient authServiceClient;
    
    @Autowired
    private WebClient accountServiceClient;
    
    @Autowired
    private WebClient transactionServiceClient;
    
    @Autowired
    private WebClient kafkaServiceClient;
    
    public Mono<TransactionResultDTO> transferMoney(TransferRequestDTO request, String jwtToken) {
        // Step 1: Validate JWT
        return authServiceClient.post()
            .uri("/api/auth/validate")
            .header("Authorization", "Bearer " + jwtToken)
            .retrieve()
            .bodyToMono(ValidationDTO.class)
            
            // Step 2: Validate accounts
            .flatMap(validation -> accountServiceClient.post()
                .uri("/api/accounts/validate-transfer")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AccountValidationDTO.class))
            
            // Step 3: Create transaction
            .flatMap(accountValidation -> transactionServiceClient.post()
                .uri("/api/transactions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TransactionDTO.class))
            
            // Step 4: Publish event
            .flatMap(transaction -> kafkaServiceClient.post()
                .uri("/api/events/publish")
                .bodyValue(new TransactionEvent(transaction))
                .retrieve()
                .bodyToMono(Void.class)
                .thenReturn(transaction))
            
            // Step 5: Build response
            .map(transaction -> new TransactionResultDTO(transaction));
    }
}
```

---

## 📊 Service Health Monitoring

All services expose actuator endpoints:

```
http://localhost:8089/actuator/health  (user-service)
http://localhost:8083/actuator/health  (auth-service)
http://localhost:8082/actuator/health  (account-service)
http://localhost:8088/actuator/health  (transaction-service)
http://localhost:8087/actuator/health  (notification-service)
http://localhost:8086/actuator/health  (audit-service)
http://localhost:8084/actuator/health  (kafka-service)
http://localhost:8085/actuator/health  (rabbitmq-service)
```

main-service can aggregate health status from all services.

---

## 📝 Summary

**Total Services main-service can integrate with**: 8

1. ✅ user-service (8089) - User management
2. ✅ auth-service (8083) - Authentication
3. ✅ account-service (8082) - Account management
4. ✅ transaction-service (8088) - Transaction processing
5. ✅ notification-service (8087) - Notifications
6. ✅ audit-service (8086) - Audit logging
7. ✅ kafka-service (8084) - Event streaming
8. ✅ rabbitmq-service (8085) - Message queuing

**Integration Patterns Used**:
- Request-Response (synchronous)
- Aggregation (parallel calls)
- Orchestration (sequential workflow)
- Event-Driven (asynchronous)

**Technologies**:
- HTTP/REST for synchronous communication
- Apache Kafka for event streaming
- RabbitMQ for message queuing
- WebClient for reactive HTTP calls
- Circuit Breaker for resilience

---

## Related Documentation
- [REQUEST_FLOW_ARCHITECTURE.md](./REQUEST_FLOW_ARCHITECTURE.md) - Architecture details
- [REQUEST_FLOW_DIAGRAM.md](./REQUEST_FLOW_DIAGRAM.md) - Visual diagrams
- [PORT_CONFIGURATION_SUMMARY.md](./PORT_CONFIGURATION_SUMMARY.md) - Port configuration

