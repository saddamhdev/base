# Global Exception Handler with Trace Context Propagation

## Problem

When using WebClient in reactive streams, the MDC (Mapped Diagnostic Context) is lost when:
1. Errors occur on reactor threads (e.g., `reactor-http-nio-*`)
2. Connection failures happen (e.g., `Connection refused`)
3. Service unavailable errors occur

This results in error logs missing `traceId`, `spanId`, and `correlationId`:

```json
{
  "timestamp": "2026-02-22T16:34:22.184",
  "level": "ERROR",
  "service": "main-service",
  "thread": "reactor-http-nio-3",
  "traceId": "",       // MISSING!
  "spanId": "",        // MISSING!
  "correlationId": "", // MISSING!
  "logger": "s.c.UserAggregatorController",
  "message": "Error creating user: john_doe"
}
```

## Solution Overview

### Module Structure

The solution is split between two modules for better separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                      core-common                                 │
│  (Servlet-based, shared by all services)                        │
├─────────────────────────────────────────────────────────────────┤
│  • GlobalExceptionHandler - Base exception handling             │
│  • MdcLoggingFilter - Sets MDC from request headers             │
│  • No reactive/WebFlux dependencies                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      main-service                                │
│  (Uses WebClient for inter-service communication)               │
├─────────────────────────────────────────────────────────────────┤
│  • MainServiceExceptionHandler - WebClient exception handling   │
│  • ReactiveErrorHandler - MDC context for reactive streams      │
│  • ServiceUnavailableException - Exception with trace context   │
│  • Has spring-boot-starter-webflux dependency                   │
└─────────────────────────────────────────────────────────────────┘
```

### Why Reactive Classes Are in main-service?

The `main-service` uses **WebClient** (reactive HTTP client) to call downstream services. WebClient requires reactive types (`Mono`, `Flux`) and throws reactive-specific exceptions (`WebClientRequestException`).

**Keeping reactive code in main-service:**
- ✅ Clean separation - servlet services don't need webflux dependency
- ✅ Only modules using WebClient need reactive error handling
- ✅ core-common stays lightweight

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Request Flow                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  HTTP Request                                                    │
│      │                                                           │
│      ▼                                                           │
│  ┌──────────────────┐                                           │
│  │  MdcLoggingFilter │  Sets traceId, correlationId             │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────┐   Captures MDC context                    │
│  │    Controller     │   ReactiveErrorHandler.captureMdcContext()│
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────┐   Wraps errors with trace context         │
│  │     Service       │   .onErrorMap(wrapWithTraceContext())    │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────┐   Handles exceptions with context         │
│  │GlobalExceptionHandler│                                        │
│  └──────────────────┘                                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## YAML Configuration

The service clients are fully configured via YAML:

```yaml
# application.yml
services:
  clients:
    user-service:
      name: user-service
      bean-name: userServiceClient
      url: http://localhost:8089
      connect-timeout: 5000
      read-timeout: 10000
    auth-service:
      name: auth-service
      bean-name: authServiceClient
      url: http://localhost:8083
      connect-timeout: 5000
      read-timeout: 10000
    account-service:
      name: account-service
      bean-name: accountServiceClient
      url: http://localhost:8082
      connect-timeout: 5000
      read-timeout: 10000
    # ... other services
