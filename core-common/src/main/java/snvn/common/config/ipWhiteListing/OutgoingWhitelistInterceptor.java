package snvn.common.config.ipWhiteListing;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class OutgoingWhitelistInterceptor implements ClientHttpRequestInterceptor {

    private final OutgoingIpWhitelistProperties properties;

    public OutgoingWhitelistInterceptor(OutgoingIpWhitelistProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution)
            throws IOException {

        if (!properties.isEnabled()) {
            return execution.execute(request, body);
        }

        URI uri = request.getURI();

        String host = uri.getHost();
        int port = uri.getPort();
        String protocol = uri.getScheme();

        boolean allowed = properties.getAllowedTargets()
                .stream()
                .filter(OutgoingIpWhitelistProperties.AllowedTarget::isEnabled)
                .anyMatch(d ->
                        d.getAddress().equals(host)
                                && protocol.equalsIgnoreCase(d.getProtocol())
                                && port >= d.getPortStart()
                                && port <= d.getPortEnd()
                );

        if (!allowed) {
            throw new RuntimeException("Outgoing call blocked: " + uri);
        }

        return execution.execute(request, body);
    }
}
