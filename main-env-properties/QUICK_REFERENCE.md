# Main Service Environment Properties - Quick Reference

## Module Location
📁 `D:\module project\base\main-env-properties`

## Quick Start

### Build
```bash
cd D:\module project\base
mvn clean install
```

### Run with Profile
```bash
# Development (default, H2 in-memory)
java -jar main/target/main-1.0-SNAPSHOT.jar

# Staging (MySQL)
java -jar main/target/main-1.0-SNAPSHOT.jar --spring.profiles.active=staging

# Production (MySQL + SSL)
java -jar main/target/main-1.0-SNAPSHOT.jar --spring.profiles.active=prod

# Testing (H2 per-test)
java -jar main/target/main-1.0-SNAPSHOT.jar --spring.profiles.active=test
```

## Environment Comparison

| Feature | Dev | Staging | Prod | Test |
|---------|-----|---------|------|------|
| Database | H2 (in-mem) | MySQL | MySQL | H2 (create-drop) |
| Logging | DEBUG | INFO | ERROR | DEBUG |
| Log File | ❌ | ❌ | ✅ | ❌ |
| SSL/TLS | ❌ | ❌ | ✅ | ❌ |
| SQL Show | ✅ | ❌ | ❌ | ✅ |
| Compression | ❌ | ✅ | ✅ | ❌ |
| Connection Pool | Default | Default | Optimized | Default |
| Metrics Export | Basic | Basic | Prometheus | Basic |

## Environment Variables

### Development
```powershell
# No variables needed - uses H2 in-memory
```

### Staging
```powershell
$env:SPRING_PROFILES_ACTIVE = "staging"
$env:DB_USERNAME = "staging_user"
$env:DB_PASSWORD = "staging_password"
```

### Production
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_USERNAME = "prod_user"
$env:DB_PASSWORD = "prod_password"
$env:SSL_KEYSTORE_PATH = "C:\certs\main-service.p12"
$env:SSL_KEYSTORE_PASSWORD = "your-password"
```

## Service Details

- **Port**: 3081
- **Context Path**: /api
- **Base URL**: http://localhost:3081/api

## Test Endpoints

```bash
# Health check
curl http://localhost:3081/api/actuator/health

# Info endpoint
curl http://localhost:3081/api/actuator/info

# Metrics (staging/prod)
curl http://localhost:3081/api/actuator/metrics

# Prometheus metrics (prod only)
curl http://localhost:3081/api/actuator/prometheus
```

## Common Tasks

### Add New Environment
1. Create `src/main/resources/application-{env}.yml`
2. Configure database and settings
3. Use: `--spring.profiles.active={env}`

### Modify Existing Profile
1. Edit the corresponding `.yml` file
2. Rebuild: `mvn clean install`
3. Restart main service

### Override at Runtime
```bash
java -jar main.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:mysql://custom-db:3306/db
```

## Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven configuration |
| `application-dev.yml` | Development config (H2) |
| `application-staging.yml` | Staging config (MySQL) |
| `application-prod.yml` | Production config (MySQL + SSL) |
| `application-test.yml` | Test config (H2) |
| `README.md` | Detailed documentation |

## Project Structure
```
base/
├── main-env-properties/           ← NEW MODULE
│   ├── pom.xml
│   ├── README.md
│   └── src/main/resources/
│       ├── application-dev.yml
│       ├── application-staging.yml
│       ├── application-prod.yml
│       └── application-test.yml
├── main/                          (UPDATED)
│   ├── pom.xml (dependency added)
│   └── src/main/resources/
│       └── application.yml (simplified)
└── pom.xml (UPDATED)
    └── (module added to build order)
```

## Troubleshooting

| Issue | Check |
|-------|-------|
| DB Connection Error | Verify DB_USERNAME, DB_PASSWORD environment variables |
| Profile not loading | Check SPRING_PROFILES_ACTIVE variable |
| SSL Error (Prod) | Verify SSL_KEYSTORE_PATH and SSL_KEYSTORE_PASSWORD |
| Query slow | Check connection pool settings in application-prod.yml |

---

**For detailed information**, see `README.md`

