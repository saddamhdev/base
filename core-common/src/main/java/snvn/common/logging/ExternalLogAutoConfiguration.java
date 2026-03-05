package snvn.common.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Auto-configuration that creates the composite ExternalLogService.
 * <p>
 * This collects ALL ExternalLogService beans (file, splunk-hec, etc.)
 * and wraps them in a CompositeExternalLogService.
 * If no implementations are enabled, it falls back to NoOpExternalLogService.
 * <p>
 * Any module that injects ExternalLogService will get this composite,
 * which fans out to all enabled implementations.
 */
@AutoConfiguration(
        afterName = {
                "snvn.log.LogFileAutoConfiguration",
                "snvn.splunk.SplunkHecAutoConfiguration",
                "snvn.rabbitmq.config.RabbitMQLogAutoConfiguration"
        }
)
public class ExternalLogAutoConfiguration {

    @Bean
    @Primary
    public ExternalLogService compositeExternalLogService(List<ExternalLogService> delegates) {
        if (delegates.isEmpty()) {
            return new NoOpExternalLogService();
        }
        if (delegates.size() == 1) {
            return delegates.get(0);
        }
        return new CompositeExternalLogService(delegates);
    }
}

