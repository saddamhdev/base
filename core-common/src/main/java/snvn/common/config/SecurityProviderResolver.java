package snvn.common.config;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityProviderResolver {

    private final List<SecurityProvider> providers;
    private final SecurityProperties properties;

    public SecurityProviderResolver(List<SecurityProvider> providers,
                                    SecurityProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    public SecurityProvider resolve() {

        return providers.stream()
                .filter(p -> p.getName().equals(properties.getProvider()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Unknown provider: " + properties.getProvider()));
    }
}
