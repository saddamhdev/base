# 🚀 Quick Start Guide - Add User Flow

## ✅ Ready to Use!

The **Add User Flow** is now complete and ready to test.

---

## 📋 Prerequisites

- ✅ Java 21 installed
- ✅ Maven installed
- ✅ Docker installed (for Kafka & RabbitMQ)
- ✅ All services built successfully

---

## 🎯 Quick Start (3 Steps)

### Step 1: Start Message Brokers (1 minute)

```powershell
# Start Kafka
cd "D:\module project\base\kafka-service"
docker-compose up -d

# Start RabbitMQ
cd "D:\module project\base\rabbitmq-service"
docker-compose up -d

# Verify they're running
docker ps
```

**Expected Output**: You should see Kafka, Zookeeper, and RabbitMQ containers running.

---

### Step 2: Start Services (2 minutes)

Open **8 separate PowerShell terminals** and run these commands:

#### Terminal 1 - Gateway Service
```powershell
cd "D:\module project\base\gateway-service"
mvn spring-boot:run
```
**Wait for**: `Started GatewayServiceApplication`

#### Terminal 2 - Main Service (Aggregator)
```powershell
cd "D:\module project\base\main"
mvn spring-boot:run
```
**Wait for**: `Started MainApplication`

#### Terminal 3 - User Service
```powershell
cd "D:\module project\base\user-service"
mvn spring-boot:run
```
**Wait for**: `Started UserServiceApplication`

#### Terminal 4 - Auth Service
```powershell
cd "D:\module project\base\auth-service"
mvn spring-boot:run
```
**Wait for**: `Started AuthServiceApplication`

#### Terminal 5 - Account Service
```powershell
cd "D:\module project\base\account-service"
mvn spring-boot:run
```
**Wait for**: `Started AccountServiceApplication`

#### Terminal 6 - Notification Service
```powershell
cd "D:\module project\base\notification-service"
mvn spring-boot:run
```
**Wait for**: `Started NotificationServiceApplication`

#### Terminal 7 - Audit Service
```powershell
cd "D:\module project\base\audit-service"
mvn spring-boot:run
```
**Wait for**: `Started AuditServiceApplication`

#### Terminal 8 - Kafka Service
```powershell
cd "D:\module project\base\kafka-service"
mvn spring-boot:run
```
**Wait for**: `Started KafkaServiceApplication`

---

### Step 3: Test the Flow (30 seconds)

```powershell
# Create a new user
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
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body
```

**Expected Response**:
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
  "authToken": "eyJhbGci...",
  "createdAt": "2026-02-20T...",
  "message": "User created successfully",
  "notificationSent": true,
  "auditLogged": true
}
```

---

## ✅ Verification

### Check User in Database
```powershell
curl http://localhost:8089/api/users/1
```

### Check Account Created
```powershell
curl http://localhost:8082/api/accounts/user/1
```

### Test Login
```powershell
$loginBody = @{
    username = "john"
    password = "SecurePass123!"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/api/auth/login" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $loginBody
```

---

## 🔍 Health Checks

Check if all services are running:

```powershell
# Gateway
curl http://localhost:8080/actuator/health

# Main Service
curl http://localhost:3081/api/users/health

# User Service
curl http://localhost:8089/actuator/health

# Auth Service
curl http://localhost:8083/actuator/health

# Account Service
curl http://localhost:8082/actuator/health

# Notification Service
curl http://localhost:8087/actuator/health

# Audit Service
curl http://localhost:8086/actuator/health

# Kafka Service
curl http://localhost:8084/actuator/health
```

**All should return**: `{"status":"UP"}`

---

## 📊 What Happens When You Create a User?

```
1. Gateway receives request (8080)
   ↓
2. Routes to main-service (3081)
   ↓
3. main-service orchestrates:
   ├─→ Creates user in user-service (8089) ✓
   ├─→ Creates auth in auth-service (8083) ✓
   ├─→ Creates account in account-service (8082) ✓
   ├─→ Publishes event to Kafka (8084) ✓
   ├─→ Sends notification (8087) [async] ✓
   └─→ Logs to audit (8086) [async] ✓
   ↓
4. Returns aggregated response to client
```

---

## 🛑 Stop Everything

### Stop Spring Boot Services
Press `Ctrl+C` in each terminal window.

### Stop Docker Containers
```powershell
cd "D:\module project\base\kafka-service"
docker-compose down

cd "D:\module project\base\rabbitmq-service"
docker-compose down
```

---

## 🐛 Troubleshooting

### Issue: Service won't start
**Solution**: Check if the port is already in use
```powershell
netstat -ano | findstr :8080
```

### Issue: Docker containers not starting
**Solution**: Make sure Docker Desktop is running
```powershell
docker ps
```

### Issue: Gateway returns 404
**Solution**: 
1. Check main-service is running on port 3081
2. Check gateway logs for routing errors

### Issue: Compilation errors
**Solution**: Rebuild the project
```powershell
cd "D:\module project\base"
mvn clean install -DskipTests
```

---

## 📚 Documentation

- **Complete Flow**: See `ADD_USER_FLOW.md`
- **Architecture**: See `ARCHITECTURE_COMPLETE.md`
- **Integration Guide**: See `MAIN_SERVICE_INTEGRATION.md`
- **Visual Diagrams**: See `REQUEST_FLOW_DIAGRAM.md`

---

## 🎓 Key Files

| File | Purpose |
|------|---------|
| `UserAggregatorController.java` | REST endpoint |
| `UserAggregatorService.java` | Orchestration logic |
| `ServiceClientConfiguration.java` | WebClient setup |
| `GatewayConfiguration.java` | Gateway routing |

---

## 🌟 Success Indicators

- ✅ User created in userdb
- ✅ Auth credentials in authdb
- ✅ Account created in accountdb
- ✅ Event published to Kafka
- ✅ Welcome email sent (async)
- ✅ Audit log created (async)
- ✅ Client receives 201 response

---

## 📞 Need Help?

Check the logs in each terminal window for error messages. Services log:
- Request received
- Service calls
- Responses
- Errors

---

**Version**: 1.0
**Status**: ✅ Ready to Test
**Last Updated**: February 20, 2026

