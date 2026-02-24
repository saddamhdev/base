# SNVN Microservices Platform

A comprehensive multi-module Spring Boot microservices application with event-driven architecture, API Gateway, and distributed messaging.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                            │
│          (Web Browser, Mobile App, Desktop App, APIs)                   │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ HTTPS/HTTP
                                 ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      GATEWAY SERVICE (8093)                              │
│                     Spring Cloud Gateway                                 │
│        • Routing • Authentication • Rate Limiting • CORS                │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ↓                       ↓                       ↓
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  MAIN SERVICE   │    │  AUTH SERVICE   │    │  USER SERVICE   │
│     (8094)      │    │     (8096)      │    │     (8102)      │
│   Aggregator    │    │  JWT & Security │    │  User Management│
└────────┬────────┘    └─────────────────┘    └─────────────────┘
         │
         ├──────────────────────────────────────────────────────┐
         ↓                       ↓                              ↓
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ ACCOUNT SERVICE │    │TRANSACTION SVC  │    │NOTIFICATION SVC │
│     (8095)      │    │     (8101)      │    │     (8100)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                              │
         └───────────────────────┼──────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      EVENT-DRIVEN MESSAGING LAYER                        │
│  ┌─────────────────────────┐        ┌─────────────────────────┐        │
│  │   KAFKA SERVICE (8097)  │        │  RABBITMQ SERVICE (8098)│        │
│  │   Broker: 9092          │        │  Broker: 5672           │        │
│  │   UI: 8080              │        │  UI: 15672              │        │
│  └─────────────────────────┘        └─────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📦 Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 4.0.0 | Application Framework |
| Spring Cloud | 2023.0.3 | Microservices Infrastructure |
| Spring Cloud Gateway | - | API Gateway |
| Apache Kafka | - | Event Streaming |
| RabbitMQ | - | Message Queuing |
| H2 Database | - | In-Memory Database |
| Maven | - | Build Tool |
| Docker | - | Containerization |

## 🚀 Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| **config-service** | 8092 | Centralized Configuration |
| **gateway-service** | 8093 | API Gateway (Entry Point) |
| **main-service** | 8094 | Main Aggregator / BFF |
| **account-service** | 8095 | Account Management |
| **auth-service** | 8096 | Authentication & JWT |
| **kafka-service** | 8097 | Kafka Event Publisher |
| **rabbitmq-service** | 8098 | RabbitMQ Messaging |
| **audit-service** | 8099 | Audit Logging |
| **notification-service** | 8100 | Notifications |
| **transaction-service** | 8101 | Transaction Processing |
| **user-service** | 8102 | User Management |
| **mcp-server** | 8103 | MCP Server |
| **splunk-service** | 8104 | Splunk Integration |

## 📁 Project Structure

```
base/
├── pom.xml                          # Parent POM
│
├── gateway-service/                 # API Gateway
├── gateway-service-env-properties/
│
├── main/                            # Main Aggregator Service
├── main-env-properties/
│
├── auth-service/                    # Authentication Service
├── user-service/                    # User Management
├── account-service/                 # Account Management
├── transaction-service/             # Transaction Processing
├── notification-service/            # Notifications
├── audit-service/                   # Audit Logging
│
├── kafka-service/                   # Kafka Integration
├── kafka-service-env-properties/
│
├── rabbitmq-service/                # RabbitMQ Integration
├── rabbitmq-service-env-properties/
│
├── splunk-service/                  # Splunk Logging
├── config-service/                  # Centralized Config
├── mcp-server/                      # MCP Server
│
├── core-common/                     # Shared Library
├── model/                           # Shared Domain Models
│
└── logs/                            # Application Logs
```

## 🔧 Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Docker & Docker Compose** (for Kafka/RabbitMQ)
- **Git**

## 🚀 Quick Start

### 1. Clone & Build

```bash
# Clone repository
git clone <repository-url>
cd base

# Build all modules
mvn clean install -DskipTests
```

### 2. Start Infrastructure (Docker)

```bash
# Start Kafka
cd kafka-service
docker-compose up -d

# Start RabbitMQ
cd ../rabbitmq-service
docker-compose up -d
```

