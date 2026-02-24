# Gateway Service Environment Properties Module

This module manages environment-specific configuration files for the `gateway-service`.

## Overview

Instead of managing multiple application.yml files across different deployment environments, this dedicated module centralizes all environment-specific configurations.

## Directory Structure

```
gateway-service-env-properties/
├── pom.xml
├── src/
│   └── main/
│       └── resources/
│           ├── application-dev.yml      (Development)
│           ├── application-staging.yml  (Staging)
│           ├── application-prod.yml     (Production)
│           └── application-test.yml     (Testing)
└── README.md (this file)
```

## Environments

### Development (dev)
- **File**: `application-dev.yml`
- **Services**: Local services (localhost)
- **Logging**: DEBUG level
- **Eureka**: Disabled
- **Use Case**: Local development and testing

### Staging (staging)
- **File**: `application-staging.yml`
- **Services**: Staging environment services
- **Logging**: INFO level
- **Eureka**: Enabled
- **Use Case**: Pre-production testing

### Production (prod)
- **File**: `application-prod.yml`
- **Services**: Production domain services (HTTPS)
- **Logging**: ERROR level
- **Eureka**: Enabled with clustering
- **Security**: SSL/TLS enabled
- **Use Case**: Live production deployment

### Test (test)
- **File**: `application-test.yml`
- **Services**: Local test services
- **Logging**: DEBUG level
- **Eureka**: Disabled
- **Use Case**: Automated testing

## Usage

### 1. Add Dependency to Gateway Service

Update `gateway-service/pom.xml`:

```xml
<dependency>
    <groupId>snvn</groupId>
    <artifactId>gateway-service-env-properties</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Active Profile

Set the active Spring profile using one of these methods:

#### Option A: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar gateway-service-1.0-SNAPSHOT.jar
```

#### Option B: Command Line Argument
```bash
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

#### Option C: application.yml
```yaml
spring:
  profiles:
    active: prod
```

#### Option D: Docker Environment Variable
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
```

### 3. Copy Configuration to Gateway Service

You can either:
- Include this module as a dependency (recommended)
- Copy the yml files to `gateway-service/src/main/resources/`
- Use Maven resource filtering to include files during build

## Configuration Details

### Common Routes Across All Environments

- **User Service Route** (`/api/users/**`): Routes to user microservice
- **Config Service Route** (`/config/**`): Routes to config microservice
- **Main Service Route** (`/**`): Default route to main microservice

### Service URLs by Environment

| Environment | User Service | Config Service | Main Service |
|-------------|--------------|----------------|--------------|
| dev        | localhost:8081 | localhost:8888 | localhost:3081 |
| staging    | staging-user-service:8081 | staging-config-service:8888 | staging-main-service:3081 |
| prod       | user-service.yourdomain.com | config-service.yourdomain.com | main-service.yourdomain.com |
| test       | localhost:8081 | localhost:8888 | localhost:3081 |

## CORS Configuration

### Development & Test
- Allows all origins: `"*"`
- Credentials disabled

### Staging
- Limited origins: `https://staging.yourdomain.com`
- Credentials enabled

### Production
- Restricted origins: `https://yourdomain.com,https://www.yourdomain.com`
- Credentials enabled
- Maximum age: 86400 seconds (1 day)

## Logging Configuration

### Development & Test
- **Root Level**: DEBUG
- **Application Level**: DEBUG

### Staging
- **Root Level**: WARN
- **Application Level**: INFO

### Production
- **Root Level**: ERROR
- **Application Level**: WARN
- **File Logging**: Enabled with rotation
- **Log Path**: `/var/log/gateway-service/application.log`
- **Max File Size**: 100MB
- **Retention**: 30 days

## Customization

To add a new environment (e.g., `application-custom.yml`):

1. Create a new file: `src/main/resources/application-custom.yml`
2. Configure the appropriate service URLs and settings
3. Activate using: `--spring.profiles.active=custom`

## Production Considerations

### SSL/TLS Configuration
The production profile expects:
- `SSL_KEYSTORE_PATH`: Path to the keystore file
- `SSL_KEYSTORE_PASSWORD`: Keystore password

### Metrics & Monitoring
- Actuator endpoints enabled: health, metrics, prometheus
- Prometheus export enabled for monitoring

### Circuit Breaker
Production routes include circuit breaker filters for fault tolerance.

## Troubleshooting

### Configuration Not Loading
1. Check the `SPRING_PROFILES_ACTIVE` environment variable
2. Verify the active profile in `application.yml`
3. Ensure the environment-specific file exists
4. Check Spring Cloud Gateway route configuration syntax

### Service Route Issues
1. Verify service URLs match your environment
2. Check network connectivity to backend services
3. Review gateway logs for routing errors

## Building

```bash
# Build this module
mvn clean install

# Build with specific profile
mvn clean install -Dspring.profiles.active=prod
```

## Integration with Gateway Service

The gateway-service should include this module as a dependency and configure Spring profiles for active environment selection. This allows:

- **Centralized Configuration**: All environment configs in one place
- **Easy Updates**: Modify configs without rebuilding gateway-service
- **Profile-based Activation**: Switch environments easily
- **Version Control**: Track configuration changes in Git

