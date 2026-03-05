package snvn.userservice.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import snvn.common.logging.ExternalLogService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that logs every incoming request and outgoing response to Splunk HEC
 * via ExternalLogService. Runs after MdcLoggingFilter so MDC context is available.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(prefix = "user-service-log.logfile", name = "enabled", havingValue = "true")
public class RequestLoggingFilterByLogFile implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilterByLogFile.class);

    private final ExternalLogService externalLogService;

    public RequestLoggingFilterByLogFile(ExternalLogService externalLogService) {
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

        // Log incoming request
        Map<String, Object> requestContext = buildContext();
        requestContext.put("eventType", "REQUEST_RECEIVED");
        requestContext.put("method", method);
        requestContext.put("path", fullPath);
        requestContext.put("remoteAddr", remoteAddr);
        requestContext.put("userAgent", httpRequest.getHeader("User-Agent"));

        externalLogService.sendLogFile("INFO",
                String.format("Incoming request: %s %s", method, fullPath),
                requestContext);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            // Log response
            Map<String, Object> responseContext = buildContext();
            responseContext.put("eventType", "REQUEST_COMPLETED");
            responseContext.put("method", method);
            responseContext.put("path", fullPath);
            responseContext.put("remoteAddr", remoteAddr);
            responseContext.put("status", status);
            responseContext.put("durationMs", duration);

            String level = status >= 500 ? "ERROR" : status >= 400 ? "WARN" : "INFO";

            externalLogService.sendLogFile(level,
                    String.format("Completed request: %s %s status=%d duration=%dms", method, fullPath, status, duration),
                    responseContext);

            log.info("Request completed1: {} {} status={} duration={}ms", method, fullPath, status, duration);
        }
    }

    private Map<String, Object> buildContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

        return context;
    }
}

