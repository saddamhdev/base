package snvn.splunkservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import snvn.splunkservice.config.SplunkHecProperties;
import snvn.splunkservice.model.LogEventRequest;
import snvn.splunkservice.model.SplunkEvent;
import snvn.splunkservice.model.SplunkResponse;
import snvn.splunkservice.service.SplunkHecService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of SplunkHecService for sending events to Splunk HTTP Event Collector
 */
@Service
public class SplunkHecServiceImpl implements SplunkHecService {

    private static final Logger logger = LoggerFactory.getLogger(SplunkHecServiceImpl.class);

    private final WebClient splunkWebClient;
    private final SplunkHecProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<SplunkEvent> eventQueue;
    private final String hostname;

    public SplunkHecServiceImpl(WebClient splunkWebClient, SplunkHecProperties properties, ObjectMapper objectMapper) {
        this.splunkWebClient = splunkWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.hostname = getHostname();
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }

    @Override
    public Mono<SplunkResponse> sendEvent(Object event) {

        if (!properties.isEnabled()) {
            return Mono.just(createDisabledResponse());
        }

        // If already SplunkEvent, don't wrap again
        if (event instanceof SplunkEvent splunkEvent) {
            return sendEvent(splunkEvent);
        }

        // If event is Map and already contains "event", unwrap it
        if (event instanceof Map<?, ?> map && map.containsKey("event")) {
            return sendEvent(map.get("event"));
        }

        SplunkEvent splunkEvent = SplunkEvent.builder()
                .event(event)
                .index(properties.getIndex())
                .source(properties.getSource())
                .sourceType(properties.getSourcetype())
                .host(hostname)
                .build();

        return sendEvent(splunkEvent);
    }

    @Override
    public Mono<SplunkResponse> sendEvent(SplunkEvent splunkEvent) {

        if (!properties.isEnabled()) {
            logger.debug("Splunk HEC is disabled, skipping event");
            return Mono.just(createDisabledResponse());
        }

        // Ensure defaults
        String index = splunkEvent.getIndex() != null ? splunkEvent.getIndex() : properties.getIndex();
        String source = splunkEvent.getSource() != null ? splunkEvent.getSource() : properties.getSource();
        String sourceType = splunkEvent.getSourceType() != null ? splunkEvent.getSourceType() : properties.getSourcetype();
        String host = splunkEvent.getHost() != null ? splunkEvent.getHost() : hostname;

        // 🔥 Build clean Splunk HEC payload manually
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("time", System.currentTimeMillis() / 1000);  // MUST be long (not double)
        payload.put("host", host);
        payload.put("source", source);
        payload.put("sourcetype", sourceType);
        payload.put("index", index);
        payload.put("event", splunkEvent.getEvent());

        String jsonPayload;

        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize Splunk event: {}", e.getMessage());
            return Mono.error(e);
        }
        System.out.println("Sending event to Splunk HEC: {}"+ jsonPayload);
        logger.debug("Sending event to Splunk HEC: {}", jsonPayload);

