# Add New User - Complete Flow Documentation

## 📋 Overview

This document describes the complete end-to-end flow for creating a new user in the microservices system.

---

## 🔄 Complete Flow Diagram

```
┌─────────────┐
│   Client    │
│  (Postman,  │
│  Browser,   │
│   Mobile)   │
└──────┬──────┘
       │
       │ 1. POST /api/users
       │    Body: { username, email, password, firstName, lastName, role }
       ↓
┌──────────────────────────────────────────────────────────┐
│              Gateway Service (Port 8080)                 │
│              Spring Cloud Gateway                        │
└──────┬───────────────────────────────────────────────────┘
       │
       │ 2. Routes to main-service
       │    POST http://localhost:3081/api/users
       ↓
┌──────────────────────────────────────────────────────────┐
│           main-service (Port 3081)                       │
│           UserAggregatorController                       │
│           UserAggregatorService                          │
└──────┬───────────────────────────────────────────────────┘
       │
       │ 3. ORCHESTRATION BEGINS
       │
       ├─── Step 3a: Create User ──────────────────┐
       │                                            ↓
       │                                    ┌───────────────┐
       │                                    │ user-service  │
       │                                    │   (8089)      │
       │                                    │ POST /api/users│
       │                                    │ Save to userdb│
       │                                    └───────┬───────┘
       │                                            │
       │    User Created: { id: 1, username: "john", email: "john@example.com" }
       │                                            │
       ├────────────────────────────────────────────┘
       │
       ├─── Step 3b: Create Auth (Parallel) ───────┐
       │                                            ↓
       │                                    ┌───────────────┐
       │                                    │ auth-service  │
       │                                    │   (8083)      │
       │                                    │ POST /api/auth│
       │                                    │ /register     │
       │                                    │ Save to authdb│
       │                                    └───────┬───────┘
       │                                            │
       │    Auth Created: { token: "eyJhbGc..." }   │
       │                                            │
       ├────────────────────────────────────────────┘
       │
       ├─── Step 3c: Create Account (Parallel) ────┐
       │                                            ↓
       │                                    ┌───────────────┐
       │                                    │account-service│
       │                                    │   (8082)      │
       │                                    │ POST /api/    │
       │                                    │ accounts      │
       │                                    │ Save to       │
       │                                    │ accountdb     │
       │                                    └───────┬───────┘
       │                                            │
       │    Account Created: { id: 1, accountNumber: "ACC123", balance: 0.0 }
       │                                            │
       ├────────────────────────────────────────────┘
       │
       ├─── Step 3d: Publish Event to Kafka ───────┐
       │                                            ↓
       │                                    ┌───────────────┐
       │                                    │ kafka-service │
       │                                    │   (8084)      │
       │                                    │ POST /api/    │
       │                                    │ kafka/events  │
       │                                    │ Publish to    │
       │                                    │ user-events   │
       │                                    │ topic         │
       │                                    └───────┬───────┘
       │                                            │
       │    Event Published: { eventType: "USER_CREATED", userId: 1 }
       │                                            │
       ├────────────────────────────────────────────┘
       │
       │    ASYNC OPERATIONS (Background Processing)
       │
       ├─── Step 3e: Send Notification (Async) ────┐
       │                                            ↓
       │                                    ┌───────────────┐
       │                                    │notification-  │
       │                                    │  service      │
       │                                    │   (8087)      │
       │                                    │ POST /api/    │
       │                                    │ notifications │
       │                                    │ /send         │
       │                                    │ Email: Welcome│
       │                                    └───────────────┘
       │
       ├─── Step 3f: Log to Audit (Async) ─────────┐
       │                                            ↓
       │                                    ┌───────────────┐
       │                                    │ audit-service │
       │                                    │   (8086)      │
       │                                    │ POST /api/    │
       │                                    │ audit/logs    │
       │                                    │ Log: USER_    │
       │                                    │ CREATED       │
       │                                    └───────────────┘
       │
       │ 4. AGGREGATION COMPLETE
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│           main-service Response                          │
│                                                          │
│  {                                                       │
│    "userId": 1,                                         │
│    "username": "john",                                  │
│    "email": "john@example.com",                         │
│    "firstName": "John",                                 │
│    "lastName": "Doe",                                   │
│    "role": "USER",                                      │
│    "enabled": true,                                     │
│    "accountId": 1,                                      │
│    "accountNumber": "ACC1708444800000",                 │
│    "initialBalance": 0.0,                               │
│    "authToken": "eyJhbGciOiJIUzI1NiIs...",              │
│    "createdAt": "2026-02-20T10:30:00",                  │
│    "message": "User created successfully",              │
│    "notificationSent": true,                            │
│    "auditLogged": true                                  │
│  }                                                       │
└──────┬───────────────────────────────────────────────────┘
       │
       │ 5. Return to Gateway
       ↓
┌──────────────────────────────────────────────────────────┐
│              Gateway Service (Port 8080)                 │
└──────┬───────────────────────────────────────────────────┘
       │
       │ 6. Return to Client
       ↓
┌─────────────┐
│   Client    │
│  Receives   │
│  Complete   │
│  Response   │
└─────────────┘
```

