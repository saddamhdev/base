package snvn.splunkservice.service;

import org.springframework.stereotype.Service;
import snvn.common.logging.ExternalLogService;

import java.util.Map;

@Service
abstract public class SplunkLogService implements ExternalLogService {

    @Override
    public void sendLogFile(String level, String message, Map<String, Object> context) {
        // Send to Splunk HEC endpoint
        System.out.println("splunkLogService.sendLog");
    }

    @Override
    public void sendErrorLogFile(String message, String throwable, Map<String, Object> context) {
        // Send error details to Splunk
        System.out.println("splunkLogService.sendErrorLog");
    }
}