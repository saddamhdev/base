package snvn.splunk;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import snvn.common.logging.AbstractExternalLogService;

import javax.net.ssl.SSLContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class SplunkExternalLogService extends AbstractExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(SplunkExternalLogService.class);

    private final RestClient restClient;
    private final SplunkHecProperties properties;

    public SplunkExternalLogService(SplunkHecProperties properties) {
        this.properties = properties;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader("Authorization", "Splunk " + properties.getToken())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (!properties.isSslVerify()) {
            builder.requestFactory(createInsecureApacheRequestFactory());
            log.warn("SplunkExternalLogService: SSL verification DISABLED (dev only)");
        }

        this.restClient = builder.build();
    }

    @Override
    public void sendLogSplunk(String level, String message, Map<String, Object> context) {

         if (!properties.isEnabled()) {
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("level", level);
        event.put("message", message);
        event.put("timestamp", Instant.now().toString());
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

        event.putAll(context);

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("index", properties.getIndex());
        payload.put("source", properties.getSource());
        payload.put("sourcetype", properties.getSourcetype());
        payload.put("time", Instant.now().getEpochSecond());

        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            try {
                restClient.post()
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                log.debug("Log sent to Splunk successfully");
                //System.out.println("Log sent to Splunk successfully");
            } catch (Exception e) {
                log.warn("Failed to send log to Splunk: {}", e.getMessage());
               // System.out.println("Failed to send log to Splunk: {}"+ e.getMessage());
            } finally {
                MDC.clear();
            }
        });
    }

    @Override
    public void sendErrorLogSplunk(String message, String throwable, Map<String, Object> context) {
        Map<String, Object> errorContext = new HashMap<>();
        if (context != null) {
            errorContext.putAll(context);
        }
        errorContext.put("exception", throwable);
        sendLogSplunk("ERROR", message, errorContext);
    }

    /**
     * DEV ONLY - disables SSL verification.
     */
    private HttpComponentsClientHttpRequestFactory createInsecureApacheRequestFactory() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory =
                    SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build();

            HttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(
                            PoolingHttpClientConnectionManagerBuilder.create()
                                    .setSSLSocketFactory(sslSocketFactory)
                                    .build()
                    )
                    .build();

            return new HttpComponentsClientHttpRequestFactory(httpClient);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure HTTP client for Splunk", e);
        }
    }
}