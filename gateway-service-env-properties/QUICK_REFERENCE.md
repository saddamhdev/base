# Gateway Service Env Properties - Quick Reference

## Module Location
📁 `D:\module project\base\gateway-service-env-properties`

## Files Included

| File | Purpose |
|------|---------|
| `pom.xml` | Maven configuration |
| `README.md` | Module overview |
| `CONFIGURATION_GUIDE.md` | Detailed configuration guide |
| `QUICK_REFERENCE.md` | This file |
| `src/main/resources/application-dev.yml` | Development config |
| `src/main/resources/application-staging.yml` | Staging config |
| `src/main/resources/application-prod.yml` | Production config |
| `src/main/resources/application-test.yml` | Test config |

## Parent POM Updated
✅ Added `gateway-service-env-properties` module to parent `pom.xml`

## Gateway Service Updated
✅ Added dependency on `gateway-service-env-properties`
✅ Simplified `application.yml` to use config import

## How to Use

### Step 1: Build
```bash
cd D:\module project\base
mvn clean install
```

### Step 2: Run with Profile
```bash
# Development (default)
java -jar gateway-service-1.0-SNAPSHOT.jar

# Specific environment
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Step 3: Access Gateway
- Base URL: `http://localhost:8080/gateway` (Dev)
- Routes available:
  - `/api/users/**` → User Service
  - `/config/**` → Config Service
  - `/**` → Main Service

## Profile Comparison

| Feature | Dev | Staging | Prod | Test |
|---------|-----|---------|------|------|
| Service URLs | localhost | staging-* | Domain HTTPS | localhost |
| Logging | DEBUG | INFO | ERROR | DEBUG |
| Eureka | Disabled | Enabled | Enabled | Disabled |
| SSL/TLS | ❌ | ❌ | ✅ | ❌ |
| Circuit Breaker | ❌ | ❌ | ✅ | ❌ |
| Metrics | Basic | Basic | Full (Prometheus) | Basic |
| CORS Origins | All | Staging domain | Production domain | Localhost |

## Environment Variables

### Development
```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

### Staging
```powershell
$env:SPRING_PROFILES_ACTIVE = "staging"
```

### Production
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:SSL_KEYSTORE_PATH = "C:\certs\gateway.p12"
$env:SSL_KEYSTORE_PASSWORD = "your-password"
```

## Customization

### Add New Environment
1. Create `src/main/resources/application-{env}.yml`
2. Configure routes and settings
3. Use: `--spring.profiles.active={env}`

### Modify Existing Profile
1. Edit the corresponding `.yml` file
2. Rebuild: `mvn clean install`
3. Restart gateway service

### Override at Runtime
```bash
java -jar gateway-service.jar \
  --spring.profiles.active=prod \
  --spring.cloud.gateway.routes[0].uri=http://custom-url
```

## Service Port
- Gateway: **8080**
- Context Path: **/gateway**
- Full Base URL: `http://localhost:8080/gateway`

## Key Features

✨ **Centralized Configuration**
- All environment configs in one module
- Easy to manage and version control

✨ **Profile-Based Activation**
- Switch environments without rebuilding
- Environment variable support

✨ **Environment-Specific Routes**
- Different URLs per environment
- Automatic CORS configuration

✨ **Production Ready**
- SSL/TLS support
- Circuit breaker protection
- Metrics and monitoring
- Log rotation

✨ **Developer Friendly**
- Debug logging in dev/test
- Simple local setup
- Clear configuration structure

## Common Commands

### Build Everything
```bash
mvn clean install
```

### Run Gateway (Dev)
```bash
java -jar gateway-service-1.0-SNAPSHOT.jar
```

### Run Gateway (Prod)
```bash
java -jar gateway-service-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### View Logs
```bash
# Real-time logs
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# From file (Production)
tail -f /var/log/gateway-service/application.log
```

### Test Health
```bash
curl http://localhost:8080/gateway/actuator/health
```

### Test Route
```bash
curl http://localhost:8080/gateway/api/users/123
curl http://localhost:8080/gateway/config/app
```

## Troubleshooting Quick Links

- Profile not loading → See CONFIGURATION_GUIDE.md > Troubleshooting
- Routes not working → Check service URLs in the environment config
- CORS errors → Review CORS configuration in the `.yml` file
- SSL errors → Verify SSL configuration in application-prod.yml

## Project Structure
```
base/
├── gateway-service-env-properties/     ← NEW MODULE
│   ├── pom.xml
│   ├── README.md
│   ├── CONFIGURATION_GUIDE.md
│   ├── QUICK_REFERENCE.md
│   └── src/main/resources/
│       ├── application-dev.yml
│       ├── application-staging.yml
│       ├── application-prod.yml
│       └── application-test.yml
├── gateway-service/
│   ├── pom.xml (UPDATED)
│   └── src/main/resources/
│       └── application.yml (UPDATED)
├── pom.xml (UPDATED)
└── [other modules...]
```

## Next Steps

1. ✅ Create `gateway-service-env-properties` module
2. ✅ Add environment-specific configuration files
3. ✅ Update parent and gateway-service pom.xml
4. ⏭️ Build and test: `mvn clean install`
5. ⏭️ Run gateway with desired profile
6. ⏭️ Test routes and CORS configuration

---

**For detailed information**, see `CONFIGURATION_GUIDE.md`
**For module overview**, see `README.md`

