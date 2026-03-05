package snvn.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka-service-log")
public class KafkaLogProperties {
    private String index = "main";
    private String sourcetype = "_json";
    private boolean enabled;
    private boolean autoStartup = true;
    private String topic = "kafka-log-topic";
    private String groupId = "kafka-log-group";
    private String source = "spring-boot";
    private String bootstrapServers = "localhost:9092";
    private boolean logfileEnabled;
    private boolean splunkEnabled;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getSourcetype() {
        return sourcetype;
    }

    public void setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoStartup() {
        return autoStartup;
    }

    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public boolean isLogfileEnabled() {
        return logfileEnabled;
    }

    public void setLogfileEnabled(boolean logfileEnabled) {
        this.logfileEnabled = logfileEnabled;
    }

    public boolean isSplunkEnabled() {
        return splunkEnabled;
    }

    public void setSplunkEnabled(boolean splunkEnabled) {
        this.splunkEnabled = splunkEnabled;
    }
}

