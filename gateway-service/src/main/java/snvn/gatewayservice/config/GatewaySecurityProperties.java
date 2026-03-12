package snvn.gatewayservice.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security")
public class GatewaySecurityProperties {

    private Keycloak keycloak = new Keycloak();
    private ApiKey apiKey = new ApiKey();
    private RateLimit rateLimit = new RateLimit();
    private IpFilter ipFilter = new IpFilter();

    public Keycloak getKeycloak() {
        return keycloak;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public IpFilter getIpFilter() {
        return ipFilter;
    }

    public static class Keycloak {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ApiKey {
        private boolean enabled;
        private String header;
        private String value;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class RateLimit {
        private boolean enabled;
        private int requestsPerMinute;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    public static class IpFilter {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}