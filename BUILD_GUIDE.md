# Build and Compilation Issues - Resolution Guide

## Issues Found and Fixed

### 1. **Core-Common Module Configuration** ✅ FIXED
**Problem:** `core-common` module was missing proper packaging configuration, causing dependency resolution failures.

**Solution Applied:**
- Added `<packaging>jar</packaging>` to core-common/pom.xml
- Added Maven compiler plugin to the build section
- This allows core-common to be properly installed to local Maven repository

### 2. **KafkaTemplate Autowiring Issues** ✅ FIXED
**Problem:** Transaction-service and auth-service were trying to autowire `KafkaTemplate<String, Object>` but no bean was configured.

**Solution Applied:**
Created Kafka configuration classes:
- `transaction-service/src/main/java/snvn/transactionservice/config/KafkaConfig.java`
- `auth-service/src/main/java/snvn/authservice/config/KafkaConfig.java`

Both files configure:
- ProducerFactory with String key serializer and JSON value serializer
- KafkaTemplate bean for dependency injection
- Added `JsonSerializer.ADD_TYPE_INFO_HEADERS = false` to avoid deprecated warnings

## How to Build the Project

### Option 1: Build Everything from Root (Recommended)
```bash
cd "D:\module project\base"
mvn clean install -DskipTests
```

This will build all modules in the correct dependency order:
1. model
2. core-common
3. All service modules (user-service, auth-service, account-service, etc.)
4. Config and gateway services

### Option 2: Build Individual Modules
If you need to build specific modules, follow this order:

```bash
# 1. Build shared libraries first
cd "D:\module project\base\model"
mvn clean install -DskipTests

cd "D:\module project\base\core-common"
mvn clean install -DskipTests

# 2. Then build individual services
cd "D:\module project\base\transaction-service"
mvn clean package

cd "D:\module project\base\auth-service"
mvn clean package
```

### Option 3: Using IntelliJ IDEA
1. Open the Maven tool window (View → Tool Windows → Maven)
2. Navigate to the root project (base)
3. Click on Lifecycle → clean
4. Click on Lifecycle → install
5. This will build all modules in order

## Remaining Warnings (Non-Critical)

### CVE Security Warnings
The following are security vulnerability warnings in transitive dependencies:
- logback-core CVE-2026-1225
- lz4-java CVE-2025-66566
- assertj-core CVE-2026-24400
- log4j-core CVE-2025-68161
- commons-beanutils CVE-2025-48734

**Note:** These are warnings, not compilation errors. They can be addressed by:
1. Updating Spring Boot version when patches are available
2. Explicitly overriding vulnerable dependency versions
3. Using Maven dependency exclusions

### IntelliJ Indexing
If IntelliJ still shows "Could not autowire" errors after creating KafkaConfig:
1. Click "File → Invalidate Caches → Invalidate and Restart"
2. Or click "Maven → Reload All Maven Projects" (the refresh icon)
3. Or "Build → Rebuild Project"

## Module Creation Summary

The following modules have been successfully created:
1. ✅ **core-common** - Shared utilities module
2. ✅ **notification-service** - Handles notifications
3. ✅ **account-service** - Manages user accounts
4. ✅ **audit-service** - Tracks system events and changes
5. ✅ **transaction-service** - Handles financial transactions (with Kafka events)
6. ✅ **auth-service** - Authentication and authorization with JWT

All modules include:
- Spring Boot configuration
- REST controllers
- Service layer
- Repository layer (JPA)
- Kafka integration (producers/consumers)
- H2 database configuration
- Actuator endpoints
- Proper Maven configuration

## Next Steps

1. **Build the project** using one of the methods above
2. **Run individual services** after successful build
3. **Configure Kafka** (ensure Kafka is running on localhost:9092)
4. **Test endpoints** using the health check endpoints
5. **Review and update** JWT secret in auth-service/application.yml for production use

## Troubleshooting

### "Cannot find symbol" errors
- Usually indicates missing dependency or import
- Run `mvn clean install` from root to ensure all modules are built
- Check if the class exists in the expected package

### "Could not resolve dependencies"
- Build parent modules first (model, core-common)
- Use `mvn clean install` instead of `mvn package` to install to local repo

### Kafka connection errors at runtime
- Ensure Kafka is running: `docker-compose up -d` (if using Docker)
- Or update `spring.kafka.bootstrap-servers` in application.yml

