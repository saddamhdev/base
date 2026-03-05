package snvn.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Default no-op implementation when no external log service is configured.
 */
public class NoOpExternalLogService extends AbstractExternalLogService {

    private static final Logger log = LoggerFactory.getLogger(NoOpExternalLogService.class);

    @Override
    public void sendLogFile(String level, String message, Map<String, Object> context) {
        log.debug("External logging not configured - message: {}", message);
    }

    @Override
    public void sendLogSplunk(String level, String message, Map<String, Object> context) {
        log.debug("External logging not configured - message: {}", message);
    }

    @Override
    public void sendLogRabbitMQ(String level, String message, Map<String, Object> context) {
        log.debug("External logging not configured - message: {}", message);
    }

    @Override
    public void sendErrorLogFile(String message, String throwable, Map<String, Object> context) {
        log.debug("External logging not configured - error: {}", message);
    }

    @Override
    public void sendErrorLogSplunk(String message, String throwable, Map<String, Object> context) {
        log.debug("External logging not configured - error: {}", message);
    }

    @Override
    public void sendErrorLogRabbitMQ(String message, String throwable, Map<String, Object> context) {
        log.debug("External logging not configured - error: {}", message);
    }

    @Override
    public void sendLogKafka(String level, String message, Map<String, Object> context) {
        log.debug("External logging not configured - message: {}", message);
    }

    @Override
    public void sendErrorLogKafka(String message, String throwable, Map<String, Object> context) {
        log.debug("External logging not configured - error: {}", message);
    }
}
