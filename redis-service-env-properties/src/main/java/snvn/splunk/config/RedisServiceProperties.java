package snvn.splunk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snvn.redis")
public class RedisServiceProperties {
    private boolean enabled ;

    private String host = "localhost";
    private int port = 6379;

    private String username;
    private String password;

    private int database = 0;
    private boolean ssl = false;

    // timeouts
    private long connectTimeoutMs = 2000;
    private long commandTimeoutMs = 2000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getDatabase() { return database; }
    public void setDatabase(int database) { this.database = database; }
    public boolean isSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }
    public long getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public long getCommandTimeoutMs() { return commandTimeoutMs; }
    public void setCommandTimeoutMs(long commandTimeoutMs) { this.commandTimeoutMs = commandTimeoutMs; }
}