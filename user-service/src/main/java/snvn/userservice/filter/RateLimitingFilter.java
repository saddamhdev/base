package snvn.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import snvn.common.logging.ExternalLogService;
import snvn.common.redis.RedisService;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@ConditionalOnProperty(prefix = "user-service.redis-rate-limiting", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "snvn.redis", name = "enabled", havingValue = "true")

public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger log =
            LoggerFactory.getLogger(RateLimitingFilter.class);
    private final RedisService redisService;
    private final ExternalLogService externalLogService;

    public RateLimitingFilter(RedisService redisService,
                              ExternalLogService externalLogService) {
        this.redisService = redisService;
        this.externalLogService = externalLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        RateLimitRule rule = resolveRule(path);

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildKey(request);
        long windowSeconds = rule.window().toSeconds();

        Long count = redisService.increment(key);

        if (count != null && count == 1) {
            redisService.set(key, "1", rule.window());
        }

        Map<String, Object> context = buildContext(request, key, count, rule);
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");
        if (count != null && count > rule.maxRequests()) {

            context.put("event", "RATE_LIMIT_EXCEEDED");
            context.put("retryAfterSeconds", windowSeconds);

            externalLogService.sendLogSplunk(
                    "WARN",
                    "Rate limit exceeded for endpoint: " + path,
                    context
            );

            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(rule.maxRequests()));
            response.setHeader("X-Rate-Limit-Remaining", "0");

            response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded. Please try again later."
                }
                """);

            return;
        }

        long remaining = rule.maxRequests() - (count == null ? 0 : count);

        context.put("event", "RATE_LIMIT_ALLOWED");
        context.put("remaining", remaining);

        externalLogService.sendLogSplunk(
                "INFO",
                "Rate limit allowed for endpoint: " + path,
                context
        );

        response.setHeader("X-Rate-Limit-Limit", String.valueOf(rule.maxRequests()));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(Math.max(remaining, 0)));

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(String path) {

        if (path.startsWith("/api/auth/login")) {
            return new RateLimitRule(5, Duration.ofMinutes(1));
        }

        if (path.startsWith("/api/auth/otp")) {
            return new RateLimitRule(3, Duration.ofMinutes(1));
        }

        if (path.startsWith("/api/transactions")) {
            return new RateLimitRule(10, Duration.ofMinutes(1));
        }

        return new RateLimitRule(60, Duration.ofMinutes(1));
    }

    private String buildKey(HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "rate:user:" + auth.getName();
        }

        return "rate:ip:" + request.getRemoteAddr();
    }

    private Map<String, Object> buildContext(HttpServletRequest request,
                                             String key,
                                             Long count,
                                             RateLimitRule rule) {

        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", Instant.now().toString());
        context.put("service", "user-service");
        context.put("path", request.getRequestURI());
        context.put("method", request.getMethod());
        context.put("rateLimitKey", key);
        context.put("requestCount", count);
        context.put("maxAllowed", rule.maxRequests());
        context.put("windowSeconds", rule.window().toSeconds());
        context.put("traceId", MDC.get("traceId"));
        context.put("correlationId", MDC.get("correlationId"));
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");
        context.put("ip", request.getRemoteAddr());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            context.put("username", auth.getName());
        }

        return context;
    }

    private record RateLimitRule(long maxRequests, Duration window) {}
}