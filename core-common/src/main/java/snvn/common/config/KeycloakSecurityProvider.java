package snvn.common.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class KeycloakSecurityProvider implements SecurityProvider {

    private final SecurityProperties properties;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    public KeycloakSecurityProvider(SecurityProperties properties,
                                    JwtAuthEntryPoint jwtAuthEntryPoint) {
        this.properties = properties;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    @Override
    public boolean isEnabled() {
        return properties.getKeycloak().isEnabled();
    }

    @Override
    public String getName() {
        return "keycloak";
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(Customizer.withDefaults())
                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthEntryPoint)
                );
    }
}