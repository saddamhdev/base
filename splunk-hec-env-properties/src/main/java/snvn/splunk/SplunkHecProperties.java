package snvn.splunk;

import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "splunk.hec")
public class SplunkHecProperties {
    private boolean enabled;
    private String url = "";
    private String token = "";
    private String index = "main";
    private String source;
    private String sourcetype = "_json";
    private boolean sslVerify = false;

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
// getters/setters

    @Override
    public String toString() {
        return "SplunkHecProperties{" +
                "enabled=" + enabled +
                ", url='" + url + '\'' +
                ", token='" + token + '\'' +
                ", index='" + index + '\'' +
                ", source='" + source + '\'' +
                ", sourcetype='" + sourcetype + '\'' +
                ", sslVerify=" + sslVerify +
                '}';
    }
}