### 3. Run Services

Start services in the following order:

```bash
# Terminal 1 - Config Service
cd config-service
mvn spring-boot:run

# Terminal 2 - Gateway Service
cd gateway-service
mvn spring-boot:run

# Terminal 3 - Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 4 - Main Service
cd main
mvn spring-boot:run

# Start other services as needed...
```

### 4. Verify

```bash
# Check Gateway Health
curl http://localhost:8093/actuator/health

# Check Main Service
curl http://localhost:8094/actuator/health
```

## 📡 API Endpoints

### Gateway Service (8093)
All requests go through the gateway:
```
http://localhost:8093/api/...
```

### Auth Service
```bash
# Login
POST http://localhost:8093/api/auth/login
Content-Type: application/json
{
    "username": "user",
    "password": "password"
}

# Register
POST http://localhost:8093/api/auth/register
```

### User Service
```bash
# Get all users
GET http://localhost:8093/api/users

# Get user by ID
GET http://localhost:8093/api/users/{id}

# Create user
POST http://localhost:8093/api/users
```

### Kafka Events
```bash
# Publish event
POST http://localhost:8097/api/kafka/events
Content-Type: application/json
{
    "eventType": "USER_CREATED",
    "payload": "{\"userId\":1}",
    "source": "test"
}

# Get all events
GET http://localhost:8097/api/kafka/events
```

### RabbitMQ Events
```bash
# Publish event
POST http://localhost:8098/api/rabbitmq/events
Content-Type: application/json
{
    "eventType": "ORDER_CREATED",
    "payload": "{\"orderId\":123}",
    "source": "test"
}
```

## 🛠️ Management & Monitoring

| Service | URL | Credentials |
|---------|-----|-------------|
| Kafka UI | http://localhost:8080 | - |
| RabbitMQ UI | http://localhost:15672 | guest/guest |
| H2 Console (per service) | http://localhost:{PORT}/h2-console | sa / (empty) |
| Actuator Health | http://localhost:{PORT}/actuator/health | - |

## 🏗️ Module Dependencies

```
model                    ← Shared domain models
    ↑
core-common              ← Shared utilities & exceptions
    ↑
┌───┴───┬───────┬───────┬───────┬───────┬───────┐
│       │       │       │       │       │       │
user    auth   account  trans  notif  audit   ...
service service service service service service
```

## 📊 Communication Patterns

### 1. Synchronous (REST)
```
Client → Gateway → Main Service → Microservices
```

### 2. Asynchronous (Event-Driven)
```
Service → Kafka/RabbitMQ → Consumer Services
```

### 3. Orchestrated Workflow
```
Main Service orchestrates:
  1. Auth Service → Validate Token
  2. Account Service → Check Balance
  3. Transaction Service → Process
  4. Kafka Service → Publish Event
```

## 🔐 Security

- **JWT Authentication** via Auth Service
- **Gateway-level** request filtering
- **CORS** configuration
- **Request tracing** with correlation IDs

## 📝 Logging

All services log to:
- Console output
- `logs/` directory (per-service log files)
- Splunk integration (optional)

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run tests for specific module
cd user-service
mvn test
```

## 🐳 Docker Commands

```bash
# Start all infrastructure
cd kafka-service && docker-compose up -d
cd ../rabbitmq-service && docker-compose up -d
cd ../splunk-service && docker-compose up -d

# Stop all
docker-compose down

# View logs
docker-compose logs -f
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE_COMPLETE.md](ARCHITECTURE_COMPLETE.md) | Full architecture details |
| [BUILD_GUIDE.md](BUILD_GUIDE.md) | Build instructions |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | Quick command reference |
| [PORT_CONFIGURATION_SUMMARY.md](PORT_CONFIGURATION_SUMMARY.md) | Port assignments |
| [ADD_USER_FLOW.md](ADD_USER_FLOW.md) | User creation flow |
| [REQUEST_FLOW_ARCHITECTURE.md](REQUEST_FLOW_ARCHITECTURE.md) | Request flow details |

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- SNVN Team

---

**Happy Coding! 🚀**

