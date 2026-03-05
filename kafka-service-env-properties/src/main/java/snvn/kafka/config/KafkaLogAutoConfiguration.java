package snvn.kafka.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
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
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.client.RestClient;
import snvn.log.LogFileServiceImplementation;
import snvn.splunk.SplunkHecProperties;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@EnableConfigurationProperties({KafkaLogProperties.class, SplunkHecProperties.class})
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "kafka-service-log", name = "enabled", havingValue = "true")
public class KafkaLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<String, Object> kafkaLogTemplate(KafkaLogProperties kafkaLogProperties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaLogProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.springframework.kafka.support.serializer.JsonSerializer");
        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(producerFactory);
    }

    @SuppressWarnings({"unchecked", "SpringJavaInjectionPointsAutowiringInspection"})
    @Bean
    public KafkaExternalLogServiceImplementation kafkaExternalLogService(KafkaLogProperties properties,
                                                                         KafkaTemplate<?, ?> kafkaTemplate) {
        return new KafkaExternalLogServiceImplementation(properties, (KafkaTemplate<String, Object>) kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "splunk.hec", name = "enabled", havingValue = "true")
    public RestClient kafkaSplunkRestClient(SplunkHecProperties splunkProperties) {
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
    @ConditionalOnMissingBean(KafkaExternalLogConsumer.class)
    public KafkaExternalLogConsumer kafkaExternalLogConsumer(
            KafkaLogProperties properties,
            @Autowired(required = false) @Qualifier("kafkaSplunkRestClient") RestClient restClient,
            @Autowired(required = false) LogFileServiceImplementation logFileService) {
        return new KafkaExternalLogConsumer(properties, restClient, logFileService);
    }

    @Bean
    @ConditionalOnMissingBean(KafkaLogTopicConfig.class)
    public KafkaLogTopicConfig kafkaLogTopicConfig(KafkaLogProperties properties) {
        return new KafkaLogTopicConfig(properties);
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
            throw new RuntimeException("Failed to create insecure HTTP client for Kafka-Splunk consumer", e);
        }
    }

}


