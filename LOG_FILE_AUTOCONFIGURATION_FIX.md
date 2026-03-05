# Log File AutoConfiguration Fix — Splunk-Compatible JSON Output

## Date: March 2, 2026

---

## Problem

1. `log-file-dev.yml` was not executing when `log-file.enabled: true` was set — `LogFileServiceImplementation` bean was never created.
2. Log output used `HashMap.toString()` format (e.g. `{event={level=INFO, message=...}}`) — **not parseable by Splunk**.

---

## Root Cause Analysis

### Issue 1: Bean never created — Splunk's NoOp won the race

Both `SplunkHecAutoConfiguration` and `LogFileAutoConfiguration` competed for the same `ExternalLogService` bean type.

**Startup sequence (BEFORE fix):**

```
1. splunk-hec-dev.yml loads → splunk.hec.enabled = false
2. SplunkHecAutoConfiguration:
   - splunkExternalLogService() → SKIPPED (enabled ≠ true)
   - noOpExternalLogService()  → CREATED ✅ (ExternalLogService now exists)
3. LogFileAutoConfiguration:
   - logFileService()          → SKIPPED ❌ (@ConditionalOnMissingBean fails)
```

**Result:** `NoOpExternalLogService` was always injected. `LogFileServiceImplementation` was never instantiated.

### Issue 2: Missing auto-configuration registration

No `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file existed. Spring Boot 4.x could not discover the auto-configuration class from the external JAR.

### Issue 3: Wrong annotation

`@Configuration` was used instead of `@AutoConfiguration` — preventing participation in Spring Boot's auto-configuration ordering lifecycle.

### Issue 4: Log output not Splunk-friendly

`HashMap.toString()` was used to serialize events:
```
LogFileService: {event={level=INFO, message=User created}, time=1740000000}
```
Splunk cannot parse this. It needs proper JSON with HEC envelope fields.

---

## Solution

### Files Changed

| # | File | Action |
|---|------|--------|
| 1 | `LogFileAutoConfiguration.java` | `@Configuration` → `@AutoConfiguration`, removed `@ConditionalOnMissingBean`, added `@Primary` |
| 2 | `LogFileServiceImplementation.java` | Full rewrite — Splunk HEC JSON format with `ObjectMapper`, `MDC` trace fields, `LinkedHashMap` ordering |
| 3 | `LogProperties.java` | Removed `@Component`, added `index`, `source`, `sourcetype` properties |
| 4 | `pom.xml` | Added `jackson-databind` and `spring-boot-autoconfigure` dependencies |
| 5 | `log-file-*.yml` (all 4 profiles) | Added `index`, `source`, `sourcetype` config |
| 6 | `AutoConfiguration.imports` | **NEW** — registers `LogFileAutoConfiguration` for Spring Boot discovery |
| 7 | Splunk `AutoConfiguration.imports` | **NEW** — registers `SplunkHecAutoConfiguration` for consistency |

---

### 1. `LogFileAutoConfiguration.java`

**Path:** `log-env-properties/src/main/java/snvn/log/LogFileAutoConfiguration.java`

**BEFORE:**
```java
@Configuration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "log-file", name = "enabled", havingValue = "true")
public class LogFileAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ExternalLogService.class)  // ❌ Blocked by Splunk's NoOp
    public ExternalLogService logFileService(LogProperties properties) {
        return new LogFileServiceImplementation(properties);
    }
}
```

**AFTER:**
```java
@AutoConfiguration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "log-file", name = "enabled", havingValue = "true")
public class LogFileAutoConfiguration {
    @Bean
    @Primary                                              // ✅ Wins over Splunk's NoOp
    public ExternalLogService logFileService(LogProperties properties) {
        return new LogFileServiceImplementation(properties);
    }
}
```

---

### 2. `LogFileServiceImplementation.java`

**Path:** `log-env-properties/src/main/java/snvn/log/LogFileServiceImplementation.java`

**BEFORE — raw HashMap output:**
```java
Map<String, Object> payload = new HashMap<>();
payload.put("event", event);
payload.put("time", Instant.now().getEpochSecond());
System.out.println("Checking Log" + payload);
log.info("LogFileService: {}", payload);
// Output: LogFileService: {event={level=INFO, message=User created}, time=1740000000}
```

**AFTER — Splunk HEC-compatible JSON:**
```java
Map<String, Object> splunkPayload = buildSplunkPayload(level, message, context);
String json = objectMapper.writeValueAsString(splunkPayload);
log.info("SplunkEvent: {}", json);
```

**Output format (what Splunk sees in the log file):**
```json
SplunkEvent: {
  "time" : 1740873600,
  "index" : "main",
  "source" : "user-service",
  "sourcetype" : "_json",
  "event" : {
    "timestamp" : "2026-03-02T12:00:00.000Z",
    "level" : "INFO",
    "message" : "User created successfully",
    "service" : "user-service",
    "traceId" : "abc123def456",
    "spanId" : "789ghi",
    "correlationId" : "corr-001",
    "jobId" : "job-42",
    "userId" : "U-1001",
    "action" : "CREATE_USER"
  }
}
```

**Key improvements:**
| Feature | Before | After |
|---------|--------|-------|
| Format | `HashMap.toString()` | Jackson JSON (`ObjectMapper`) |
| Field ordering | Random (`HashMap`) | Deterministic (`LinkedHashMap`) |
| Splunk HEC envelope | Missing `index`, `source`, `sourcetype` | Full HEC-compatible envelope |
| MDC trace fields | Not included | Auto-extracted from `MDC` (traceId, spanId, correlationId, jobId) |
| Null handling | Nulls included | Nulls filtered out |
| Pretty print | None | `SerializationFeature.INDENT_OUTPUT` for log readability |

---

### 3. `LogProperties.java`

**Path:** `log-env-properties/src/main/java/snvn/log/LogProperties.java`

**BEFORE:**
```java
@Component                                    // ❌ Duplicate registration
@ConfigurationProperties(prefix = "log-file")
public class LogProperties {
    boolean enabled;
    // only getter/setter for enabled
}
```

**AFTER:**
```java
@ConfigurationProperties(prefix = "log-file")
public class LogProperties {
    private boolean enabled;
    private String index = "main";              // ✅ Splunk index
    private String source = "spring-boot";      // ✅ Splunk source (overridden per service)
    private String sourcetype = "_json";        // ✅ Splunk sourcetype
    // getters/setters for all fields
}
```

---

### 4. `pom.xml`

**Path:** `log-env-properties/pom.xml`

**Added dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

---

### 5. YAML Configuration (all profiles)

**Path:** `log-env-properties/src/main/resources/log-file-{dev,prod,staging,test}.yml`

**BEFORE:**
```yaml
log-file:
  enabled: true
