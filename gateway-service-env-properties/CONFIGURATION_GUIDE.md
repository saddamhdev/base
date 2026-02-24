# Gateway Service Environment Properties - Configuration Guide

## Quick Start

### 1. Build the Module
```bash
cd D:\module project\base
mvn clean install
```

### 2. Activate an Environment Profile
Set the active profile when running the gateway-service:

```bash
# Development (default)
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=dev

# Staging
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=staging

# Production
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=prod

# Testing
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=test
```

## Module Structure

```
gateway-service-env-properties/
├── pom.xml                           # Maven configuration
├── README.md                         # Module documentation
├── .gitignore                        # Git ignore rules
└── src/main/resources/
    ├── application-dev.yml           # Development configuration
    ├── application-staging.yml       # Staging configuration
    ├── application-prod.yml          # Production configuration
    └── application-test.yml          # Test configuration
```

## Environment Details

### Development (application-dev.yml)
**Purpose**: Local development and testing

**Key Features**:
- Local service URLs (localhost)
- Debug logging enabled
- CORS allows all origins
- Eureka discovery disabled
- No SSL/TLS required

**When to use**: Developer workstations, local testing

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Staging (application-staging.yml)
**Purpose**: Pre-production environment testing

**Key Features**:
- Internal service URLs (staging-*)
- Info level logging for application
- Limited CORS origins (staging domain)
- Eureka discovery enabled
- Compression enabled
- No SSL/TLS (can be added if needed)

**When to use**: QA testing, integration testing, client demonstrations

```bash
docker run -e SPRING_PROFILES_ACTIVE=staging gateway-service:latest
```

### Production (application-prod.yml)
**Purpose**: Live production deployment

**Key Features**:
- HTTPS service URLs
- Error level logging (minimal logs)
- Restricted CORS origins (production domain)
- Eureka discovery enabled with clustering
- SSL/TLS mandatory
- Circuit breaker protection
- Prometheus metrics enabled
- Log file rotation (100MB files, 30-day retention)
- Health check and metrics endpoints

**When to use**: Production deployment

**Prerequisites**:
- SSL keystore configured
- Environment variables set:
  - `SSL_KEYSTORE_PATH`: Path to keystore
  - `SSL_KEYSTORE_PASSWORD`: Keystore password

```bash
export SPRING_PROFILES_ACTIVE=prod
export SSL_KEYSTORE_PATH=/opt/ssl/gateway.p12
export SSL_KEYSTORE_PASSWORD=your-password
java -jar gateway-service-1.0-SNAPSHOT.jar
```

### Test (application-test.yml)
**Purpose**: Automated testing environment

**Key Features**:
- Local service URLs (localhost)
- Debug logging for detailed test output
- CORS allows localhost
- Eureka discovery disabled
- No SSL/TLS required

**When to use**: Unit tests, integration tests

## Configuration Layers

Configurations are applied in this order (later overrides earlier):

1. **application.yml** (Base configuration in gateway-service)
   - Application name
   - Default profile (dev)
   - Config import directive

2. **application-{profile}.yml** (Environment-specific)
   - All environment-specific settings
   - Routes, CORS, logging, server config, etc.

3. **Environment Variables**
   - Can override any yaml property
   - Example: `SPRING_CLOUD_GATEWAY_ROUTES_0_URI=http://custom-url`

4. **Command Line Arguments**
   - Highest priority
   - Example: `--spring.profiles.active=prod`

## Gateway Routes Configuration

All environments define three main routes:

### 1. User Service Route
```
Pattern: /api/users/**
Behavior: Rewrites path to /api/{segment}, forwards to user-service
Header: X-Service: user-service
```

### 2. Config Service Route
```
Pattern: /config/**
Behavior: Forwards to config-service
Header: X-Service: config-service
```

### 3. Main Service Route
```
Pattern: /**
Behavior: Catch-all route to main-service
Header: X-Service: main-service
```

## CORS Configuration by Environment

### Development
```yaml
allowedOrigins: "*"          # All origins
allowedMethods: All          # GET, POST, PUT, DELETE, OPTIONS
allowedHeaders: "*"          # All headers
allowCredentials: false      # No credentials
```

### Staging
```yaml
allowedOrigins: "https://staging.yourdomain.com"
allowedMethods: All
allowedHeaders: "*"
allowCredentials: true       # Credentials allowed
maxAge: 7200 seconds
```

### Production
```yaml
allowedOrigins: "https://yourdomain.com,https://www.yourdomain.com"
allowedMethods: All
allowedHeaders: "*"
allowCredentials: true
maxAge: 86400 seconds        # 24 hours
```

