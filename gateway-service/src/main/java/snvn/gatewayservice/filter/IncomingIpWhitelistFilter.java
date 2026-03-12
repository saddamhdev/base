package snvn.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import snvn.common.logging.ExternalLogService;
import snvn.gatewayservice.config.GatewayServiceLogProperties;
import snvn.gatewayservice.config.ipWhiteListing.IncomingIpWhitelistProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class IncomingIpWhitelistFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(IncomingIpWhitelistFilter.class);

    private final IncomingIpWhitelistProperties properties;
    private final ExternalLogService externalLogService;
    private final GatewayServiceLogProperties logProperties;

    public IncomingIpWhitelistFilter(
            IncomingIpWhitelistProperties properties,
            GatewayServiceLogProperties logProperties,
            ExternalLogService externalLogService) {

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

        var request = exchange.getRequest();
        var remoteAddress = request.getRemoteAddress();

        final String clientIp = resolveClientIp(exchange);

        int clientPort = remoteAddress != null ? remoteAddress.getPort() : -1;

        String protocol = request.getURI().getScheme();

        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        String spanId = request.getHeaders().getFirst("X-Span-Id");
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");

        Map<String, Object> logData = new HashMap<>();
        logData.put("traceId", traceId);
        logData.put("spanId", spanId);
        logData.put("correlationId", correlationId);
        logData.put("clientIp", clientIp);
        logData.put("clientPort", clientPort);
        logData.put("protocol", protocol);
        logData.put("path", request.getPath().toString());
        logData.put("timestamp", Instant.now());

        boolean allowed = properties.getAllowedIps()
                .stream()
                .filter(IncomingIpWhitelistProperties.AllowedIp::isEnabled)
                .anyMatch(d ->
                        clientIp.equals(d.getAddress())
                                && protocol.equalsIgnoreCase(d.getProtocol())
                                && clientPort >= d.getPortStart()
                                && clientPort <= d.getPortEnd()
                );

        if (!allowed) {

            log.warn("Blocked request from IP: {}", clientIp);

            sendErrorLog("WARN", "IP not whitelisted", logData);

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);

            return exchange.getResponse().setComplete();
        }

        sendLog("INFO", "IP whitelist passed", logData);

        return chain.filter(exchange);
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
        if(logProperties.isKafkaEnabled()){
            externalLogService.sendErrorLogKafka(level, message, context);
        }
    }

    @Override
    public int getOrder() {
        return -60;
        // Runs before rate limiter (-50)
    }

    private String resolveClientIp(ServerWebExchange exchange) {

        String ip = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if (ip == null || ip.isBlank()) {
            var remote = exchange.getRequest().getRemoteAddress();
            if (remote != null) {
                ip = remote.getAddress().getHostAddress();
            }
        }

        return ip;
    }
}