package snvn.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds to gateway-service-log.* in YAML.
 * Controls which log channels the gateway filters send to.
 *
 * <pre>
 * gateway-service-log:
 *   logfile:
 *     enabled: false
 *   splunk:
 *     enabled: false
 *   rabbitmq:
 *     enabled: true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "service-log")
public class ServiceLogProperties {

    private Channel logfile = new Channel();
    private Channel splunk = new Channel();
    private Channel rabbitmq = new Channel();
    private Channel kafka= new Channel();

    public Channel getLogfile() {
        return logfile;
    }

    public void setLogfile(Channel logfile) {
        this.logfile = logfile;
    }

    public Channel getSplunk() {
        return splunk;
    }

    public void setSplunk(Channel splunk) {
        this.splunk = splunk;
    }

    public Channel getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(Channel rabbitmq) {
        this.rabbitmq = rabbitmq;
    }
    public Channel getKafka() {
        return kafka;
    }

    public void setKafka(Channel kafka) {
        this.kafka = kafka;
    }
    public boolean isLogfileEnabled() {
        return logfile.isEnabled();
    }

    public boolean isSplunkEnabled() {
        return splunk.isEnabled();
    }

    public boolean isRabbitmqEnabled() {
        return rabbitmq.isEnabled();
    }

  public boolean isKafkaEnabled(){
        return kafka.isEnabled();
  }

    @Override
    public String toString() {
        return "GatewayServiceLogProperties{" +
                "logfile=" + logfile.isEnabled() +
                ", splunk=" + splunk.isEnabled() +
                ", rabbitmq=" + rabbitmq.isEnabled() +
                ", kafka=" + kafka.isEnabled() +

                '}';
    }

    public static class Channel {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

