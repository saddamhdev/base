package snvn.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import snvn.log.LogFileServiceImplementation;
import snvn.splunk.SplunkHecProperties;

import javax.net.ssl.SSLContext;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties({RabbitMQLogProperties.class, SplunkHecProperties.class})
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "rabbitmq-service-log", name = "enabled", havingValue = "true")
public class RabbitMQLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new MessageConverter() {
            @Override
            public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
                try {
                    messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    messageProperties.setContentEncoding("UTF-8");
                    byte[] bytes = objectMapper.writeValueAsBytes(object);
                    return new Message(bytes, messageProperties);
                } catch (Exception e) {
                    throw new MessageConversionException("Failed to convert object to JSON Message", e);
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public Object fromMessage(Message message) throws MessageConversionException {
                try {
                    return objectMapper.readValue(message.getBody(), Map.class);
                } catch (Exception e) {
                    throw new MessageConversionException("Failed to convert JSON Message to object", e);
                }
            }
        };
    }

    @Bean
    public RabbitMQExternalLogServiceImplementation rabbitMQExternalLogService(RabbitMQLogProperties properties, RabbitTemplate rabbitTemplate) {
        return new RabbitMQExternalLogServiceImplementation(properties, rabbitTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "splunk.hec", name = "enabled", havingValue = "true")
    public RestClient rabbitMQSplunkRestClient(SplunkHecProperties splunkProperties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(splunkProperties.getUrl())
                .defaultHeader("Authorization", "Splunk " + splunkProperties.getToken())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (!splunkProperties.isSslVerify()) {
            builder.requestFactory(createInsecureRequestFactory());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(RabbitMQExternalLogConsumer.class)
    public RabbitMQExternalLogConsumer rabbitMQExternalLogConsumer(
            RabbitMQLogProperties properties,
            @Autowired(required = false) @Qualifier("rabbitMQSplunkRestClient") RestClient restClient,
            @Autowired(required = false) LogFileServiceImplementation logFileService) {
        return new RabbitMQExternalLogConsumer(properties, restClient, logFileService);
    }

    @Bean
    @ConditionalOnMissingBean(LogExchangeConfig.class)
    public LogExchangeConfig logExchangeConfig(RabbitMQLogProperties properties) {
        return new LogExchangeConfig(properties);
    }

    /**
     * DEV ONLY — disables SSL verification for self-signed Splunk HEC certs.
     */
    private HttpComponentsClientHttpRequestFactory createInsecureRequestFactory() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory =
                    SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build();

            HttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(
                            PoolingHttpClientConnectionManagerBuilder.create()
                                    .setSSLSocketFactory(sslSocketFactory)
                                    .build()
                    )
                    .build();

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure HTTP client for RabbitMQ-Splunk consumer", e);
        }
    }

}
