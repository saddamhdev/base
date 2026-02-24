package snvn.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for WebClient beans to communicate with microservices.
 * Service clients are configured via YAML and accessed through WebClientRegistry.
 */
@Configuration
@EnableConfigurationProperties(ServiceClientConfiguration.ServiceClientsProperties.class)
public class ServiceClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ServiceClientConfiguration.class);

    /**
     * Creates the WebClientRegistry bean
     */
    @Bean
    public WebClientRegistry webClientRegistry(ServiceClientsProperties properties) {
        return new WebClientRegistry(properties);
    }

    /**
     * Registry that provides WebClient instances by service name.
     * WebClients are lazily created and cached.
     */
    public static class WebClientRegistry {
        private static final Logger log = LoggerFactory.getLogger(WebClientRegistry.class);

        private final ServiceClientsProperties properties;
        private final Map<String, WebClient> clientCache = new ConcurrentHashMap<>();

        public WebClientRegistry(ServiceClientsProperties properties) {
            this.properties = properties;
            int clientCount = (properties.getClients() != null) ? properties.getClients().size() : 0;
            log.info("WebClientRegistry initialized with {} service clients", clientCount);

            if (properties.getClients() != null) {
                properties.getClients().forEach((key, config) ->
                    log.info("  - {} -> {}", key, config.getUrl()));
            }
        }

        /**
         * Get WebClient by service key (e.g., "user-service")
         */
        public WebClient getClient(String serviceKey) {
            return clientCache.computeIfAbsent(serviceKey, key -> {
                if (properties.getClients() == null) {
                    throw new IllegalStateException("No service clients configured. Check 'services.clients' in application YAML.");
                }
                ServiceClientConfig config = properties.getClients().get(key);
                if (config == null) {
                    throw new IllegalArgumentException("No service client configured for: " + key +
                        ". Available clients: " + properties.getClients().keySet());
                }
                log.info("Creating WebClient for {} -> {}", key, config.getUrl());
                return buildWebClient(config);
            });
        }

        /**
         * Convenience methods for specific services
         */
        public WebClient getUserServiceClient() {
            return getClient("user-service");
        }

        public WebClient getAuthServiceClient() {
            return getClient("auth-service");
        }

        public WebClient getAccountServiceClient() {
            return getClient("account-service");
        }

        public WebClient getTransactionServiceClient() {
            return getClient("transaction-service");
        }

        public WebClient getNotificationServiceClient() {
            return getClient("notification-service");
        }

        public WebClient getAuditServiceClient() {
            return getClient("audit-service");
        }

        public WebClient getKafkaServiceClient() {
            return getClient("kafka-service");
        }

        public WebClient getRabbitmqServiceClient() {
            return getClient("rabbitmq-service");
        }

        private WebClient buildWebClient(ServiceClientConfig config) {
            return WebClient.builder()
                    .baseUrl(config.getUrl())
                    .filter(traceHeaderPropagationFilter())
                    .build();
        }

        /**
         * Creates a filter that propagates trace context headers from MDC to outgoing requests
         */
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
    }

    /**
     * Configuration properties for all service clients - loaded from YAML
     */
    @ConfigurationProperties(prefix = "services")
    public static class ServiceClientsProperties {
        private Map<String, ServiceClientConfig> clients = new HashMap<>();

        public Map<String, ServiceClientConfig> getClients() {
            return clients;
        }

        public void setClients(Map<String, ServiceClientConfig> clients) {
            this.clients = clients;
        }
    }

    /**
     * Configuration for individual service client
     */
    public static class ServiceClientConfig {
        private String url;
        private String name;
        private String beanName;
        private int connectTimeout = 5000;
        private int readTimeout = 10000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}

