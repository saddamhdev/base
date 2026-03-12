package snvn.gatewayservice.config.ipWhiteListing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
@ConfigurationProperties(prefix = "incoming-ip-whitelist")
public class IncomingIpWhitelistProperties {

    private boolean enabled;
    private List<AllowedIp> allowedIps;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<AllowedIp> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<AllowedIp> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public static class AllowedIp {

        private boolean enabled;
        private String service;
        private String address;
        private String protocol;
        private int portStart;
        private int portEnd;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public int getPortStart() {
            return portStart;
        }

        public void setPortStart(int portStart) {
            this.portStart = portStart;
        }

        public int getPortEnd() {
            return portEnd;
        }

        public void setPortEnd(int portEnd) {
            this.portEnd = portEnd;
        }
    }
}