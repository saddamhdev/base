package snvn.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import snvn.common.config.SecurityProvider;
import snvn.common.config.SecurityProviderResolver;
import snvn.common.config.ServiceLogProperties;
import snvn.common.logging.ExternalLogService;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityConfig.class);

    private final SecurityProviderResolver resolver;
    private final ServiceLogProperties logProperties;
    private final ExternalLogService externalLogService;

    public SecurityConfig(SecurityProviderResolver resolver,
                          ServiceLogProperties logProperties,
                          ExternalLogService externalLogService) {

        this.resolver = resolver;
        this.logProperties = logProperties;
        this.externalLogService = externalLogService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        SecurityProvider provider = resolver.resolve();

        Map<String, Object> context = new HashMap<>();

        context.put("logger", log.getName() + ".securityFilterChain()");
        context.put("service", "user-service");
        context.put("provider", provider.getName());
        context.put("enabled", provider.isEnabled());

        if (provider.isEnabled()) {

            log.info("Security ENABLED using provider={}", provider.getName());

            sendLog("INFO",
                    "User-service security initialized with provider=" + provider.getName(),
                    context);

            provider.configure(http);

        } else {

            log.warn("Security DISABLED for user-service");

            sendLog("WARN",
                    "User-service running in PERMIT-ALL mode",
                    context);

            http.authorizeHttpRequests(auth ->
                    auth.anyRequest().permitAll());
        }

        return http.build();
    }

    private void sendLog(String level, String message, Map<String, Object> context) {

        if (logProperties.isLogfileEnabled()) {
            externalLogService.sendLogFile(level, message, context);
        }

        if (logProperties.isSplunkEnabled()) {
            externalLogService.sendLogSplunk(level, message, context);
        }

        if (logProperties.isRabbitmqEnabled()) {
            externalLogService.sendLogRabbitMQ(level, message, context);
        }

        if (logProperties.isKafkaEnabled()) {
            externalLogService.sendLogKafka(level, message, context);
        }
    }
}