```

**AFTER:**
```yaml
log-file:
  enabled: true
  index: main
  source: ${spring.application.name:spring-boot}
  sourcetype: _json
```

`source` resolves dynamically to the service name (e.g. `user-service`, `auth-service`).

---

### 6. Auto-Configuration Registration Files (NEW)

**log-env-properties:**
`log-env-properties/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
```
snvn.log.LogFileAutoConfiguration
```

**splunk-hec-env-properties:**
`splunk-hec-env-properties/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
```
snvn.splunk.SplunkHecAutoConfiguration
```

---

## Bean Resolution (AFTER Fix)

### Scenario A: `log-file.enabled=true` + `splunk.hec.enabled=false` (dev)

```
LogFileAutoConfiguration activates → logFileService() @Primary     ✅ WINNER
SplunkHecAutoConfiguration → noOpExternalLogService() (no @Primary)
→ Spring injects LogFileServiceImplementation everywhere
```

### Scenario B: `log-file.enabled=false` + `splunk.hec.enabled=true` (prod)

```
LogFileAutoConfiguration SKIPPED entirely
SplunkHecAutoConfiguration → splunkExternalLogService() @Primary   ✅ WINNER
→ Spring injects SplunkExternalLogService everywhere
```

### Scenario C: Both disabled

```
LogFileAutoConfiguration SKIPPED
SplunkHecAutoConfiguration → noOpExternalLogService()              ✅ Fallback
```

---

## Splunk Search Queries

Once the log file is monitored by Splunk (via `splunk add monitor` or Universal Forwarder), use these searches:

```spl
# All events from user-service
index=main source="user-service" sourcetype="_json"

# Filter by level
index=main source="user-service" level="ERROR"

# Search by traceId
index=main traceId="abc123def456"

# Correlate across services
index=main correlationId="corr-001" | stats count by source, level

# Error timeline
index=main level="ERROR" | timechart count by source
```

---

## Build & Verify

```bash
# Rebuild
mvn clean install -pl log-env-properties,splunk-hec-env-properties -am

# Run
mvn spring-boot:run -pl user-service

# Check log file for Splunk JSON
tail -f logs/user-service.log | grep "SplunkEvent"
```

**Expected output:**
```
SplunkEvent: {
  "time" : 1740873600,
  "index" : "main",
  "source" : "user-service",
  "sourcetype" : "_json",
  "event" : { ... }
}
```

**Should NOT see:**
```
GlobalExceptionHandler initialized with NoOpExternalLogService (default)
```

---

## Summary

| # | Problem | Fix |
|---|---------|-----|
| 1 | Splunk's NoOp bean blocked log-file bean | Removed `@ConditionalOnMissingBean`, added `@Primary` |
| 2 | Missing `AutoConfiguration.imports` | Created for both modules |
| 3 | `@Configuration` instead of `@AutoConfiguration` | Changed annotation |
| 4 | `@Component` duplicate on `LogProperties` | Removed |
| 5 | Log output was `HashMap.toString()` — not Splunk-parseable | Rewrote with Jackson `ObjectMapper` + Splunk HEC JSON structure |
| 6 | No Splunk metadata (index/source/sourcetype) | Added to `LogProperties` + all YAML profiles |
| 7 | No MDC trace fields in log events | Auto-extracted traceId, spanId, correlationId, jobId from `MDC` |
| 8 | Missing `jackson-databind` + `spring-boot-autoconfigure` deps | Added to `pom.xml` |


