package snvn.gatewayservice.config.ipWhiteListing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IncomingIpWhitelistProperties.class)
public class DependencyConfig {
}