package snvn.common.config.ipWhiteListing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "outgoing-ip-whitelist")
public class OutgoingIpWhitelistProperties {

    private boolean enabled;
    private List<AllowedTarget> allowedTargets;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<AllowedTarget> getAllowedTargets() {
        return allowedTargets;
    }

    public void setAllowedTargets(List<AllowedTarget> allowedTargets) {
        this.allowedTargets = allowedTargets;
    }

    public static class AllowedTarget {

        private boolean enabled;
        private String service;
        private String address;
        private String protocol;
        private int portStart;
        private int portEnd;

        // getters setters

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
