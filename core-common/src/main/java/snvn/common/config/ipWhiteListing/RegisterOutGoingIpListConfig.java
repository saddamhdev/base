package snvn.common.config.ipWhiteListing;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableConfigurationProperties(OutgoingIpWhitelistProperties.class)
public class RegisterOutGoingIpListConfig {


}