## Service URLs Reference

| Route | Dev | Staging | Prod |
|-------|-----|---------|------|
| User Service | http://localhost:8081 | http://staging-user-service:8081 | https://user-service.yourdomain.com |
| Config Service | http://localhost:8888 | http://staging-config-service:8888 | https://config-service.yourdomain.com |
| Main Service | http://localhost:3081 | http://staging-main-service:3081 | https://main-service.yourdomain.com |

## Logging Configuration by Environment

### Development & Test
```
Root Level: DEBUG
Application (snvn.*): DEBUG
Pattern: Simple timestamp and message
```

### Staging
```
Root Level: WARN
Application (snvn.*): INFO
Pattern: Full logging with thread and class info
```

### Production
```
Root Level: ERROR
Application (snvn.*): WARN
File: /var/log/gateway-service/application.log
Rotation: 100MB per file, 30-day history
Pattern: Full logging format
```

## Eureka Service Discovery

### Development & Test
- Disabled
- Direct service URLs used

### Staging & Production
- Enabled
- Services registered/discovered via Eureka
- Staging: Single Eureka server
- Production: Eureka cluster for high availability

## SSL/TLS Configuration

### Production Only
The production profile includes SSL configuration:

```yaml
server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

**Setup Steps**:
1. Generate a PKCS12 keystore
2. Set environment variables
3. Ensure certificate is valid and trusted

## Actuator & Monitoring Endpoints

### Development & Staging
- Limited endpoints available
- Basic health and info

### Production
```
Enabled Endpoints:
- /actuator/health       (Service health status)
- /actuator/metrics      (Application metrics)
- /actuator/prometheus   (Prometheus-compatible metrics)
```

## Circuit Breaker (Production Only)

Production routes include circuit breaker filters for fault tolerance:

```yaml
- CircuitBreaker=user-service-cb
- CircuitBreaker=config-service-cb
- CircuitBreaker=main-service-cb
```

**Benefits**:
- Prevents cascading failures
- Automatic fallback handling
- Improves system resilience

## Environment Variable Examples

### Development
```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_CLOUD_GATEWAY_ROUTES_0_URI=http://localhost:8081
```

### Production
```bash
SPRING_PROFILES_ACTIVE=prod
SSL_KEYSTORE_PATH=/opt/ssl/gateway.p12
SSL_KEYSTORE_PASSWORD=secure-password
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka1:8761/eureka/,http://eureka2:8761/eureka/
```

## Docker Deployment Example

```dockerfile
FROM openjdk:21-slim
COPY gateway-service-1.0-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV SSL_KEYSTORE_PATH=/opt/ssl/gateway.p12
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker run -e SPRING_PROFILES_ACTIVE=staging my-registry/gateway-service:1.0
```

## Kubernetes Deployment Example

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-config
data:
  SPRING_PROFILES_ACTIVE: prod
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
spec:
  template:
    spec:
      containers:
      - name: gateway
        image: my-registry/gateway-service:1.0
        envFrom:
        - configMapRef:
            name: gateway-config
        env:
        - name: SSL_KEYSTORE_PATH
          value: /etc/ssl/gateway.p12
```

## Troubleshooting

### Issue: Configuration not loading
**Check**:
1. Profile name matches file: `application-{profile}.yml`
2. `SPRING_PROFILES_ACTIVE` is set correctly
3. Dependency on `gateway-service-env-properties` is declared

### Issue: Routes not working
**Check**:
1. Service URLs are correct for the environment
2. Services are running and accessible
3. Gateway logs for routing errors

### Issue: CORS errors
**Check**:
1. Browser origin matches allowed origins
2. Request method is in allowed methods
3. Headers are in allowed headers list

### Issue: SSL errors (Production)
**Check**:
1. Keystore path exists and is readable
2. Password is correct
3. Certificate is valid and not expired
4. Environment variables are set

## Adding a New Environment

1. Create `application-{newenv}.yml` with appropriate config
2. Copy from existing template (dev, staging, prod)
3. Update service URLs and settings
4. Run: `java -jar gateway-service.jar --spring.profiles.active={newenv}`

## Best Practices

✅ **Do**:
- Use environment-specific profiles for different deployments
- Keep sensitive data in environment variables
- Use Eureka in production for service discovery
- Enable logging rotation in production
- Use HTTPS in production
- Restrict CORS origins in production
- Monitor metrics and health checks

❌ **Don't**:
- Hardcode URLs in application.yml
- Use debug logging in production
- Allow all CORS origins in production
- Disable SSL in production
- Commit sensitive credentials
- Use same config for all environments

