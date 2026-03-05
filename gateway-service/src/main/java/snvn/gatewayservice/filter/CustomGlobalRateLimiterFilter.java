package snvn.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import snvn.common.logging.ExternalLogService;
import snvn.gatewayservice.config.GatewayServiceLogProperties;
import snvn.gatewayservice.config.RateLimitProperties;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified Rate Limiter GlobalFilter.
 * Sends logs only to channels enabled via GatewayServiceLogProperties.
 */
@Component
public class CustomGlobalRateLimiterFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(CustomGlobalRateLimiterFilter.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final ExternalLogService externalLogService;
    private final GatewayServiceLogProperties logProperties;

    public CustomGlobalRateLimiterFilter(
            ReactiveStringRedisTemplate redisTemplate,
            RateLimitProperties properties,
            GatewayServiceLogProperties logProperties,
            ExternalLogService externalLogService) {

        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.logProperties = logProperties;
        this.externalLogService = externalLogService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String incomingApiKey = exchange.getRequest()
                .getHeaders()
                .getFirst("X-API-KEY");

        final String apiKey =
                (incomingApiKey == null || incomingApiKey.isBlank())
                        ? "anonymous"
                        : incomingApiKey;

        String minuteBucket = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        final String redisKey =
                "global-rate-limit:" + apiKey + ":" + minuteBucket;

        // Read tracing headers (already injected by ApplicationDetailsGlobalFilter)
        String traceId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Trace-Id");

        String spanId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Span-Id");

        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Correlation-Id");

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count ->
                        redisTemplate.expire(redisKey, Duration.ofMinutes(1))
                                .thenReturn(count)
                )
                .flatMap(count -> {

                    Map<String, Object> logData = new HashMap<>();
                    logData.put("traceId", traceId);
                    logData.put("spanId", spanId);
                    logData.put("correlationId", correlationId);
                    logData.put("logger", log.getName() + ".filter()");
                    logData.put("apiKey", apiKey);
                    logData.put("attemptCount", count);
                    logData.put("limit", properties.getRequestsPerMinute());
                    logData.put("timestamp", Instant.now());

                    // Send to enabled channels only
                    sendLog("INFO", "Rate limit attempt", logData);

                    if (count > properties.getRequestsPerMinute()) {

                        exchange.getResponse()
                                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

                        sendErrorLog("WARN", "Rate limit exceeded", logData);

                        return exchange.getResponse().setComplete();
                    }

                    return chain.filter(exchange);
                });
    }

    private void sendLog(String level, String message, Map<String, Object> context) {
        if (logProperties.isLogfileEnabled()) {
            externalLogService.sendLogFile(level, message, context);
        }
        if (logProperties.isSplunkEnabled()) {
            externalLogService.sendLogSplunk(level, message, context);
        }
        if (logProperties.isRabbitmqEnabled()) {
            externalLogService.sendLogRabbitMQ(level, message, context);
        }
        if (logProperties.isKafkaEnabled()) {
            externalLogService.sendLogKafka(level, message, context);
        }
    }

    private void sendErrorLog(String level, String message, Map<String, Object> context) {
        if (logProperties.isLogfileEnabled()) {
            externalLogService.sendErrorLogFile(level, message, context);
        }
        if (logProperties.isSplunkEnabled()) {
            externalLogService.sendErrorLogSplunk(level, message, context);
        }
        if (logProperties.isRabbitmqEnabled()) {
            externalLogService.sendErrorLogRabbitMQ(level, message, context);
        }
    }

    @Override
    public int getOrder() {
        return -50; // AFTER trace filter (ApplicationDetailsGlobalFilter is -100)
    }
}

