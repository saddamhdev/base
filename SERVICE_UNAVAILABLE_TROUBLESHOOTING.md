# Service Temporarily Unavailable - Troubleshooting Guide

## Problem
You're receiving "Service temporarily unavailable" response from the gateway.

## Causes and Solutions

### 1. **Backend Service (Port 8081) is Not Running** ⚠️ MOST COMMON

**Check if service is running:**
```powershell
Get-NetTCPConnection -State Listen | Where-Object {$_.LocalPort -eq 8081}
```

**Solution:**
- Start the main service on port 8081
- Or change the URI in `application-dev.yml` to match your service port

### 2. **Redis is Not Running** ⚠️ REQUIRED FOR RATE LIMITER

**Check if Redis is running:**
```powershell
Get-NetTCPConnection -State Listen | Where-Object {$_.LocalPort -eq 6379}
```

**Solution:**
- Start Redis on port 6379
- Or disable RequestRateLimiter temporarily by removing it from routes

### 3. **Circuit Breaker is OPEN**

The circuit breaker opens when:
- Failure rate exceeds 70% (adjusted for dev)
- Minimum of 10 consecutive calls have failed
- This prevents cascading failures

**Current Circuit Breaker Settings (Development):**
```yaml
slidingWindowSize: 20           # Evaluates last 20 calls
minimumNumberOfCalls: 10        # Need 10+ calls to evaluate
failureRateThreshold: 70        # Open if >70% fail
waitDurationInOpenState: 10000  # 10 seconds before retry
```

**How to recover:**
1. Fix the backend service
2. Wait 10 seconds (waitDurationInOpenState)
3. Gateway will try again (half-open state)
4. If 1 call succeeds, circuit closes

## How to Disable Filters for Development

### Option 1: Disable CircuitBreaker Only
Remove the CircuitBreaker filter from routes in `application-dev.yml`:

```yaml
routes:
  - id: main-service-users
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/**
    filters:
      # Remove CircuitBreaker, keep RequestRateLimiter if needed
      - name: RequestRateLimiter
        args:
          key-resolver: "#{@ipAddressKeyResolver}"
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20
```

### Option 2: Disable All Filters
Remove all filters from routes:

```yaml
routes:
  - id: main-service-users
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/**
    # No filters
```

## Debugging Steps

1. **Check logs:**
   ```
   Look for: "Circuit breaker is OPEN"
   Look for: "Connection refused" 
   Look for: "RateLimiter"
   ```

2. **Monitor Gateway Health:**
   - Actuator endpoint: `http://localhost:8080/gateway/actuator`
   - Circuit breaker status: `http://localhost:8080/gateway/actuator/health/circuitbreakers`

3. **Test Individual Routes:**
   - Test without filters first
   - Test with filters only once backend is confirmed working

4. **Check Redis Connection:**
   - If rate limiter is enabled, ensure Redis is running
   - Redis password: `@#12345678`

## Quick Checklist

- [ ] Is main service running on port 8081?
- [ ] Is Redis running on port 6379?
- [ ] Check gateway logs for specific errors
- [ ] Try disabling RequestRateLimiter if Redis is not available
- [ ] Wait 10+ seconds if circuit breaker was open
- [ ] Restart gateway after configuration changes

## Production vs Development Settings

**Current Development Settings (More Lenient):**
- Failure rate threshold: 70%
- Minimum calls: 10
- Wait time before retry: 10 seconds

**For Production (More Strict):**
```yaml
failureRateThreshold: 50
minimumNumberOfCalls: 5
waitDurationInOpenState: 30000  # 30 seconds
```

## Related Files

- Configuration: `gateway-service-env-properties/src/main/resources/application-dev.yml`
- Gateway Config: `gateway-service/src/main/java/snvn/gatewayservice/config/GatewayConfiguration.java`
- Error Handler: `gateway-service/src/main/java/snvn/gatewayservice/exception/GlobalErrorHandler.java`
- Fallback Controller: `gateway-service/src/main/java/snvn/gatewayservice/controller/FallbackController.java`

