package snvn.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Configuration to enable automatic MDC context propagation
 * across reactive/reactor threads.
 *
 * Without this, MDC values (traceId, spanId, correlationId) are lost
 * when execution switches from servlet threads to reactor threads
 * (e.g., when WebClient receives a response).
 */
@Configuration
public class ReactorContextPropagationConfig {

    private static final Logger log = LoggerFactory.getLogger(ReactorContextPropagationConfig.class);

    @PostConstruct
    public void enableContextPropagation() {
        // Enable automatic context propagation for Reactor
        // This ensures MDC values are propagated to reactor threads
        Hooks.enableAutomaticContextPropagation();
        log.info("Reactor automatic context propagation enabled for MDC");
    }
}

