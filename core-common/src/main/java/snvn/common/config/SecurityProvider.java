package snvn.common.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface SecurityProvider {

    boolean isEnabled();

    void configure(HttpSecurity http) throws Exception;

    String getName();
}
