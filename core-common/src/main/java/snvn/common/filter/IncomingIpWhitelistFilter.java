package snvn.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import snvn.common.config.ServiceLogProperties;
import snvn.common.config.ipWhiteListing.IncomingIpWhitelistProperties;
import snvn.common.logging.ExternalLogService;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class IncomingIpWhitelistFilter implements Filter {

    private static final Logger log =
            LoggerFactory.getLogger(IncomingIpWhitelistFilter.class);

    private final IncomingIpWhitelistProperties properties;
    private final ExternalLogService externalLogService;
    private final ServiceLogProperties logProperties;

    public IncomingIpWhitelistFilter(
            IncomingIpWhitelistProperties properties,
            ExternalLogService externalLogService,
            ServiceLogProperties logProperties) {

        this.properties = properties;
        this.externalLogService = externalLogService;
        this.logProperties = logProperties;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(httpRequest);
        int clientPort = httpRequest.getRemotePort();
        String protocol = httpRequest.getScheme();

        Map<String, Object> logData = new HashMap<>();
        logData.put("traceId", MDC.get("traceId"));
        logData.put("spanId", MDC.get("spanId"));
        logData.put("correlationId", MDC.get("correlationId"));
        logData.put("clientIp", clientIp);
        logData.put("clientPort", clientPort);
        logData.put("protocol", protocol);
        logData.put("path", httpRequest.getRequestURI());
        logData.put("method", httpRequest.getMethod());
        logData.put("timestamp", Instant.now());

        boolean allowed = properties.getAllowedIps()
                .stream()
                .filter(IncomingIpWhitelistProperties.AllowedIp::isEnabled)
                .anyMatch(d ->
                        d.getAddress().equals(clientIp)
                                && protocol != null
                                && protocol.equalsIgnoreCase(d.getProtocol())
                                && clientPort >= d.getPortStart()
                                && clientPort <= d.getPortEnd()
                );

        if (!allowed) {

            log.warn("IP whitelist blocked request from {}", clientIp);

            sendErrorLog("WARN", "IP not whitelisted", logData);

            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.getWriter().write("Access denied: IP not allowed");

            return;
        }

        sendLog("INFO", "IP whitelist passed", logData);

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        return ip != null ? ip : "unknown";
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

        if (logProperties.isKafkaEnabled()) {
            externalLogService.sendErrorLogKafka(level, message, context);
        }
    }
}