package snvn.gatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import snvn.common.logging.ExternalLogService;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityConfig.class);

    private final GatewaySecurityProperties securityProperties;
    private final GatewayServiceLogProperties logProperties;
    private final ExternalLogService externalLogService;

    public SecurityConfig(GatewaySecurityProperties securityProperties,
                          GatewayServiceLogProperties logProperties,
                          ExternalLogService externalLogService) {

        this.securityProperties = securityProperties;
        this.logProperties = logProperties;
        this.externalLogService = externalLogService;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        boolean keycloakEnabled = securityProperties.getKeycloak().isEnabled();

        Map<String, Object> context = new HashMap<>();
        context.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");
        context.put("service", "gateway-service");
        context.put("keycloakEnabled", keycloakEnabled);

        if (keycloakEnabled) {

            log.info("Keycloak security ENABLED");

            sendLog("INFO",
                    "Gateway security initialized with Keycloak JWT authentication",
                    context);

            http
                    .authorizeExchange(exchange -> exchange
                            .pathMatchers("/actuator/**").permitAll()
                            .anyExchange().authenticated()
                    )
                    .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));

        } else {

            log.warn("Keycloak security DISABLED");

            sendLog("WARN",
                    "Gateway security running in PERMIT-ALL mode",
                    context);

            http
                    .authorizeExchange(exchange ->
                            exchange.anyExchange().permitAll()
                    );
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