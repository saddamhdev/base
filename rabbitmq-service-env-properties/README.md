# RabbitMQ Service Environment Properties

This module contains environment-specific configuration files for the `rabbitmq-service`.

## Overview

Environment-specific properties are externalized to support different configurations across development, test, staging, and production environments.

## Available Profiles

### 1. Development (`application-dev.yml`)
- **Database**: H2 in-memory database
- **RabbitMQ**: Local instance (localhost:5672)
- **Server Port**: 8085
- **Logging**: DEBUG level for RabbitMQ service
- **H2 Console**: Enabled
- **Features**:
  - Auto-acknowledgment mode
  - Simple retry configuration (3 attempts)
  - Standard queue names

### 2. Test (`application-test.yml`)
- **Database**: H2 in-memory database
- **RabbitMQ**: Local instance with test-specific queues
- **Server Port**: 8085
- **Logging**: INFO level with DEBUG for service
- **Features**:
  - Test-specific queue/exchange names
  - Simplified retry (2 attempts)
  - DDL auto mode: create-drop

### 3. Staging (`application-staging.yml`)
- **Database**: PostgreSQL on staging server
- **RabbitMQ**: Staging RabbitMQ cluster
- **Server Port**: 8085
- **Logging**: WARN level with file logging
- **Features**:
  - Manual acknowledgment mode
  - Advanced retry configuration (5 attempts, exponential backoff)
  - Virtual host: `/staging`
  - Connection pooling (max: 10)
  - Dead Letter Queue (DLQ) support
  - Prometheus metrics enabled
  - Environment variables for credentials

### 4. Production (`application-prod.yml`)
- **Database**: PostgreSQL production cluster
- **RabbitMQ**: Production RabbitMQ cluster
- **Server Port**: 8085
- **Logging**: ERROR level with structured logging
- **Features**:
  - SSL enabled for RabbitMQ
  - Manual acknowledgment mode
  - Enhanced retry (10 attempts, exponential backoff)
  - Virtual host: `/production`
  - Optimized connection pooling (max: 20)
  - DLQ with production-specific names
  - SSL/TLS for server
  - Prometheus metrics
  - Batch processing optimization
  - File logging with rotation (100MB, 30 days)

## Configuration Details

### RabbitMQ Properties

Each environment defines:
- **Queue Name**: Environment-specific queue names
- **Exchange Name**: Environment-specific exchange names
- **Routing Key**: Environment-specific routing keys
- **Dead Letter Queue (DLQ)**: Separate DLQ configuration per environment

### Environment Variables

Production and Staging require the following environment variables:

```bash
# Database
DB_USERNAME=<database_username>
DB_PASSWORD=<database_password>

# RabbitMQ
RABBITMQ_USERNAME=<rabbitmq_username>
RABBITMQ_PASSWORD=<rabbitmq_password>

# SSL (Production only)
SSL_KEYSTORE_PATH=<path_to_keystore>
SSL_KEYSTORE_PASSWORD=<keystore_password>
```

## Usage

To use a specific profile, set the Spring active profile:

```bash
# Development
java -jar rabbitmq-service.jar --spring.profiles.active=dev

# Test
java -jar rabbitmq-service.jar --spring.profiles.active=test

# Staging
java -jar rabbitmq-service.jar --spring.profiles.active=staging

# Production
java -jar rabbitmq-service.jar --spring.profiles.active=prod
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

Output: `rabbitmq-service-env-properties-1.0-SNAPSHOT-config.jar`

## Integration

The rabbitmq-service should include this module as a dependency to access environment-specific configurations.

