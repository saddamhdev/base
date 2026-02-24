# Splunk Docker Setup Guide

## Quick Start

### 1. Start Splunk Container
```bash
cd splunk-service
docker-compose up -d
```

### 2. Wait for Splunk to Start
Splunk takes about **2-3 minutes** to fully start. Check the logs:
```bash
docker logs -f splunk
```
Wait until you see: `Ansible playbook complete, will begin polling for Splunk On Prem...`

### 3. Access Splunk Web UI
| Property | Value |
|----------|-------|
| URL | http://localhost:8000 |
| Username | admin |
| Password | Admin@123456 |

### 4. Verify HEC is Enabled
After Splunk starts, HEC should be automatically enabled.

If HEC is not working, enable it manually:
1. Go to **Settings → Data Inputs → HTTP Event Collector**
2. Click **Global Settings**
3. Enable **All Tokens**
4. Set **HTTP Port Number** to `8088`
5. Click **Save**

### 5. Test with Postman

**Endpoint:**
```
POST http://localhost:8104/api/splunk/log
```

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
    "message": "Test log message from splunk-service",
    "level": "INFO",
    "source": "auth-service",
    "sourceType": "application_log",
    "host": "localhost"
}
```

### 6. View Logs in Splunk
1. Go to http://localhost:8000
2. Click **Search & Reporting**
3. Search: `index=main`

---

## Troubleshooting

### Connection Refused / Premature Close
- Ensure Splunk container is running: `docker ps`
- Check if port 8088 is accessible: `curl http://localhost:8088/services/collector/health`
- Wait for Splunk to fully start (2-3 minutes)

### Stop Splunk
```bash
docker-compose down
```

### Remove All Data
```bash
docker-compose down -v
```

