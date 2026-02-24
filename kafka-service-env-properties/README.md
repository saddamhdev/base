# Kafka Service Environment Properties

This module contains environment-specific configuration files for the `kafka-service`.

## Overview

Environment-specific properties are externalized to support different configurations across development, test, staging, and production environments.

## Available Profiles

### 1. Development (`application-dev.yml`)
- **Database**: H2 in-memory database
- **Kafka**: Local single broker (localhost:9092)
- **Server Port**: 8084
- **Logging**: DEBUG level for Kafka service
- **H2 Console**: Enabled
- **Features**:
  - Auto-commit enabled
  - Standard acknowledgment (acks=1)
  - Simple retry configuration (3 attempts)
  - Consumer group: `kafka-event-group-dev`
  - Topic: `kafka-event-topic-dev`

### 2. Test (`application-test.yml`)
- **Database**: H2 in-memory database
- **Kafka**: Local instance with test-specific topics
- **Server Port**: 8084
- **Logging**: INFO level with DEBUG for service
- **Features**:
  - Auto-commit disabled (manual commit)
  - Test-specific topic names
  - Minimal retry (1 attempt)
  - DDL auto mode: create-drop
  - Consumer group: `kafka-event-group-test`

### 3. Staging (`application-staging.yml`)
- **Database**: PostgreSQL on staging server
- **Kafka**: Staging Kafka cluster (2 brokers)
- **Server Port**: 8084
- **Logging**: WARN level with file logging
- **Features**:
  - Manual commit (auto-commit disabled)
  - All acknowledgment (acks=all)
  - Advanced retry configuration (5 attempts)
  - SASL_SSL security protocol
  - Connection pooling (max: 10)
  - Idempotent producer enabled
  - Prometheus metrics enabled
  - Environment variables for credentials
  - Consumer group: `kafka-event-group-staging`
  - Max poll records: 100

### 4. Production (`application-prod.yml`)
- **Database**: PostgreSQL production cluster
- **Kafka**: Production Kafka cluster (3 brokers)
- **Server Port**: 8084
- **Logging**: ERROR level with structured logging
- **Features**:
  - SSL/TLS enabled for Kafka
  - All acknowledgment (acks=all)
  - Enhanced retry (10 attempts)
  - SASL_SSL security protocol
  - Optimized connection pooling (max: 20)
  - Idempotent producer with compression (snappy)
  - Batch processing optimization
  - SSL/TLS for server
  - Prometheus metrics
  - File logging with rotation (100MB, 30 days)
  - Consumer group: `kafka-event-group-prod`
  - Max poll records: 500
  - Optimized poll intervals and session timeouts

## Configuration Details

### Kafka Properties

Each environment defines:
- **Bootstrap Servers**: Environment-specific Kafka broker addresses
- **Consumer Group**: Environment-specific consumer group IDs
- **Topic Name**: Environment-specific topic names
- **Security**: SASL_SSL for staging and production

### Producer Settings

- **Development/Test**: Basic serialization, minimal retries
- **Staging**: Idempotence enabled, moderate retries
- **Production**: Full idempotence, compression, batching, maximum retries

### Consumer Settings

- **Development**: Auto-commit enabled
- **Test/Staging/Production**: Manual commit for better control
- **Production**: Optimized poll settings for high throughput

### Environment Variables

Production and Staging require the following environment variables:

```bash
# Database
DB_USERNAME=<database_username>
DB_PASSWORD=<database_password>

# Kafka
KAFKA_USERNAME=<kafka_username>
KAFKA_PASSWORD=<kafka_password>

# SSL (Production only)
SSL_KEYSTORE_PATH=<path_to_keystore>
SSL_KEYSTORE_PASSWORD=<keystore_password>
```

## Usage

To use a specific profile, set the Spring active profile:

```bash
# Development
java -jar kafka-service.jar --spring.profiles.active=dev

# Test
java -jar kafka-service.jar --spring.profiles.active=test

# Staging
java -jar kafka-service.jar --spring.profiles.active=staging

# Production
java -jar kafka-service.jar --spring.profiles.active=prod
```

Or set via environment variable:
```bash
export SPRING_PROFILES_ACTIVE=prod
```

## Building

This module is packaged as a configuration JAR:

```bash
mvn clean package
```

Output: `kafka-service-env-properties-1.0-SNAPSHOT-config.jar`

## Integration

The kafka-service should include this module as a dependency to access environment-specific configurations.

## Performance Tuning

### Production Settings

The production profile includes optimized settings:
- **Batch Size**: 32KB for efficient network usage
- **Linger MS**: 10ms to allow batching
- **Compression**: Snappy compression for reduced network traffic
- **Max Poll Records**: 500 for high throughput
- **Idempotence**: Enabled for exactly-once semantics

### Monitoring

All environments expose Prometheus metrics at `/actuator/prometheus` for monitoring Kafka performance.

