# RabbitMQ Service Environment Properties - Quick Reference

## Module Structure
```
rabbitmq-service-env-properties/
├── pom.xml
├── README.md
├── QUICK_REFERENCE.md
└── src/main/resources/
    ├── application-dev.yml      # Development environment
    ├── application-test.yml     # Test environment
    ├── application-staging.yml  # Staging environment
    └── application-prod.yml     # Production environment
```

## Environment Comparison

| Feature | Development | Test | Staging | Production |
|---------|------------|------|---------|------------|
| **Database** | H2 (in-memory) | H2 (in-memory) | PostgreSQL | PostgreSQL Cluster |
| **RabbitMQ Host** | localhost | localhost | staging-rabbitmq | prod-rabbitmq-cluster |
| **Port** | 8085 | 8085 | 8085 | 8085 |
| **ACK Mode** | auto | auto | manual | manual |
| **Prefetch** | 1 | 1 | 5 | 10 |
| **Retry Attempts** | 3 | 2 | 5 | 10 |
| **SSL** | ❌ | ❌ | ❌ | ✅ |
| **Virtual Host** | default | default | /staging | /production |
| **Logging Level** | DEBUG | DEBUG | INFO | WARN |
| **H2 Console** | ✅ | ✅ | ❌ | ❌ |

## Queue Naming Convention

- **Dev**: `rabbitmq-event-queue`
- **Test**: `rabbitmq-event-queue-test`
- **Staging**: `rabbitmq-event-queue-staging`
- **Production**: `rabbitmq-event-queue-prod`

## Quick Commands

```bash
# Build module
mvn clean package

# Use specific profile
java -jar rabbitmq-service.jar --spring.profiles.active=dev

# Set environment variable
export SPRING_PROFILES_ACTIVE=prod
```

## Required Environment Variables (Staging/Prod)

```bash
DB_USERNAME=<database_username>
DB_PASSWORD=<database_password>
RABBITMQ_USERNAME=<rabbitmq_username>
RABBITMQ_PASSWORD=<rabbitmq_password>
SSL_KEYSTORE_PATH=<path_to_keystore>        # Production only
SSL_KEYSTORE_PASSWORD=<keystore_password>    # Production only
```

## Key Differences by Environment

### Development
- Local development with minimal configuration
- Auto-acknowledgment for simplicity
- H2 console enabled for debugging
- Verbose logging

### Test
- Isolated test environment
- Separate queue names to avoid conflicts
- Create-drop DDL for clean state
- Simplified retry logic

### Staging
- Pre-production environment
- Manual acknowledgment for reliability
- PostgreSQL database
- Credential management via environment variables
- Moderate connection pooling

### Production
- Full security (SSL/TLS)
- Optimized for high throughput
- Maximum retry attempts
- Batch processing
- Extensive monitoring
- Structured logging with file rotation

