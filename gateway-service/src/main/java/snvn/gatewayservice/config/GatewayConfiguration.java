package snvn.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfiguration {
    // Gateway routes are now configured in application.yml
    // using Spring Cloud Gateway's YAML-based configuration

    /**
     * KeyResolver for RequestRateLimiter filter
     * Resolves the rate limit key based on the client's IP address
     */
    @Bean
    public KeyResolver ipAddressKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown"
        );
    }
}


