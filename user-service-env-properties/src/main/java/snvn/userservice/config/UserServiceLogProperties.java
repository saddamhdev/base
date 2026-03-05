package snvn.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for user-service logging backends.
 * <p>
 * Controls which ExternalLogService implementations are active for user-service.
 *
 * <pre>
 * user-service-log:
 *   logfile:
 *     enabled: true
 *   splunk:
 *     enabled: true
 *   rabbitmq:
 *     enabled: true
 *   kafka:
 *     enabled: true
 * </pre>
 */
@ConfigurationProperties(prefix = "user-service-log")
public class UserServiceLogProperties {

    private final Logfile logfile = new Logfile();
    private final Splunk splunk = new Splunk();
    private final Rabbitmq rabbitmq = new Rabbitmq();
    private final Kafka kafka=new Kafka();

    public Logfile getLogfile() {
        return logfile;
    }

    public Splunk getSplunk() {
        return splunk;
    }

    public Rabbitmq getRabbitmq() {
        return rabbitmq;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Logfile {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Splunk {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Rabbitmq {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Kafka {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Override
    public String toString() {
        return "UserServiceLogProperties{" +
                "logfile.enabled=" + logfile.isEnabled() +
                ", splunk.enabled=" + splunk.isEnabled() +
                ", rabbitmq.enabled=" + rabbitmq.isEnabled() +
                ", kafka.enabled=" + kafka.isEnabled() +
                '}';
    }
}

