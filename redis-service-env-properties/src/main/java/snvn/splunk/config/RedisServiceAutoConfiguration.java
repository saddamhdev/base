package snvn.splunk.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import snvn.common.redis.RedisService;
import snvn.splunk.service.RedisServiceImpl;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(RedisServiceProperties.class)
@ConditionalOnClass({ RedisConnectionFactory.class, LettuceConnectionFactory.class })
@ConditionalOnProperty(
        prefix = "snvn.redis",
        name = "enabled",
        havingValue = "true"
)public class RedisServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(RedisServiceProperties p) {

        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration(p.getHost(), p.getPort());

        standalone.setDatabase(p.getDatabase());

        if (p.getUsername() != null && !p.getUsername().isBlank()) {
            standalone.setUsername(p.getUsername());
        }

        if (p.getPassword() != null && !p.getPassword().isBlank()) {
            standalone.setPassword(RedisPassword.of(p.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofMillis(p.getCommandTimeoutMs()))
                        .shutdownTimeout(Duration.ofMillis(500));

        if (p.isSsl()) {
            builder.useSsl();   // ✅ FIXED
        }

        return new LettuceConnectionFactory(standalone, builder.build());
    }

    @Bean
    @ConditionalOnMissingBean(RedisService.class)
    public RedisService redisService(StringRedisTemplate template) {
        return new RedisServiceImpl(template);
    }
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

}