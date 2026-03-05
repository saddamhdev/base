package snvn.userservice.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import snvn.common.logging.ExternalLogService;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that logs every incoming request and outgoing response to Splunk HEC
 * via ExternalLogService. Runs after MdcLoggingFilter so MDC context is available.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(prefix = "user-service-log.rabbitmq", name = "enabled", havingValue = "true")
public class RequestLoggingFilterByRabbitMQ implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilterByRabbitMQ.class);

    private final ExternalLogService externalLogService;

    public RequestLoggingFilterByRabbitMQ(ExternalLogService externalLogService) {
        this.externalLogService = externalLogService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;
        String remoteAddr = httpRequest.getRemoteAddr();
        long startTime = System.currentTimeMillis();

        // Log incoming request to Splunk
        Map<String, Object> requestContext = buildContext();
        requestContext.put("event", "REQUEST_RECEIVED");
        requestContext.put("method", method);
        requestContext.put("path", fullPath);
        requestContext.put("remoteAddr", remoteAddr);
        requestContext.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");
        requestContext.put("userAgent", httpRequest.getHeader("User-Agent"));
        System.out.println("abs chk");
        externalLogService.sendLogRabbitMQ("INFO",
                String.format("Incoming request by rabbitmq: %s %s", method, fullPath),
                requestContext);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            // Log response to Splunk
            Map<String, Object> responseContext = buildContext();
            responseContext.put("event", "REQUEST_COMPLETED");
            responseContext.put("method", method);
            responseContext.put("path", fullPath);
            responseContext.put("remoteAddr", remoteAddr);
            responseContext.put("status", status);
            responseContext.put("durationMs", duration);
            responseContext.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

            String level = status >= 500 ? "ERROR" : status >= 400 ? "WARN" : "INFO";

            externalLogService.sendLogRabbitMQ(level,
                    String.format("Completed request: %s %s status=%d duration=%dms", method, fullPath, status, duration),
                    responseContext);

            log.info("Request completedRa: {} {} status={} duration={}ms", method, fullPath, status, duration);
        }
    }

    private Map<String, Object> buildContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("service", "user-service");
        context.put("timestamp", Instant.now().toString());
        context.put("traceId", MDC.get("traceId"));
        context.put("spanId", MDC.get("spanId"));
        context.put("correlationId", MDC.get("correlationId"));
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

        return context;
    }
}

