# Gateway Request Headers Configuration

## Overview
The Spring Cloud Gateway has been configured to automatically add custom HTTP headers to all downstream requests. This enhances traceability, debugging, and service identification.

## Added Request Headers

### Headers Added to ALL Routes

#### 1. **X-Request-ID** ✅
- **Purpose**: Unique request identifier for tracing across services
- **Value**: Auto-generated UUID
- **Format**: `UUID.randomUUID()`
- **Example**: `550e8400-e29b-41d4-a716-446655440000`
- **Use Case**: 
  - Trace requests through microservices
  - Correlate logs across services
  - Debugging and monitoring

#### 2. **X-Gateway-Service** ✅
- **Purpose**: Identify that request came through gateway
- **Value**: Static string `"gateway-service"`
- **Format**: Plain text
- **Use Case**:
  - Services can identify if request is from gateway
  - Distinguish between direct and proxied requests
  - Security and access control

---

### Route-Specific Headers

#### 3. **X-Request-Timestamp** (users route only)
- **Route**: `/api/users/**`
- **Purpose**: Timestamp when request entered gateway
- **Value**: Current system time in milliseconds
- **Format**: `System.currentTimeMillis()`
- **Example**: `1708515234567`
- **Use Case**: Measure request processing time

#### 4. **X-Auth-Request** (auth route only)
- **Route**: `/api/auth/**`
- **Purpose**: Mark authentication-related requests
- **Value**: Static string `"true"`
- **Use Case**: Backend can apply auth-specific logic

---

## Routes with Headers

| Route ID | Path | Headers |
|----------|------|---------|
| main-service-users | `/api/users/**` | X-Request-ID, X-Gateway-Service, X-Request-Timestamp |
| main-service-auth | `/api/auth/**` | X-Request-ID, X-Gateway-Service, X-Auth-Request |
| main-service-accounts | `/api/accounts/**` | X-Request-ID, X-Gateway-Service |
| main-service-transactions | `/api/transactions/**` | X-Request-ID, X-Gateway-Service |
| config-service | `/config/**` | X-Request-ID, X-Gateway-Service |
| main-service-default | `/**` (fallback) | X-Request-ID, X-Gateway-Service |

---

## Configuration in YAML

```yaml
routes:
  - id: main-service-users
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/**
    filters:
      - name: AddRequestHeader
        args:
          name: X-Request-ID
          value: "#{T(java.util.UUID).randomUUID()}"
      - name: AddRequestHeader
        args:
          name: X-Gateway-Service
          value: "gateway-service"
      - name: AddRequestHeader
        args:
          name: X-Request-Timestamp
          value: "#{T(java.lang.System).currentTimeMillis()}"
```

---

## How to Access Headers in Microservices

### Spring Boot REST Controller Example
```java
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            @RequestHeader(value = "X-Gateway-Service", required = false) String gatewayService,
            @RequestHeader(value = "X-Request-Timestamp", required = false) String timestamp) {
        
        System.out.println("Request ID: " + requestId);
        System.out.println("Gateway Service: " + gatewayService);
        System.out.println("Timestamp: " + timestamp);
        
        // Your logic here
        return ResponseEntity.ok("Headers received");
    }
}
```

### Accessing Headers via HttpServletRequest
```java
@GetMapping
public ResponseEntity<?> getUsers(HttpServletRequest request) {
    String requestId = request.getHeader("X-Request-ID");
    String gatewayService = request.getHeader("X-Gateway-Service");
    String timestamp = request.getHeader("X-Request-Timestamp");
    
    // Process headers
    return ResponseEntity.ok("Success");
}
```

---

## Testing the Headers

### Using cURL
```bash
curl -v http://localhost:8080/gateway/api/users/1
```

Look for headers in the response:
```
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
X-Gateway-Service: gateway-service
X-Request-Timestamp: 1708515234567
```

### Using Postman
1. Send request to `http://localhost:8080/gateway/api/users/1`
2. Check "Headers" tab in response
3. Verify custom headers are present

---

## Adding More Custom Headers

To add more headers to a route, add another `AddRequestHeader` filter:

```yaml
filters:
  - name: AddRequestHeader
    args:
      name: X-Custom-Header
      value: "custom-value"
  - name: AddRequestHeader
    args:
      name: X-User-Agent
      value: "#{request.headers['User-Agent']}"  # Reference request header
```

---

## Performance Considerations

✅ **Minimal Impact**: Header addition is a lightweight operation
✅ **UUID Generation**: Fast, minimal CPU overhead
✅ **String Operations**: Negligible performance impact
✅ **No Additional Network Calls**: Headers are added in-memory

---

## Security Considerations

⚠️ **Information Disclosure**: These headers reveal:
- Request flow path (gateway → service)
- Request timing information
- Internal service identification

For **Production**:
- Consider removing X-Gateway-Service
- Use request masking for sensitive identifiers
- Implement header validation in backend services

---

## Related Configuration Files

- **Main Config**: `gateway-service-env-properties/src/main/resources/application-dev.yml`
- **Gateway Service**: `gateway-service/src/main/java/snvn/gatewayservice/`
- **Key Resolver**: `GatewayConfiguration.java`

---

## Troubleshooting

### Headers Not Appearing in Downstream Service

1. Check if request reaches the service:
   ```bash
   # Enable debug logging
   logging.level.org.springframework.cloud.gateway=DEBUG
   ```

2. Verify route predicates match:
   - Path pattern in route config
   - Actual request URL

3. Check if headers are being removed:
   - Middleware might strip headers
   - Check gateway filters order

### Headers Missing in Response

- Headers added by gateway are for **downstream requests**
- They won't appear in the gateway response to client
- They only appear in backend service logs

---

## Version Info

- Spring Cloud Gateway: 2023.0.3
- Spring Boot: 3.3.2
- Java: 11+

