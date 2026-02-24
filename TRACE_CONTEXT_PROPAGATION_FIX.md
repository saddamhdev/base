# Trace Context Propagation Fix Documentation

## Issue Summary
**Problem:** `traceId`, `spanId`, and `correlationId` values were not matching between gateway-service, main-service, and downstream services (user-service, account-service, etc.).

**Root Causes Identified:**
1. Main-service WebClient beans were not forwarding trace headers to downstream services
2. Reactor thread context was losing MDC values when switching threads

---

## Investigation

### Symptom Analysis
From gateway-service log:
```json
{"traceId":"66a7f9b2951a49e79a22a7f0cf0201b8", "correlationId":"f4c2970c-c755-4323-94d4-4b49cdf5ab36"}
```

From user-service log:
```json
{"traceId":"ae080dd0b22740e5a35d8bec77cc6a6c", "correlationId":"1f4275a0-269e-426a-b260-c9c46cbf2260"}
```

**Observation:** The trace IDs were completely different, indicating headers were not being propagated.

### Request Flow Analysis

```
Client
   │
   ▼
Gateway Service (port 8080)
   │ ← Generates traceId, correlationId
   │ ← Sets headers: X-Trace-Id, X-Correlation-Id, X-B3-TraceId, etc.
   │
   ▼
Main Service (port 8081)
   │ ← MdcLoggingFilter extracts headers ✓
   │ ← Sets MDC values ✓
   │ ← WebClient calls downstream services ✗ (headers NOT forwarded)
   │
   ▼
User Service (port 8089)
   │ ← MdcLoggingFilter finds NO headers
   │ ← Generates NEW traceId (WRONG!)
```

---

## Solution Implementation

### Fix 1: WebClient Header Propagation Filter

**File:** `main/src/main/java/snvn/config/ServiceClientConfiguration.java`

Added a filter that reads trace context from MDC and adds headers to outgoing WebClient requests:

```java
private ExchangeFilterFunction traceHeaderPropagationFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");
        String correlationId = MDC.get("correlationId");

        ClientRequest.Builder requestBuilder = ClientRequest.from(clientRequest);

        if (traceId != null && !traceId.isBlank()) {
            requestBuilder.header("X-Trace-Id", traceId);
            requestBuilder.header("X-B3-TraceId", traceId);
        }
        if (spanId != null && !spanId.isBlank()) {
            requestBuilder.header("X-Span-Id", spanId);
            requestBuilder.header("X-B3-SpanId", spanId);
        }
        if (correlationId != null && !correlationId.isBlank()) {
            requestBuilder.header("X-Correlation-Id", correlationId);
        }

        return Mono.just(requestBuilder.build());
    });
}
```

Applied to all WebClient beans:
```java
@Bean
public WebClient userServiceClient() {
    return WebClient.builder()
        .baseUrl("http://localhost:8089")
        .filter(traceHeaderPropagationFilter())
        .build();
}
```

### Fix 2: Add core-common Dependency to main-service

**File:** `main/pom.xml`

```xml
<dependency>
    <groupId>snvn</groupId>
    <artifactId>core-common</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

### Fix 3: Add Component Scan

**File:** `main/src/main/java/snvn/MainApplication.java`

```java
@SpringBootApplication
@ComponentScan(basePackages = {"snvn", "snvn.common"})
public class MainApplication {
    // ...
}
```

### Fix 4: Enhanced MdcLoggingFilter Debugging

**File:** `core-common/src/main/java/snvn/common/filter/MdcLoggingFilter.java`

Added diagnostic logging to show whether values came from headers:
```java
log.debug("MDC context set - traceId={}, spanId={}, correlationId={} [fromHeaders: trace={}, span={}, correlation={}]",
        traceId, spanId, correlationId, traceIdFromHeader, spanIdFromHeader, correlationIdFromHeader);
```

---

## Remaining Issue: Reactor Thread Context

### Problem
In the main-service log, we see:
```
Line 304: traceId="220514e6..." (http-nio-8081-exec-1) ✓
Line 336: traceId="" (reactor-http-nio-2) ✗
```

When the reactor thread (reactor-http-nio-2) handles the WebClient response, the MDC context is lost because MDC is thread-local and doesn't automatically propagate to reactor threads.

### Solution Implemented: Reactor Context Propagation

**File:** `main/pom.xml` - Added dependency:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>context-propagation</artifactId>
</dependency>
```

**File:** `main/src/main/java/snvn/config/ReactorContextPropagationConfig.java` - Created:
```java
@Configuration
public class ReactorContextPropagationConfig {

    @PostConstruct
    public void enableContextPropagation() {
        // Enable automatic context propagation for Reactor
        // This ensures MDC values are propagated to reactor threads
        Hooks.enableAutomaticContextPropagation();
    }
}
```

