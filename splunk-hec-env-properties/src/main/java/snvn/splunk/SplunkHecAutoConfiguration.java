package snvn.splunk;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(SplunkHecProperties.class)
@ConditionalOnProperty(prefix = "splunk.hec", name = "enabled", havingValue = "true")
public class SplunkHecAutoConfiguration {

    @Bean
    public SplunkExternalLogService splunkExternalLogService(SplunkHecProperties properties) {
        return new SplunkExternalLogService(properties);
    }

}