---

## 📝 Step-by-Step Flow

### Step 1: Client Request
**Endpoint**: `POST http://localhost:8080/api/users`

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}
```

---

### Step 2: Gateway Routing
**Gateway Service** (Port 8080) receives the request and routes it to:
- **Target**: main-service at `http://localhost:3081/api/users`
- **Route Configuration**: Defined in `GatewayConfiguration.java`

---

### Step 3: main-service Orchestration

#### 3a. Create User in user-service
**Service**: user-service (Port 8089)
**Endpoint**: `POST /api/users`

**Request**:
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "enabled": true
}
```

**Response**:
```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "enabled": true
}
```

**Database**: Saved to H2 database `userdb`

---

#### 3b. Create Auth Credentials (Parallel)
**Service**: auth-service (Port 8083)
**Endpoint**: `POST /api/auth/register`

**Request**:
```json
{
  "username": "john",
  "password": "SecurePass123!",
  "userId": "1"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

**Database**: Saved to H2 database `authdb`

---

#### 3c. Create Default Account (Parallel)
**Service**: account-service (Port 8082)
**Endpoint**: `POST /api/accounts`

**Request**:
```json
{
  "userId": 1,
  "accountType": "CHECKING",
  "balance": 0.0,
  "accountNumber": "ACC1708444800000"
}
```

**Response**:
```json
{
  "id": 1,
  "userId": 1,
  "accountNumber": "ACC1708444800000",
  "accountType": "CHECKING",
  "balance": 0.0,
  "status": "ACTIVE"
}
```

**Database**: Saved to H2 database `accountdb`

---

#### 3d. Publish Event to Kafka
**Service**: kafka-service (Port 8084)
**Endpoint**: `POST /api/kafka/events`

**Event**:
```json
{
  "eventType": "USER_CREATED",
  "userId": 1,
  "username": "john",
  "email": "john@example.com",
  "timestamp": 1708444800000
}
```

**Topic**: `user-events`

**Consumers**:
- audit-service (logs the event)
- notification-service (may send notifications)
- Any other service subscribed to user-events

---

#### 3e. Send Welcome Notification (Async)
**Service**: notification-service (Port 8087)
**Endpoint**: `POST /api/notifications/send`

**Request**:
```json
{
  "userId": 1,
  "email": "john@example.com",
  "type": "WELCOME_EMAIL",
  "subject": "Welcome to Our Platform!",
  "message": "Hello John Doe,\n\nWelcome to our platform! Your account has been created successfully."
}
```

**Actions**:
- Send welcome email to john@example.com
- Queue notification in RabbitMQ for processing
- Save notification record to notificationdb

---

#### 3f. Log to Audit Service (Async)
**Service**: audit-service (Port 8086)
**Endpoint**: `POST /api/audit/logs`

**Request**:
```json
{
  "eventType": "USER_CREATED",
  "userId": 1,
  "username": "john",
  "action": "CREATE_USER",
  "details": "New user account created",
  "timestamp": 1708444800000
}
```

**Database**: Saved to H2 database `auditdb`

---

### Step 4: Aggregated Response

main-service aggregates all responses and returns:

```json
{
  "userId": 1,
  "username": "john",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "enabled": true,
  "accountId": 1,
  "accountNumber": "ACC1708444800000",
  "initialBalance": 0.0,
  "authToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "createdAt": "2026-02-20T10:30:00",
  "message": "User created successfully",
  "notificationSent": true,
  "auditLogged": true
}
```

---

## 🚀 How to Test

### Using cURL

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER"
  }'
```

### Using PowerShell

```powershell
$headers = @{
    "Content-Type" = "application/json"
}

