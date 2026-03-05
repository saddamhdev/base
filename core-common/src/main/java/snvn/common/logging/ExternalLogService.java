package snvn.common.logging;


import java.util.Map;

/**
 * Interface for external log forwarding (e.g., Splunk).
 * Implementations should be provided by service modules.
 */
public interface ExternalLogService {

    /**
     * Send log data to external logging system
     */
    void sendLogFile(String level, String message, Map<String, Object> context);
    void sendLogSplunk(String level, String message, Map<String, Object> context);
    void sendLogRabbitMQ(String level, String message, Map<String, Object> context);
    void sendLogKafka(String level, String message, Map<String, Object> context);

    /**
     * Send error log with exception details
     */
    void sendErrorLogFile(String message, String throwable, Map<String, Object> context);
    void sendErrorLogSplunk(String message, String throwable, Map<String, Object> context);
    void sendErrorLogRabbitMQ(String message, String throwable, Map<String, Object> context);
    void sendErrorLogKafka(String message, String throwable, Map<String, Object> context);
}