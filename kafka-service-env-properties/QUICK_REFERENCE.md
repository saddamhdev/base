# Kafka Service Environment Properties - Quick Reference

## Module Structure
```
kafka-service-env-properties/
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
| **Kafka Brokers** | 1 (localhost) | 1 (localhost) | 2 brokers | 3 brokers |
| **Port** | 8084 | 8084 | 8084 | 8084 |
| **Auto Commit** | ✅ | ❌ | ❌ | ❌ |
| **ACKs** | 1 | 1 | all | all |
| **Retries** | 3 | 1 | 5 | 10 |
| **Security** | None | None | SASL_SSL | SASL_SSL |
| **SSL** | ❌ | ❌ | ❌ | ✅ |
| **Idempotence** | ❌ | ❌ | ✅ | ✅ |
| **Compression** | None | None | None | Snappy |
| **Max Poll Records** | default | default | 100 | 500 |
| **Logging Level** | DEBUG | DEBUG | INFO | WARN |
| **H2 Console** | ✅ | ✅ | ❌ | ❌ |

## Topic Naming Convention

- **Dev**: `kafka-event-topic-dev`
- **Test**: `kafka-event-topic-test`
- **Staging**: `kafka-event-topic-staging`
- **Production**: `kafka-event-topic-prod`

## Consumer Group Convention

- **Dev**: `kafka-event-group-dev`
- **Test**: `kafka-event-group-test`
- **Staging**: `kafka-event-group-staging`
- **Production**: `kafka-event-group-prod`

## Quick Commands

```bash
# Build module
mvn clean package

# Use specific profile
java -jar kafka-service.jar --spring.profiles.active=dev

# Set environment variable
export SPRING_PROFILES_ACTIVE=prod
```

## Required Environment Variables (Staging/Prod)

```bash
DB_USERNAME=<database_username>
DB_PASSWORD=<database_password>
KAFKA_USERNAME=<kafka_username>
KAFKA_PASSWORD=<kafka_password>
SSL_KEYSTORE_PATH=<path_to_keystore>        # Production only
SSL_KEYSTORE_PASSWORD=<keystore_password>    # Production only
```

## Key Differences by Environment

### Development
- Single Kafka broker on localhost
- Auto-commit enabled for simplicity
- Basic acknowledgment (acks=1)
- H2 console enabled for debugging
- Verbose logging

### Test
- Isolated test environment
- Manual commit for control
- Separate topic names to avoid conflicts
- Create-drop DDL for clean state
- Minimal retries for fast failure

### Staging
- Multi-broker Kafka cluster (2 brokers)
- Manual commit for reliability
- All acknowledgment (acks=all)
- SASL_SSL security
- Idempotent producer
- PostgreSQL database
- Credential management via environment variables

### Production
- Multi-broker Kafka cluster (3 brokers)
- Full security (SASL_SSL + server SSL)
- Optimized for high throughput:
  - Batch size: 32KB
  - Linger ms: 10
  - Snappy compression
  - Max poll records: 500
- Idempotent producer
- Maximum retry attempts (10)
- Extensive monitoring
- Structured logging with file rotation

## Performance Tuning (Production)

- **Batch Processing**: 32KB batch size with 10ms linger time
- **Compression**: Snappy for optimal network usage
- **Throughput**: 500 records per poll
- **Reliability**: All acknowledgments + idempotence
- **Session Management**: 45s timeout, 15s heartbeat