This enables automatic MDC context propagation across reactor thread boundaries.
```

---

## Files Modified

| File | Change |
|------|--------|
| `gateway-service/.../ApplicationDetailsGlobalFilter.java` | Fixed traceId generation, changed to HIGHEST_PRECEDENCE |
| `core-common/.../filter/MdcLoggingFilter.java` | Created new filter + enhanced debugging |
| `core-common/.../exception/GlobalExceptionHandler.java` | Added traceId/spanId to error responses |
| `main/pom.xml` | Added core-common, main-env-properties, and context-propagation dependencies |
| `main/.../MainApplication.java` | Added @ComponentScan and @ConfigurationPropertiesScan |
| `main/.../config/ServiceClientConfiguration.java` | YAML-based config with @ConfigurationProperties + trace header propagation filter |
| `main/.../config/ReactorContextPropagationConfig.java` | **NEW** - Enables MDC propagation to reactor threads |
| `main-env-properties/application-dev.yml` | Added services.* URLs + updated logging pattern |
| `main-env-properties/application-prod.yml` | Added services.* URLs with env vars + updated logging pattern |
| `main-env-properties/application-staging.yml` | Added services.* URLs with env vars + updated logging pattern |
| `main-env-properties/application-test.yml` | Added services.* URLs for test env + updated logging pattern |
| All service `*Application.java` files | Added @ComponentScan for snvn.common |
| All service `application.yml` files | Added traceId/spanId to log pattern |
| `kafka-service/pom.xml` | Added core-common dependency |
| `rabbitmq-service/pom.xml` | Added core-common dependency |

---

## YAML-Based Service URL Configuration

Service URLs are now externalized to YAML files using `@ConfigurationProperties`.

### Configuration Class

```java
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceClientConfiguration {
    private String userServiceUrl;
    private String authServiceUrl;
    // ... other service URLs
    
    // Setters for property binding
    public void setUserServiceUrl(String url) { this.userServiceUrl = url; }
    // ...
}
```

### YAML Configuration (application-dev.yml)

```yaml
services:
  user-service-url: http://localhost:8089
  auth-service-url: http://localhost:8083
  account-service-url: http://localhost:8082
  transaction-service-url: http://localhost:8088
  notification-service-url: http://localhost:8087
  audit-service-url: http://localhost:8086
  kafka-service-url: http://localhost:8084
  rabbitmq-service-url: http://localhost:8085
```

### Production YAML (with environment variables)

```yaml
services:
  user-service-url: ${USER_SERVICE_URL:http://user-service:8089}
  auth-service-url: ${AUTH_SERVICE_URL:http://auth-service:8083}
  # ... supports Docker/Kubernetes service discovery
```

### Benefits

1. **Environment-specific configuration** - Different URLs per environment (dev, staging, prod, test)
2. **Environment variable support** - Override via `${VAR_NAME:default}` syntax
3. **No code changes needed** - Just update YAML for different environments
4. **Container-friendly** - Works with Docker Compose and Kubernetes

---

## Verification

### Before Fix
```
Gateway:      traceId=abc123
Main-service: traceId=abc123 (http thread) → traceId="" (reactor thread)
User-service: traceId=xyz789 (NEW - WRONG!)
```

### After Fix
```
Gateway:      traceId=abc123
Main-service: traceId=abc123 [fromHeaders: trace=true]
User-service: traceId=abc123 [fromHeaders: trace=true]
```

### Log Verification
Look for this pattern in downstream service logs:
```json
{
  "message": "MDC context set - traceId=xxx, spanId=xxx, correlationId=xxx [fromHeaders: trace=true, span=true, correlation=true]"
}
```

If `fromHeaders: trace=true`, the propagation is working correctly.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLIENT                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         GATEWAY SERVICE (8080)                           │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ ApplicationDetailsGlobalFilter (HIGHEST_PRECEDENCE)             │    │
│  │  • Generates traceId (32-char hex)                              │    │
│  │  • Generates spanId (16-char hex)                               │    │
│  │  • Generates correlationId (UUID)                               │    │
│  │  • Sets MDC for logging                                         │    │
│  │  • Injects headers: X-Trace-Id, X-Span-Id, X-Correlation-Id,   │    │
│  │                     X-B3-TraceId, X-B3-SpanId                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                     Headers: X-Trace-Id, X-Correlation-Id, etc.
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         MAIN SERVICE (8081)                              │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ MdcLoggingFilter (HIGHEST_PRECEDENCE)                           │    │
│  │  • Extracts headers from request                                │    │
│  │  • Sets MDC: traceId, spanId, correlationId                    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ ServiceClientConfiguration                                       │    │
│  │  • WebClient beans with traceHeaderPropagationFilter()          │    │
│  │  • Reads MDC values                                             │    │
│  │  • Adds headers to outgoing requests                            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                     Headers: X-Trace-Id, X-Correlation-Id, etc.
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│ USER SERVICE      │   │ ACCOUNT SERVICE   │   │ AUTH SERVICE      │
│ (8089)            │   │ (8082)            │   │ (8083)            │
│                   │   │                   │   │                   │
│ MdcLoggingFilter  │   │ MdcLoggingFilter  │   │ MdcLoggingFilter  │
│ • Extracts headers│   │ • Extracts headers│   │ • Extracts headers│
│ • Sets MDC        │   │ • Sets MDC        │   │ • Sets MDC        │
│ • Same traceId!   │   │ • Same traceId!   │   │ • Same traceId!   │
└───────────────────┘   └───────────────────┘   └───────────────────┘
```

---

## Headers Reference

| Header Name | Description | Format |
|-------------|-------------|--------|
| `X-Trace-Id` | Distributed trace ID | 32-char hex |
| `X-B3-TraceId` | B3 format trace ID (Zipkin) | 32-char hex |
| `X-Span-Id` | Current span ID | 16-char hex |
| `X-B3-SpanId` | B3 format span ID | 16-char hex |
| `X-Correlation-Id` | Business correlation ID | UUID |

---

## Troubleshooting

### Issue: `fromHeaders: trace=false` in logs
**Cause:** Headers not being received by the service
**Check:**
1. Verify gateway is adding headers (check response headers)
2. Verify WebClient filter is applied
3. Check if any proxy/load balancer is stripping headers

### Issue: traceId empty in reactor threads
**Cause:** MDC doesn't propagate to reactor threads automatically
**Solution:** Enable context propagation:
```java
Hooks.enableAutomaticContextPropagation();
```

### Issue: Different traceId in different services
**Cause:** Headers not forwarded between services
**Check:**
1. WebClient beans have `traceHeaderPropagationFilter()`
2. `core-common` dependency is included
3. `@ComponentScan` includes `snvn.common`

---

## Date: February 21, 2026
## Author: Development Team



