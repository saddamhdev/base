package snvn.log;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "log-file")
public class LogProperties {

    private boolean enabled;
    private String index = "main";
    private String source = "spring-boot";
    private String sourcetype = "_json";
    private String splunkFile = "logs/splunk-events.log";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getSplunkFile() {
        return splunkFile;
    }

    public void setSplunkFile(String splunkFile) {
        this.splunkFile = splunkFile;
    }
}
