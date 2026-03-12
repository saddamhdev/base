package snvn.common.config.ipWhiteListing;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@EnableConfigurationProperties(IncomingIpWhitelistProperties.class)
public class DependencyConfig {
}
