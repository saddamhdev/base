package snvn.common.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class LdapSecurityProvider implements SecurityProvider {

    private final SecurityProperties properties;

    public LdapSecurityProvider(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.getLdap().isEnabled();
    }

    @Override
    public String getName() {
        return "ldap";
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .formLogin(form -> {});    }
}
