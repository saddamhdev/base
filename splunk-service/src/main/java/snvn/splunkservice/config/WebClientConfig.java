package snvn.splunkservice.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
public class WebClientConfig {

    private final SplunkHecProperties splunkHecProperties;

    public WebClientConfig(SplunkHecProperties splunkHecProperties) {
        this.splunkHecProperties = splunkHecProperties;
    }

    @Bean
    public WebClient splunkWebClient() throws SSLException {
        HttpClient httpClient;

        if (!splunkHecProperties.isSslVerify()) {
            // Disable SSL verification for development/testing
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            httpClient = HttpClient.create()
                    .secure(t -> t.sslContext(sslContext));
        } else {
            httpClient = HttpClient.create();
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(splunkHecProperties.getUrl())
                .defaultHeader("Authorization", "Splunk " + splunkHecProperties.getToken())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}

