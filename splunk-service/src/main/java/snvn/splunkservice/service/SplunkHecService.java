package snvn.splunkservice.service;

import snvn.splunkservice.model.LogEventRequest;
import snvn.splunkservice.model.SplunkEvent;
import snvn.splunkservice.model.SplunkResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Splunk HEC operations
 */
public interface SplunkHecService {

    /**
     * Send a single event to Splunk HEC
     *
     * @param event the event data (can be a string or object)
     * @return Mono of SplunkResponse
     */
    Mono<SplunkResponse> sendEvent(Object event);

    /**
     * Send a single SplunkEvent to Splunk HEC
     *
     * @param splunkEvent the Splunk event
     * @return Mono of SplunkResponse
     */
    Mono<SplunkResponse> sendEvent(SplunkEvent splunkEvent);

    /**
     * Send multiple events to Splunk HEC in batch
     *
     * @param events list of events
     * @return Mono of SplunkResponse
     */
    Mono<SplunkResponse> sendBatch(List<SplunkEvent> events);

    /**
     * Send a log event using LogEventRequest
     *
     * @param request the log event request
     * @return Mono of SplunkResponse
     */
    Mono<SplunkResponse> sendLogEvent(LogEventRequest request);

    /**
     * Send a metric event to Splunk HEC
     *
     * @param metricName  the metric name
     * @param metricValue the metric value
     * @param dimensions  additional dimensions/tags
     * @return Mono of SplunkResponse
     */
    Mono<SplunkResponse> sendMetric(String metricName, Number metricValue, Map<String, String> dimensions);

    /**
     * Queue an event for batch processing
     *
     * @param event the event to queue
     */
    void queueEvent(SplunkEvent event);

    /**
     * Flush all queued events immediately
     */
    void flushQueue();

    /**
     * Check if Splunk HEC is healthy and reachable
     *
     * @return Mono of boolean indicating health status
     */
    Mono<Boolean> healthCheck();
}

