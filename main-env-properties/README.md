# Main Service Environment Properties Module

This module manages environment-specific configuration files for the `main` service.

## Overview

Instead of managing multiple application.yml files across different deployment environments, this dedicated module centralizes all environment-specific configurations for the main service.

## Directory Structure

```
main-env-properties/
├── pom.xml
├── README.md
├── src/
│   └── main/
│       └── resources/
│           ├── application-dev.yml      (Development)
│           ├── application-staging.yml  (Staging)
│           ├── application-prod.yml     (Production)
│           └── application-test.yml     (Testing)
```

## Environments

### Development (dev)
- **File**: `application-dev.yml`
- **Database**: H2 in-memory database
- **Logging**: DEBUG level
- **SQL**: Shown with formatting
- **Use Case**: Local development and testing

### Staging (staging)
- **File**: `application-staging.yml`
- **Database**: MySQL on staging cluster
- **Logging**: INFO level
- **SQL**: Hidden (production-like)
- **Use Case**: Pre-production testing

### Production (prod)
- **File**: `application-prod.yml`
- **Database**: MySQL with connection pooling optimizations
- **Logging**: ERROR level with file rotation
- **Features**: SSL/TLS, Prometheus metrics, performance optimizations
- **Use Case**: Live production deployment

### Test (test)
- **File**: `application-test.yml`
- **Database**: H2 in-memory (created/dropped per test)
- **Logging**: DEBUG level for test output
- **Use Case**: Automated testing

## Usage

### 1. Add Dependency to Main Module

Update `main/pom.xml`:

```xml
<dependency>
    <groupId>snvn</groupId>
    <artifactId>main-env-properties</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Active Profile

Set the active Spring profile using one of these methods:

#### Option A: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar main-1.0-SNAPSHOT.jar
```

#### Option B: Command Line Argument
```bash
java -jar main-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

#### Option C: application.yml
```yaml
spring:
  profiles:
    active: prod
```

## Configuration Details

### Database Configuration by Environment

| Environment | Database | Driver | Connection |
|---|---|---|---|
| Dev | H2 (in-memory) | H2 | Direct |
| Staging | MySQL | MySQL 8 JDBC | staging-mysql:3306 |
| Prod | MySQL | MySQL 8 JDBC | prod-mysql:3306 |
| Test | H2 (in-memory, per-test) | H2 | Direct |

### Hibernate Configuration

| Feature | Dev | Staging | Prod | Test |
|---|---|---|---|---|
| DDL Auto | update | validate | validate | create-drop |
| Show SQL | true | false | false | true |
| Format SQL | true | false | false | true |
| Batch Size | default | default | 20 | default |
| Fetch Size | default | default | 50 | default |

### Server Configuration

- **Port**: 3081 (all environments)
- **Context Path**: /api
- **Compression**: Disabled (dev), Enabled (staging, prod)
- **SSL/TLS**: Production only

### Database Connection Pool (Staging & Production)

Production includes HikariCP optimizations:
- Maximum Pool Size: 20
- Minimum Idle: 5
- Connection Timeout: 30 seconds
- Idle Timeout: 10 minutes
- Max Lifetime: 30 minutes

### Logging Configuration

#### Development & Test
- Root Level: DEBUG
- Application Level: DEBUG
- SQL Logging: DEBUG with trace-level parameter binding
- Output: Console

#### Staging
- Root Level: WARN
- Application Level: INFO
- SQL Logging: OFF
- Output: Console

#### Production
- Root Level: ERROR
- Application Level: WARN
- SQL Logging: OFF
- File Output: /var/log/main-service/application.log
- Rotation: 100MB per file, 30-day retention

## Actuator Endpoints

### Development & Test
```
/actuator/health - Service health status
/actuator/info   - Application information
```

### Staging
```
/actuator/health  - Service health status
/actuator/info    - Application information
/actuator/metrics - Application metrics
```

### Production
```
/actuator/health              - Service health status (includes liveness/readiness)
/actuator/metrics             - Application metrics
/actuator/prometheus          - Prometheus-compatible metrics export
```

## Environment Variables

### Development
None required - uses embedded H2 database.

### Staging
```bash
SPRING_PROFILES_ACTIVE=staging
DB_USERNAME=staging_user
DB_PASSWORD=staging_password
```

### Production
```bash
SPRING_PROFILES_ACTIVE=prod
DB_USERNAME=prod_user
DB_PASSWORD=prod_password
SSL_KEYSTORE_PATH=/opt/ssl/main-service.p12
SSL_KEYSTORE_PASSWORD=your-keystore-password
```

### Test
```bash
SPRING_PROFILES_ACTIVE=test
```

## Docker Deployment Example

```dockerfile
FROM openjdk:21-slim
COPY main-1.0-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_USERNAME=app_user
ENV DB_PASSWORD=secure_password
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker run -e SPRING_PROFILES_ACTIVE=staging \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=pass \
  my-registry/main-service:latest
```

## Kubernetes Deployment Example

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: main-service-secrets
type: Opaque
stringData:
  DB_USERNAME: prod_user
  DB_PASSWORD: secure_password
  SSL_KEYSTORE_PASSWORD: keystore_pass
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: main-service
spec:
  template:
    spec:
      containers:
      - name: main-service
        image: my-registry/main-service:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: main-service-secrets
              key: DB_USERNAME
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: main-service-secrets
              key: DB_PASSWORD
        volumeMounts:
        - name: ssl-volume
          mountPath: /opt/ssl
          readOnly: true
      volumes:
      - name: ssl-volume
        secret:
          secretName: main-service-ssl
```

## Production Considerations

### Database Optimization
- Connection pooling configured for high concurrency
- Batch processing enabled for bulk operations
- Fetch size optimized for query performance

### Security
- SSL/TLS enabled for secure communication
- Database credentials from environment variables (never hardcoded)
- Password management through secret managers

### Monitoring & Observability
- Prometheus metrics for monitoring
- Health checks for liveness/readiness probes
- Structured logging for log aggregation

### Performance
- Response compression enabled
- Connection reuse through pooling
- Query batching and optimization

## Troubleshooting

### Database Connection Issues
1. Verify `DB_USERNAME` and `DB_PASSWORD` environment variables
2. Check database server is running and accessible
3. Review connection pool settings in application-prod.yml
4. Check logs for connection errors

### SSL/TLS Errors (Production)
1. Verify `SSL_KEYSTORE_PATH` and `SSL_KEYSTORE_PASSWORD`
2. Ensure keystore file exists and is readable
3. Validate certificate is not expired
4. Check certificate matches service hostname

### Query Performance Issues
1. Review SQL logging in development
2. Check HikariCP pool statistics
3. Monitor metrics endpoint in staging/production
4. Consider batch size adjustments in application-prod.yml

## Building

```bash
# Build this module
mvn clean install

# Build with specific profile
mvn clean install -Dspring.profiles.active=prod
```

## Integration with Main Service

The main service should include this module as a dependency and configure Spring profiles for active environment selection. This allows:

- **Centralized Configuration**: All environment configs in one place
- **Easy Updates**: Modify configs without rebuilding main service
- **Profile-based Activation**: Switch environments easily
- **Version Control**: Track configuration changes in Git

