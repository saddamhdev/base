package snvn.log;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "log-file", name = "enabled", havingValue = "true")
public class LogFileAutoConfiguration {

    @Bean
    public LogFileServiceImplementation logFileServiceImplementation(LogProperties properties) {
        return new LogFileServiceImplementation(properties);
    }

}