$body = @{
    username = "john"
    email = "john@example.com"
    password = "SecurePass123!"
    firstName = "John"
    lastName = "Doe"
    role = "USER"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/users" `
    -Method POST `
    -Headers $headers `
    -Body $body
```

### Using Postman

1. **Method**: POST
2. **URL**: `http://localhost:8080/api/users`
3. **Headers**: 
   - Content-Type: application/json
4. **Body** (raw JSON):
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}
```

---

## 📊 Service Interactions Summary

| Step | Service | Port | Action | Database | Async |
|------|---------|------|--------|----------|-------|
| 1 | gateway-service | 8080 | Route request | - | No |
| 2 | main-service | 3081 | Orchestrate | testdb | No |
| 3a | user-service | 8089 | Create user | userdb | No |
| 3b | auth-service | 8083 | Create auth | authdb | No |
| 3c | account-service | 8082 | Create account | accountdb | No |
| 3d | kafka-service | 8084 | Publish event | - | No |
| 3e | notification-service | 8087 | Send email | notificationdb | Yes |
| 3f | audit-service | 8086 | Log event | auditdb | Yes |

---

## 🔍 Verification Steps

### 1. Check User in user-service
```bash
curl http://localhost:8089/api/users/1
```

### 2. Check Account in account-service
```bash
curl http://localhost:8082/api/accounts/user/1
```

### 3. Check Auth Token
```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"SecurePass123!"}'
```

### 4. Check Kafka Events
```bash
curl http://localhost:8084/api/kafka/events/topic/user-events
```

### 5. Check Audit Logs
```bash
curl http://localhost:8086/api/audit/logs/user/1
```

### 6. Check Notifications
```bash
curl http://localhost:8087/api/notifications/user/1
```

### 7. Check H2 Databases
- user-service: http://localhost:8089/h2-console (JDBC URL: jdbc:h2:mem:userdb)
- auth-service: http://localhost:8083/h2-console (JDBC URL: jdbc:h2:mem:authdb)
- account-service: http://localhost:8082/h2-console (JDBC URL: jdbc:h2:mem:accountdb)
- audit-service: http://localhost:8086/h2-console (JDBC URL: jdbc:h2:mem:auditdb)

---

## ⚠️ Prerequisites

### Services Must Be Running

Start services in this order:

```powershell
# 1. Start Kafka
cd "D:\module project\base\kafka-service"
docker-compose up -d

# 2. Start RabbitMQ
cd "D:\module project\base\rabbitmq-service"
docker-compose up -d

# 3. Start all Spring Boot services
# In separate terminals:

# Terminal 1: Gateway
cd "D:\module project\base\gateway-service"
mvn spring-boot:run

# Terminal 2: Main Service
cd "D:\module project\base\main"
mvn spring-boot:run

# Terminal 3: User Service
cd "D:\module project\base\user-service"
mvn spring-boot:run

# Terminal 4: Auth Service
cd "D:\module project\base\auth-service"
mvn spring-boot:run

# Terminal 5: Account Service
cd "D:\module project\base\account-service"
mvn spring-boot:run

# Terminal 6: Notification Service
cd "D:\module project\base\notification-service"
mvn spring-boot:run

# Terminal 7: Audit Service
cd "D:\module project\base\audit-service"
mvn spring-boot:run

# Terminal 8: Kafka Service
cd "D:\module project\base\kafka-service"
mvn spring-boot:run
```

### Health Check All Services

```powershell
# Check if all services are up
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:3081/api/users/health # Main
curl http://localhost:8089/actuator/health  # User
curl http://localhost:8083/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Account
curl http://localhost:8087/actuator/health  # Notification
curl http://localhost:8086/actuator/health  # Audit
curl http://localhost:8084/actuator/health  # Kafka
```

---

## 🎯 Expected Results

### Success Response (201 Created)
```json
{
  "userId": 1,
  "username": "john",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "enabled": true,
  "accountId": 1,
  "accountNumber": "ACC1708444800000",
  "initialBalance": 0.0,
  "authToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "createdAt": "2026-02-20T10:30:00",
  "message": "User created successfully",
  "notificationSent": true,
  "auditLogged": true
}
```

### Error Responses

#### 400 Bad Request (Invalid Input)
```json
{
  "message": "Failed to create user: Invalid request data"
}
```

#### 500 Internal Server Error (Service Failure)
```json
{
  "message": "Failed to create user: user-service is unavailable"
}
```

---

## 🔧 Troubleshooting

### Issue: Gateway returns 404
**Solution**: Check if main-service is running on port 3081
```bash
curl http://localhost:3081/api/users/health
```

### Issue: User created but account not created
**Solution**: Check account-service logs and status
```bash
curl http://localhost:8082/actuator/health
```

### Issue: Notification not sent
**Solution**: Check notification-service is running (async operation, won't block user creation)

### Issue: Kafka event not published
**Solution**: Check Kafka is running
```bash
docker ps | grep kafka
```

---

## 📚 Related Files

- **Controller**: `main/src/main/java/snvn/controller/UserAggregatorController.java`
- **Service**: `main/src/main/java/snvn/service/UserAggregatorService.java`
- **DTOs**: `main/src/main/java/snvn/dto/CreateUserRequest.java`, `CreateUserResponse.java`
- **Config**: `main/src/main/java/snvn/config/ServiceClientConfiguration.java`
- **Gateway**: `gateway-service/src/main/java/snvn/gatewayservice/config/GatewayConfiguration.java`

---

## ✅ Success Indicators

- ✅ User record in userdb
- ✅ Auth credentials in authdb
- ✅ Default account in accountdb
- ✅ Event in Kafka user-events topic
- ✅ Welcome email sent (async)
- ✅ Audit log created (async)
- ✅ Client receives complete aggregated response

---

**Last Updated**: February 20, 2026
**Flow Version**: 1.0

