package snvn.gatewayservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayHealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "API Gateway");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @GetMapping("/routes")
    public Map<String, Object> routes() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "API Gateway");
        response.put("routes", new String[]{
                "user-service -> /api/users/**",
                "auth-service -> /api/auth/**",
                "account-service -> /api/accounts/**",
                "transaction-service -> /api/transactions/**",
                "notification-service -> /api/notifications/**",
                "audit-service -> /api/audit/**",
                "kafka-service -> /api/kafka/**",
                "rabbitmq-service -> /api/rabbitmq/**",
                "config-service -> /config/**"
        });
        response.put("port", 8080);
        return response;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        Map<String, String> response = new HashMap<>();
        response.put("name", "API Gateway Service");
        response.put("version", "1.0-SNAPSHOT");
        response.put("description", "Spring Cloud Gateway for routing requests to microservices");
        return response;
    }
}

