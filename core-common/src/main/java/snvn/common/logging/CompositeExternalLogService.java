package snvn.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import snvn.common.config.ServiceLogProperties;

import java.util.List;
import java.util.Map;

/**
 * Composite implementation that delegates to all available ExternalLogService beans.
 * When both log-file and splunk-hec are enabled, logs are sent to both.
 * When none are enabled, falls back to NoOp behavior.
 */
public class CompositeExternalLogService implements ExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(CompositeExternalLogService.class);

    private final List<ExternalLogService> delegates;

    public CompositeExternalLogService(List<ExternalLogService> delegates) {
        this.delegates = delegates;
        log.info("CompositeExternalLogService initialized with {} delegate(s): {}",
                delegates.size(),
                delegates.stream().map(d -> d.getClass().getSimpleName()).toList());
    }


    @Override
    public void sendLogFile(String level, String message, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendLogFile(level, message, context);
            } catch (Exception e) {
                log.warn("Failed to send log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendLogSplunk(String level, String message, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendLogSplunk(level, message, context);
            } catch (Exception e) {
                log.warn("Failed to send log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendLogRabbitMQ(String level, String message, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendLogRabbitMQ(level, message, context);
            } catch (Exception e) {
                log.warn("Failed to send log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendErrorLogFile(String message, String throwable, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendErrorLogFile(message, throwable, context);
            } catch (Exception e) {
                log.warn("Failed to send error log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendErrorLogSplunk(String message, String throwable, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendErrorLogSplunk(message, throwable, context);
            } catch (Exception e) {
                log.warn("Failed to send error log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendErrorLogRabbitMQ(String message, String throwable, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendErrorLogRabbitMQ(message, throwable, context);
            } catch (Exception e) {
                log.warn("Failed to send error log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendLogKafka(String level, String message, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendLogKafka(level, message, context);
            } catch (Exception e) {
                log.warn("Failed to send log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendErrorLogKafka(String message, String throwable, Map<String, Object> context) {
        for (ExternalLogService delegate : delegates) {
            try {
                delegate.sendErrorLogKafka(message, throwable, context);
            } catch (Exception e) {
                log.warn("Failed to send error log via {}: {}", delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}

