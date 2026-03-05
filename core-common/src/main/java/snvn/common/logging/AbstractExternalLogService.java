package snvn.common.logging;

import java.util.Map;

/**
 * Abstract base class that provides no-op defaults for all ExternalLogService methods.
 * Each module extends this and overrides ONLY the methods it handles:
 * <ul>
 *   <li>log-env-properties → overrides sendLogFile, sendErrorLogFile</li>
 *   <li>splunk-hec-env-properties → overrides sendLogSplunk, sendErrorLogSplunk</li>
 *   <li>rabbitmq-service-env-properties → overrides sendLogRabbitMQ, sendErrorLogRabbitMQ</li>
 *   <li>kafka-service-env-properties → overrides sendLogKafka, sendErrorLogKafka</li>
 * </ul>
 */
public abstract class AbstractExternalLogService implements ExternalLogService {

    @Override
    public void sendLogFile(String level, String message, Map<String, Object> context) {
    }

    @Override
    public void sendLogSplunk(String level, String message, Map<String, Object> context) {
    }

    @Override
    public void sendLogRabbitMQ(String level, String message, Map<String, Object> context) {
    }

    @Override
    public void sendErrorLogFile(String message, String throwable, Map<String, Object> context) {
    }

    @Override
    public void sendErrorLogSplunk(String message, String throwable, Map<String, Object> context) {
    }

    @Override
    public void sendErrorLogRabbitMQ(String message, String throwable, Map<String, Object> context) {
    }

    @Override
    public void sendLogKafka(String level, String message, Map<String, Object> context) {
    }

    @Override
    public void sendErrorLogKafka(String message, String throwable, Map<String, Object> context) {
    }
}

