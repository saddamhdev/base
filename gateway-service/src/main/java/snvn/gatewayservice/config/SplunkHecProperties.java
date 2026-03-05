package snvn.gatewayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Splunk HTTP Event Collector (HEC).
 *
 * Example configuration in application.yml:
 * <pre>
 * splunk:
 *   hec:
 *     enabled: true
 *     url: https://splunk-server:8088/services/collector
 *     token: your-hec-token
 *     index: main
 *     source: gateway-service
 *     sourcetype: _json
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "splunk.hec")
public class SplunkHecProperties {

    /**
     * Enable/disable Splunk HEC logging
     */
    private boolean enabled ;

    /**
     * Splunk HEC endpoint URL
     */
    private String url = "";

    /**
     * Splunk HEC authentication token
     */
    private String token = "";

    /**
     * Splunk index to send events to
     */
    private String index = "main";

    /**
     * Source identifier for events
     */
    private String source ;

    /**
     * Source type for events
     */
    private String sourcetype = "_json";

    /**
     * Enable/disable SSL certificate verification.
     * Set to false for development with self-signed certificates.
     */
    private boolean sslVerify = true;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourcetype() {
        return sourcetype;
    }

    public void setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
    }

    public boolean isSslVerify() {
        return sslVerify;
    }

    public void setSslVerify(boolean sslVerify) {
        this.sslVerify = sslVerify;
    }
}

