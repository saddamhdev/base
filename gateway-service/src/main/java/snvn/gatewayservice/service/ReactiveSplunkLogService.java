package snvn.gatewayservice.service;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import snvn.common.logging.ExternalLogService;
import snvn.gatewayservice.config.SplunkHecProperties;

import javax.net.ssl.SSLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Reactive implementation of ExternalLogService for Gateway Service.
 * Uses WebClient (non-blocking) to send logs to Splunk HEC.
 */
@Service
abstract public class ReactiveSplunkLogService implements ExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveSplunkLogService.class);

    private final WebClient webClient;
    private final SplunkHecProperties properties;

    public ReactiveSplunkLogService(SplunkHecProperties properties) {
        this.properties = properties;

        boolean canEnable = properties.isEnabled()
                && properties.getUrl() != null
                && !properties.getUrl().isBlank();

        WebClient client = null;
        if (canEnable) {
            try {
                WebClient.Builder builder = WebClient.builder()
                        .baseUrl(properties.getUrl())
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Splunk " + properties.getToken())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

                // Configure SSL based on sslVerify property
                if (!properties.isSslVerify()) {
                    SslContext sslContext = SslContextBuilder
                            .forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build();

                    HttpClient httpClient = HttpClient.create()
                            .secure(t -> t.sslContext(sslContext));

                    builder.clientConnector(new ReactorClientHttpConnector(httpClient));
                    log.info("ReactiveSplunkLogService initialized with SSL verification DISABLED - Splunk HEC URL: {}", properties.getUrl());
                } else {
                    log.info("ReactiveSplunkLogService initialized with SSL verification enabled - Splunk HEC URL: {}", properties.getUrl());
                }

                client = builder.build();
            } catch (SSLException e) {
                log.error("Failed to initialize SSL context for Splunk HEC: {}", e.getMessage());
            }
        } else {
            log.info("ReactiveSplunkLogService initialized - Splunk HEC DISABLED (logs will be written locally only)");
        }
        this.webClient = client;
    }

    @Override
    public void sendLogFile(String level, String message, Map<String, Object> context) {
       // System.out.println("Checking " + message);
        if (!properties.isEnabled() || webClient == null) {
            log.debug("Splunk disabled - level={} message={}", level, message);
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("level", level);
        event.put("message", message);
        event.put("timestamp", Instant.now().toString());
        if (context != null) {
            event.putAll(context);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("index", properties.getIndex());
        payload.put("source", properties.getSource());
        payload.put("time", Instant.now().getEpochSecond());
       // System.out.println(payload);
        // Send asynchronously without blocking
        webClient.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        response -> {
                            log.debug("Log sent to Splunk successfully: {}", payload);
                            System.out.println("Log sent to Splunk successfully: {}"+ payload);
                        },
                        error -> {log.warn("Failed to send log to Splunk: {} - {}", payload, error.getMessage());
                            System.out.println("Failed to send log to Splunk: {} - {}"+ payload+ error.getMessage());
                        }
                );
    }

    @Override
    public void sendErrorLogFile(String level, String message, Map<String, Object> context) {
        Map<String, Object> errorContext = new HashMap<>();
        if (context != null) {
            errorContext.putAll(context);
        }
        //System.err.println("ERROR"+ message+ errorContext);
        sendLogFile(level, message, errorContext);
    }
}
