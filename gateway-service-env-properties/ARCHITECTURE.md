# Gateway Service Environment Properties - Architecture

## Module Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Parent Project (base)                       │
│  pom.xml (lists all modules including env-properties)           │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ├─────────────────────────────────────┐
                  │                                     │
        ┌─────────▼──────────┐            ┌────────────▼─────────┐
        │  gateway-service   │            │ gateway-service-env  │
        │  (main application)│◄───────────┤ properties (configs) │
        │                    │  depends   │ (NEW MODULE)         │
        └─────────┬──────────┘  on        └────────────┬─────────┘
                  │                                     │
         ┌────────▼────────┐              ┌────────────▼─────────┐
         │ application.yml │              │  src/main/resources  │
         │ (simplified)    │              │                      │
         └─────────────────┘              ├─ app-dev.yml        │
                                          ├─ app-staging.yml    │
                                          ├─ app-prod.yml       │
                                          └─ app-test.yml       │
                                          └────────────┬─────────┘
                                                       │
                                        ┌──────────────┼─────────────┐
                                        │              │             │
                           ┌────────────▼┐  ┌─────────▼──┐  ┌──────▼───┐
                           │ Development │  │  Staging   │  │Production│
                           │ (localhost) │  │(staging-*) │  │(https)   │
                           │ Eureka: OFF │  │Eureka: ON  │  │Eureka:ON │
                           │ Debug logs  │  │Info logs   │  │SSL, Prom │
                           └─────────────┘  └────────────┘  └──────────┘
```

## Runtime Profile Selection

```
Application Startup
         │
         ├─ Check SPRING_PROFILES_ACTIVE environment variable
         ├─ Check --spring.profiles.active command line argument
         ├─ Check spring.profiles.active in application.yml
         │
         ▼
    Select Profile (dev/staging/prod/test)
         │
         ▼
    Spring Config Import: classpath*:application-*.yml
         │
         ▼
    Load application-{active-profile}.yml
         │
         ▼
    Gateway Service Configured
         │
         ├─ Routes configured
         ├─ CORS configured
         ├─ Logging configured
         ├─ Eureka configured (if enabled)
         └─ SSL/TLS configured (if enabled)
         │
         ▼
    Gateway Service Started ✅
```

## Configuration Layer Stack

```
┌──────────────────────────────────┐  ← Highest Priority
│   Command Line Arguments         │  (--spring.profiles.active=prod)
├──────────────────────────────────┤
│   Environment Variables          │  (SPRING_PROFILES_ACTIVE=prod)
├──────────────────────────────────┤
│   application-{profile}.yml      │  (application-prod.yml)
│   (from env-properties module)   │
├──────────────────────────────────┤
│   application.yml                │
│   (from gateway-service)         │
├──────────────────────────────────┤
│   Default Spring Config          │  ← Lowest Priority
└──────────────────────────────────┘
```

## File Organization

```
gateway-service-env-properties/
│
├── 📄 pom.xml
│   └─ Maven module configuration
│   └─ Defines JAR packaging with config classifier
│   └─ Enables resource filtering for yml files
│
├── 📁 src/main/resources/
│   │
│   ├── 📋 application-dev.yml
│   │   └─ Development environment
│   │   └─ localhost services
│   │   └─ Debug logging
│   │
│   ├── 📋 application-staging.yml
│   │   └─ Staging environment
│   │   └─ staging-* services
│   │   └─ Info logging
│   │
│   ├── 📋 application-prod.yml
│   │   └─ Production environment
│   │   └─ Domain HTTPS services
│   │   └─ Error logging with file rotation
│   │   └─ SSL/TLS enabled
│   │   └─ Prometheus metrics
│   │
│   └── 📋 application-test.yml
│       └─ Test environment
│       └─ localhost services
│       └─ Debug logging
│
├── 📚 Documentation/
│   ├── 📖 README.md
│   │   └─ Module overview and usage
│   │
│   ├── 📖 CONFIGURATION_GUIDE.md
│   │   └─ Detailed configuration walkthrough
│   │   └─ Environment comparisons
│   │   └─ Deployment examples
│   │
│   ├── 📖 QUICK_REFERENCE.md
│   │   └─ Quick start and common commands
│   │
│   ├── 📖 SUMMARY.md
│   │   └─ Module summary and benefits
│   │
│   └── 📖 ARCHITECTURE.md (this file)
│       └─ Architecture and design diagrams
│
└── 🚫 .gitignore
    └─ Build artifacts and IDE files
