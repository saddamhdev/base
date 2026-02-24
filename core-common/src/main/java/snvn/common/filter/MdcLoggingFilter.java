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

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts trace context headers from incoming requests
 * and sets them in the MDC for logging purposes.
 *
 * Headers extracted:
 * - X-Trace-Id / X-B3-TraceId: Distributed trace ID
 * - X-Span-Id / X-B3-SpanId: Current span ID
 * - X-Correlation-Id: Business correlation ID
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MdcLoggingFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_B3_HEADER = "X-B3-TraceId";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String SPAN_ID_B3_HEADER = "X-B3-SpanId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Debug: Log all incoming headers to diagnose header propagation
            if (log.isTraceEnabled()) {
                java.util.Enumeration<String> headerNames = httpRequest.getHeaderNames();
                StringBuilder headers = new StringBuilder("Incoming headers: ");
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    headers.append(name).append("=").append(httpRequest.getHeader(name)).append(", ");
                }
                log.trace(headers.toString());
            }

            // Extract trace ID from headers (try multiple header names)
            String traceId = getHeaderValue(httpRequest, TRACE_ID_HEADER, TRACE_ID_B3_HEADER);
            boolean traceIdFromHeader = (traceId != null && !traceId.isBlank());
            if (!traceIdFromHeader) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            // Extract span ID from headers
            String spanId = getHeaderValue(httpRequest, SPAN_ID_HEADER, SPAN_ID_B3_HEADER);
            boolean spanIdFromHeader = (spanId != null && !spanId.isBlank());
            if (!spanIdFromHeader) {
                spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            // Extract correlation ID from headers
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            boolean correlationIdFromHeader = (correlationId != null && !correlationId.isBlank());
            if (!correlationIdFromHeader) {
                correlationId = UUID.randomUUID().toString();
            }

            // Set MDC values for logging
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID, spanId);
            MDC.put(MDC_CORRELATION_ID, correlationId);

            // Add trace headers to response
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            httpResponse.setHeader(SPAN_ID_HEADER, spanId);
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            log.debug("MDC context set - traceId={}, spanId={}, correlationId={} [fromHeaders: trace={}, span={}, correlation={}]",
                    traceId, spanId, correlationId, traceIdFromHeader, spanIdFromHeader, correlationIdFromHeader);

            // Continue with the filter chain
            chain.doFilter(request, response);

        } finally {
            // Clear MDC after request is complete
            MDC.clear();
        }
    }

    /**
     * Gets header value trying multiple header names
     */
    private String getHeaderValue(HttpServletRequest request, String... headerNames) {
        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

