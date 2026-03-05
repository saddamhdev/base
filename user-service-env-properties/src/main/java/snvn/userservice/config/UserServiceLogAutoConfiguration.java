package snvn.userservice.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(UserServiceLogProperties.class)
public class UserServiceLogAutoConfiguration {
}

