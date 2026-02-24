package snvn.splunkservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "splunk.hec")
public class SplunkHecProperties {

    // Getters and Setters
    private String url;
    private String token;
    private String index;
    private String source;
    private String sourcetype;
    private boolean enabled;
    private int batchSize = 100;
    private long flushInterval = 5000;
    private boolean sslVerify = false;

}

