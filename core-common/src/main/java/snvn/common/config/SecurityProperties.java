package snvn.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private String provider;

    private Provider keycloak = new Provider();
    private Provider okta = new Provider();
    private Provider auth0 = new Provider();
    private Provider azure = new Provider();
    private CustomProvider custom = new CustomProvider();

    private Ldap ldap = new Ldap();
    private Mtls mtls = new Mtls();

    private ApiKey apiKey = new ApiKey();
    private RateLimit rateLimit = new RateLimit();
    private IpFilter ipFilter = new IpFilter();

    // ---------------- PROVIDERS ----------------

    public static class Provider {

        private boolean enabled;
        private String issuerUri;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getIssuerUri() { return issuerUri; }
        public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
    }

    public static class CustomProvider extends Provider {

        private String jwkSetUri;

        public String getJwkSetUri() { return jwkSetUri; }
        public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
    }

    // ---------------- LDAP ----------------

    public static class Ldap {

        private boolean enabled;
        private String url;
        private String baseDn;
        private String userDnPattern;
        private String managerDn;
        private String managerPassword;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getBaseDn() { return baseDn; }
        public void setBaseDn(String baseDn) { this.baseDn = baseDn; }

        public String getUserDnPattern() { return userDnPattern; }
        public void setUserDnPattern(String userDnPattern) { this.userDnPattern = userDnPattern; }

        public String getManagerDn() { return managerDn; }
        public void setManagerDn(String managerDn) { this.managerDn = managerDn; }

        public String getManagerPassword() { return managerPassword; }
        public void setManagerPassword(String managerPassword) { this.managerPassword = managerPassword; }
    }

    // ---------------- MTLS ----------------

    public static class Mtls {

        private boolean enabled;
        private String trustStore;
        private String trustStorePassword;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getTrustStore() { return trustStore; }
        public void setTrustStore(String trustStore) { this.trustStore = trustStore; }

        public String getTrustStorePassword() { return trustStorePassword; }
        public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    }

    // ---------------- API KEY ----------------

    public static class ApiKey {

        private boolean enabled;
        private String header;
        private String value;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    // ---------------- RATE LIMIT ----------------

    public static class RateLimit {

        private boolean enabled;
        private int requestsPerMinute;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    }

    // ---------------- IP FILTER ----------------

    public static class IpFilter {

        private boolean enabled;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    // getters

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Provider getKeycloak() { return keycloak; }
    public Provider getOkta() { return okta; }
    public Provider getAuth0() { return auth0; }
    public Provider getAzure() { return azure; }
    public CustomProvider getCustom() { return custom; }

    public Ldap getLdap() { return ldap; }
    public Mtls getMtls() { return mtls; }

    public ApiKey getApiKey() { return apiKey; }
    public RateLimit getRateLimit() { return rateLimit; }
    public IpFilter getIpFilter() { return ipFilter; }
}