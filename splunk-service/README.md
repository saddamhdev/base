# Splunk Service

A Spring Boot microservice for sending events and logs to Splunk via HTTP Event Collector (HEC).

## Overview

This service provides a REST API for sending events, logs, and metrics to Splunk's HTTP Event Collector (HEC). It supports:

- Single event submission
- Batch event submission
- Event queuing with automatic flushing
- Metric submission
- Log event submission
- Health check for Splunk connectivity

## Configuration

### Splunk Docker Container

The service is configured to work with a Splunk Docker container running with the following setup:

```bash
docker run -d \
  --name splunk \
  -p 3060:8000 \
  -p 8088:8088 \
  -p 8089:8089 \
  -e SPLUNK_START_ARGS=--accept-license \
  -e SPLUNK_PASSWORD=YourPassword \
  -e SPLUNK_GENERAL_TERMS=--accept-sgt-current-at-splunk-com \
  splunk/splunk:latest
```

### Application Properties

The service is configured with the following default settings:

| Property | Value | Description |
|----------|-------|-------------|
| Server Port | 8104 | Service port |
| HEC URL | http://localhost:8088/services/collector/event | Splunk HEC endpoint |
| HEC Token | a22e99ae-51de-4bff-8567-a726b83760d6 | HEC authentication token |
| Index | main | Default Splunk index |
| Source | splunk-service | Default event source |
| Sourcetype | _json | Default sourcetype |
| Batch Size | 100 | Events per batch |
| Flush Interval | 5000ms | Queue flush interval |

### Splunk Web UI

Access Splunk Web UI at: http://localhost:3060

- Username: admin
- Password: YourPassword

## API Endpoints

### Send Event
Send a single event to Splunk.

```
POST /api/splunk/event
Content-Type: application/json

{
  "message": "User logged in",
  "userId": "12345",
  "action": "LOGIN"
}
```

### Send Log Event
Send a structured log event.

```
POST /api/splunk/log
Content-Type: application/json

{
  "message": "Application started successfully",
  "level": "INFO",
  "source": "main-service",
  "additionalFields": {
    "component": "startup",
    "duration": 1500
  }
}
```

### Send Batch
Send multiple events in a single request.

```
POST /api/splunk/batch
Content-Type: application/json

[
  {"message": "Event 1", "type": "test"},
  {"message": "Event 2", "type": "test"},
  {"message": "Event 3", "type": "test"}
]
```

### Queue Event
Queue an event for batch processing.

```
POST /api/splunk/queue
Content-Type: application/json

{
  "message": "Queued event",
  "priority": "low"
}
```

### Flush Queue
Manually flush the event queue.

```
POST /api/splunk/flush
```

### Send Metric
Send a metric value to Splunk.

```
POST /api/splunk/metric?name=cpu_usage&value=75.5
Content-Type: application/json

{
  "host": "server-01",
  "environment": "production"
}
```

### Health Check
Check Splunk HEC connectivity.

```
GET /api/splunk/health
```

## Running the Service

### Prerequisites
1. Splunk Docker container running with HEC enabled
2. HEC token configured in Splunk
3. Java 21+
4. Maven

### Build and Run

```bash
# Build
mvn clean install -pl splunk-service -am

# Run
mvn spring-boot:run -pl splunk-service
```

Or using the JAR:

```bash
java -jar splunk-service/target/splunk-service-1.0-SNAPSHOT.jar
```

## Dependencies

- Spring Boot 4.0.0
- Spring WebFlux (for reactive HTTP client)
- Jackson (JSON processing)
- Core Common module
- Model module

## Architecture

```
splunk-service/
├── src/main/java/snvn/splunkservice/
│   ├── SplunkServiceApplication.java
│   ├── config/
│   │   ├── SplunkHecProperties.java
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   └── SplunkController.java
│   ├── model/
│   │   ├── ApiResponse.java
│   │   ├── LogEventRequest.java
│   │   ├── SplunkEvent.java
│   │   └── SplunkResponse.java
│   └── service/
│       ├── SplunkHecService.java
│       └── impl/
│           └── SplunkHecServiceImpl.java
└── src/main/resources/
    └── application.yml
```

## Port Configuration

| Service | Port |
|---------|------|
| Splunk Web UI | 3060 |
| Splunk HEC | 8088 |
| Splunk Management | 8089 |
| This Service | 8104 |

## Troubleshooting

### Connection Refused
Ensure Splunk container is running and HEC is enabled:
```bash
docker logs splunk
docker ps
```

### Invalid Token
Verify the HEC token in Splunk Settings > Data Inputs > HTTP Event Collector

### SSL Errors
The service is configured with `ssl-verify: false` for development. Enable SSL verification in production.

## License

Internal use only.