        return splunkWebClient.post()
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(SplunkResponse.class)
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        System.out.println("Event sent successfully to Splunk");
                        logger.debug("Event sent successfully to Splunk");
                    } else {
                        System.err.println("Event sent successfully to Splunk");
                        logger.warn("Splunk returned non-success response: {}", response);
                    }
                })
                .doOnError(error ->{
                            System.err.println("Error sending event to Splunk: {}"+ error.getMessage());
                            logger.error("Error sending event to Splunk: {}", error.getMessage());
                        }
                       );
    }

    @Override
    public Mono<SplunkResponse> sendBatch(List<SplunkEvent> events) {
        if (!properties.isEnabled()) {
            logger.debug("Splunk HEC is disabled, skipping batch");
            return Mono.just(createDisabledResponse());
        }

        if (events == null || events.isEmpty()) {
            return Mono.just(createSuccessResponse());
        }

        StringBuilder payload = new StringBuilder();
        for (SplunkEvent event : events) {
            // Set defaults
            if (event.getIndex() == null) {
                event.setIndex(properties.getIndex());
            }
            if (event.getSource() == null) {
                event.setSource(properties.getSource());
            }
            if (event.getSourceType() == null) {
                event.setSourceType(properties.getSourcetype());
            }
            if (event.getHost() == null) {
                event.setHost(hostname);
            }

            try {
                payload.append(objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event: {}", e.getMessage());
            }
        }

        logger.debug("Sending batch of {} events to Splunk HEC", events.size());

        return splunkWebClient.post()
                .bodyValue(payload.toString())
                .retrieve()
                .bodyToMono(SplunkResponse.class)
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        logger.debug("Batch of {} events sent successfully to Splunk", events.size());
                    } else {
                        logger.warn("Failed to send batch to Splunk: {}", response);
                    }
                })
                .doOnError(error -> logger.error("Error sending batch to Splunk: {}", error.getMessage()));
    }

    @Override
    public Mono<SplunkResponse> sendLogEvent(LogEventRequest request) {
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("message", request.getMessage());
        eventData.put("level", request.getLevel() != null ? request.getLevel() : "INFO");
        eventData.put("timestamp", request.getTimestamp());

        if (request.getAdditionalFields() != null) {
            eventData.putAll(request.getAdditionalFields());
        }

        SplunkEvent splunkEvent = SplunkEvent.builder()
                .event(eventData)
                .source(request.getSource() != null ? request.getSource() : properties.getSource())
                .sourceType(request.getSourceType() != null ? request.getSourceType() : properties.getSourcetype())
                .host(request.getHost() != null ? request.getHost() : hostname)
                .index(properties.getIndex())
                .build();

        return sendEvent(splunkEvent);
    }

    @Override
    public Mono<SplunkResponse> sendMetric(String metricName, Number metricValue, Map<String, String> dimensions) {
        Map<String, Object> metricData = new LinkedHashMap<>();
        metricData.put("metric_name", metricName);
        metricData.put("_value", metricValue);
        metricData.put("timestamp", System.currentTimeMillis() / 1000);
        if (dimensions != null) {
            metricData.putAll(dimensions);
        }

        SplunkEvent splunkEvent = SplunkEvent.builder()
                .event(metricData)
                .source(properties.getSource())
                .sourceType("metric")
                .host(hostname)
                .index(properties.getIndex())
                .build();

        return sendEvent(splunkEvent);
    }

    @Override
    @Async
    public void queueEvent(SplunkEvent event) {
        eventQueue.add(event);
        logger.debug("Event queued. Queue size: {}", eventQueue.size());

        if (eventQueue.size() >= properties.getBatchSize()) {
            flushQueue();
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${splunk.hec.flush-interval:5000}")
    public void flushQueue() {
        if (eventQueue.isEmpty()) {
            return;
        }

        List<SplunkEvent> batch = new ArrayList<>();
        SplunkEvent event;
        while ((event = eventQueue.poll()) != null && batch.size() < properties.getBatchSize()) {
            batch.add(event);
        }

        if (!batch.isEmpty()) {
            logger.debug("Flushing {} events from queue", batch.size());
            sendBatch(batch).subscribe();
        }
    }

    @Override
    public Mono<Boolean> healthCheck() {
      //  System.out.println("health checking");
        if (!properties.isEnabled()) {
            return Mono.just(false);
        }
      //  System.out.println(properties.isEnabled());
        // Send a test event to verify connectivity
        Map<String, Object> healthEvent = new HashMap<>();
        healthEvent.put("message", "Health check from splunk-service Good Practise");
        healthEvent.put("type", "health_check");
        healthEvent.put("timestamp", System.currentTimeMillis()/ 1000);

        return sendEvent(healthEvent)
                .map(SplunkResponse::isSuccess)
                .onErrorReturn(false);
    }

    private SplunkResponse createDisabledResponse() {
        SplunkResponse response = new SplunkResponse();
        response.setCode(0);
        response.setText("Splunk HEC is disabled");
        return response;
    }

    private SplunkResponse createSuccessResponse() {
        SplunkResponse response = new SplunkResponse();
        response.setCode(0);
        response.setText("Success");
        return response;
    }
}

