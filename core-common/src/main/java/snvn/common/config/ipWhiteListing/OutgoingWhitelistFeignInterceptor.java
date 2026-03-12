/*
package snvn.common.config.ipWhiteListing;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class OutgoingWhitelistFeignInterceptor implements RequestInterceptor {

    private final OutgoingIpWhitelistProperties properties;

    public OutgoingWhitelistFeignInterceptor(OutgoingIpWhitelistProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate template) {

        URI uri = URI.create(template.url());

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
            throw new RuntimeException("Feign call blocked: " + uri);
        }
    }
}
*/