```

## Gateway Routes Architecture

```
Incoming Request
        │
        ▼
    Gateway Service (Port 8080)
    Context: /gateway
        │
        ├─────────────────┬──────────────┬──────────────┐
        │                 │              │              │
        ▼                 ▼              ▼              ▼
    Routing Decision (Spring Cloud Gateway)
        │
        ├─ Path: /api/users/**      ├─ Path: /config/**      ├─ Path: /**
        │                           │                        │
        ▼                           ▼                        ▼
    User Service Route         Config Service Route    Main Service Route
    RewritePath               Direct Forward            Direct Forward
    /api/users → /api         No path modification      No path modification
        │                           │                        │
        ├─ Filter:                  ├─ Filter:               ├─ Filter:
        │  AddRequestHeader         │  AddRequestHeader      │  AddRequestHeader
        │  X-Service: user-service  │  X-Service: config     │  X-Service: main
        │                           │                        │
        ▼                           ▼                        ▼
    [User Service]            [Config Service]       [Main Service]
    localhost:8081            localhost:8888         localhost:3081
    (dev/test)                (dev/test)             (dev/test)
```

## CORS Handling Flow

```
Browser Request
        │
        ▼
    Check Origin Header
        │
        ├─ If Preflight (OPTIONS)
        │   │
        │   ▼
        │   Check Allowed Origins (from config)
        │   │
        │   ├─ Match Found
        │   │   └─ Return CORS Headers
        │   │
        │   └─ No Match
        │       └─ Return 403 Forbidden
        │
        └─ If Regular Request
            │
            ▼
            Check CORS Headers
            │
            ├─ Valid → Forward to Service
            │
            └─ Invalid → Return 403 Forbidden
```

## Environment-Specific Service URLs

```
Development (localhost)
├─ User Service:    http://localhost:8081
├─ Config Service:  http://localhost:8888
└─ Main Service:    http://localhost:3081

Staging (staging cluster)
├─ User Service:    http://staging-user-service:8081
├─ Config Service:  http://staging-config-service:8888
└─ Main Service:    http://staging-main-service:3081

Production (production cluster)
├─ User Service:    https://user-service.yourdomain.com
├─ Config Service:  https://config-service.yourdomain.com
└─ Main Service:    https://main-service.yourdomain.com

Testing (localhost)
├─ User Service:    http://localhost:8081
├─ Config Service:  http://localhost:8888
└─ Main Service:    http://localhost:3081
```

## Logging Architecture

```
Gateway Service
    │
    ├─ Appender: Console (all environments)
    │   └─ Pattern: [timestamp] [level] [logger] - message
    │
    └─ Appender: File (production only)
        └─ Path: /var/log/gateway-service/application.log
        └─ Size: 100MB per file
        └─ Retention: 30 days
        └─ Rotation: Automatic when max size reached

Log Level Configuration
┌─────────────┬────────────┬─────────────┬────────────┐
│ Environment │ Root Level │ App Level   │ Output     │
├─────────────┼────────────┼─────────────┼────────────┤
│ Dev         │ DEBUG      │ DEBUG       │ Console    │
│ Staging     │ WARN       │ INFO        │ Console    │
│ Prod        │ ERROR      │ WARN        │ Console+File│
│ Test        │ DEBUG      │ DEBUG       │ Console    │
└─────────────┴────────────┴─────────────┴────────────┘
```

## Eureka Service Discovery Integration

```
Production Environment
    │
    ├─ Gateway Service
    │   └─ Eureka Client: Enabled
    │       └─ Registers with Eureka at startup
    │       └─ Heartbeat every 30 seconds
    │       └─ Discovers other services from Eureka
    │
    ├─ User Service
    │   └─ Registers with Eureka
    │       └─ Service Name: user-service
    │
    ├─ Config Service
    │   └─ Registers with Eureka
    │       └─ Service Name: config-service
    │
    └─ Main Service
        └─ Registers with Eureka
            └─ Service Name: main-service

Eureka Cluster
    ├─ Eureka Server 1
    ├─ Eureka Server 2
    └─ Eureka Server 3
```

## Build Process

```
mvn clean install
    │
    ├─ Build gateway-service-env-properties
    │   │
    │   ├─ Compile: (no Java code)
    │   ├─ Resource Filter: application-*.yml
    │   ├─ Package: JAR with classifier=config
    │   │   └─ JAR contains all *.yml files
    │   └─ Install: To local repository
    │
    └─ Build gateway-service
        │
        ├─ Resolve Dependency: gateway-service-env-properties
        │   └─ Include JAR in classpath
        ├─ Compile: Java source code
        ├─ Copy Resources: application.yml + imported configs
        ├─ Package: Executable JAR
        └─ Install: To local repository
```

## SSL/TLS Configuration (Production)

```
Production Configuration
    │
    ├─ SSL Enabled: true
    ├─ Keystore Path: ${SSL_KEYSTORE_PATH}
    │   └─ Example: /opt/ssl/gateway.p12
    ├─ Keystore Password: ${SSL_KEYSTORE_PASSWORD}
    │   └─ From environment variable (never hardcoded)
    └─ Keystore Type: PKCS12

Request Flow (HTTPS)
    │
    ├─ Client → SSL/TLS Handshake → Gateway (Port 8080)
    │   └─ Certificate verified from keystore
    │
    └─ Gateway → Service (HTTP or HTTPS)
        └─ Depends on service URL in config
```

## Dependency Injection Map

```
Parent POM
    ├─ Spring Boot Dependencies (3.3.2)
    ├─ Spring Cloud Dependencies (2023.0.3)
    │
    └─ gateway-service-env-properties
        └─ No dependencies (config only)

gateway-service
    ├─ Depends on: gateway-service-env-properties
    │   └─ Gets config files at runtime
    ├─ spring-cloud-starter-gateway
    ├─ spring-boot-starter-actuator
    └─ Other Spring Boot starters (from BOM)
```

---

## Summary

The **gateway-service-env-properties** module provides:

1. **Centralized Configuration** - All environment configs in one module
2. **Profile-Based Management** - Switch environments at runtime
3. **Environment Isolation** - Separate configs for dev/staging/prod/test
4. **Production Ready** - SSL/TLS, metrics, logging rotation, circuit breaker
5. **Easy Extensibility** - Add new environments by creating new yml files
6. **Version Controlled** - Track all configuration changes in Git

The architecture supports microservice patterns including service discovery, circuit breaking, CORS handling, and comprehensive monitoring.

