package snvn.configservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config/health")
public class HealthController {

    @GetMapping
    public String health() {
        return "Config Service is running!";
    }

    @GetMapping("/status")
    public ConfigStatus getStatus() {
        return new ConfigStatus("ACTIVE", "Config Service is operational");
    }

    public static class ConfigStatus {
        private String status;
        private String message;

        public ConfigStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