```

## Code Examples

### 1. GlobalExceptionHandler Usage

The `GlobalExceptionHandler` automatically handles:
- `ServiceUnavailableException` - Custom exception with embedded trace context
- `WebClientRequestException` - Connection errors, timeouts
- `WebClientResponseException` - 4xx/5xx from downstream services
- `ConnectException` - TCP connection failures
- `IllegalStateException` - Configuration errors
- `IllegalArgumentException` - Invalid requests
- `Exception` - Catch-all for unhandled exceptions

### 2. ReactiveErrorHandler in Controller

```java
@PostMapping
public Mono<ResponseEntity<CreateUserResponse>> createUser(@RequestBody CreateUserRequest request) {
    logger.info("Received request to create user: {}", request.getUsername());

    // Capture MDC context before entering reactive stream
    final Map<String, String> mdcContext = ReactiveErrorHandler.captureMdcContext();

    return userAggregatorService.createUser(request)
        .doOnSubscribe(subscription -> {
            // Restore MDC context when subscription starts
            ReactiveErrorHandler.restoreMdcContext(mdcContext);
        })
        .map(response -> {
            // Restore MDC context for logging
            ReactiveErrorHandler.restoreMdcContext(mdcContext);
            logger.info("User creation completed successfully for: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        })
        .onErrorMap(ReactiveErrorHandler.wrapWithTraceContext("user-orchestration"));
        // Let GlobalExceptionHandler handle the wrapped exception
}
```

### 3. ReactiveErrorHandler in Service

```java
private Mono<User> createUserInUserService(CreateUserRequest request, Map<String, String> mdcContext) {
    return userServiceClient().post()
        .uri("/api/users")
        .bodyValue(user)
        .retrieve()
        .bodyToMono(User.class)
        .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
        .doOnSuccess(createdUser -> {
            ReactiveErrorHandler.restoreMdcContext(mdcContext);
            logger.info("User created in user-service: {}", createdUser.getId());
        })
        .doOnError(error -> {
            ReactiveErrorHandler.restoreMdcContext(mdcContext);
            logger.error("Failed to create user in user-service", error);
        })
        .onErrorMap(ReactiveErrorHandler.wrapWithTraceContext("user-service"));
}
```

## Error Response Format

All errors return a standardized JSON response:

```json
{
  "timestamp": "2026-02-22T16:34:22.184Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Service 'user-service' (http://localhost:8089) is unavailable: Connection refused",
  "traceId": "ae080dd0b22740e5a35d8bec77cc6a6c",
  "spanId": "88ef38fc81144b9c",
  "correlationId": "1f4275a0-269e-426a-b260-c9c46cbf2260",
  "jobId": "",
  "exceptionType": "ServiceUnavailableException"
}
```

## Files Modified/Created

### core-common module

| File | Description |
|------|-------------|
| `pom.xml` | Added `spring-boot-starter-webflux` dependency |
| `GlobalExceptionHandler.java` | Enhanced with WebClient exception handlers |
| `ServiceUnavailableException.java` | New custom exception with trace context |
| `ReactiveErrorHandler.java` | New utility for reactive error handling |

### main module

| File | Description |
|------|-------------|
| `UserAggregatorController.java` | Updated to use ReactiveErrorHandler |
| `UserAggregatorService.java` | Updated to propagate MDC context |

## Dependencies Required

```xml
<!-- In core-common/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## How It Works

1. **Request Arrives**: `MdcLoggingFilter` sets `traceId`, `spanId`, `correlationId` in MDC
2. **Controller**: Captures MDC context using `ReactiveErrorHandler.captureMdcContext()`
3. **Reactive Chain**: Context is passed through and restored at each stage
4. **Error Occurs**: `ReactiveErrorHandler.wrapWithTraceContext()` wraps the error with captured context
5. **GlobalExceptionHandler**: Extracts trace info from `ServiceUnavailableException` and includes in response

## Troubleshooting

### IDE Shows "Cannot resolve symbol"

After adding webflux dependency, reload Maven projects:
- IntelliJ: Right-click on pom.xml → Maven → Reload Project
- Or: Maven tool window → Refresh button

### traceId Still Empty

Ensure:
1. `MdcLoggingFilter` is registered and running before your controller
2. Headers are being propagated: `X-Trace-Id`, `X-Correlation-Id`
3. `ReactiveErrorHandler.captureMdcContext()` is called at the start of your reactive chain

### Connection Refused Errors

The downstream service is not running. Check:
1. Service is started on the correct port
2. Firewall is not blocking
3. URL in YAML configuration is correct

