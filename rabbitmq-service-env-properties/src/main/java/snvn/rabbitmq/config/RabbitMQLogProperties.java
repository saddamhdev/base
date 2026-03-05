package snvn.rabbitmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq-service-log")
public class RabbitMQLogProperties {
    private String index = "main";
    private String sourcetype = "_json";
    private boolean enabled;
    private boolean autoStartup = true;
    private String exchange = "rabbitmq-log-exchange";
    private String routingKey = "rabbitmq-log-routing-key";
    private String queue = "rabbitmq-log-queue";
    private String source = "spring-boot";
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

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

