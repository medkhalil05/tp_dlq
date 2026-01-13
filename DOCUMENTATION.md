# tp-dlq — Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Setup & Installation](#setup--installation)
5. [Configuration](#configuration)
6. [Validation Rules](#validation-rules)
7. [Error Handling & DLQ](#error-handling--dlq)
8. [Web Dashboard](#web-dashboard)
9. [REST API](#rest-api)
10. [Metrics & Monitoring](#metrics--monitoring)
11. [Development Guide](#development-guide)

---

## Overview

**tp-dlq** is a Spring Boot application demonstrating the Dead Letter Queue (DLQ) pattern with Apache Kafka. It validates incoming order messages, routes invalid or malformed messages to a dedicated DLQ topic, and provides a web interface for monitoring and reprocessing failed messages.

### Key Technologies
- **Spring Boot 3.5.9** — Application framework
- **Apache Kafka** — Message broker
- **Spring Kafka** — Kafka integration
- **Thymeleaf** — Web templating
- **Micrometer** — Metrics and monitoring
- **Prometheus** — Metrics collection and storage
- **Grafana** — Metrics visualization and dashboards
- **Jackson** — JSON processing
- **Docker** — Containerization

---

## Architecture

### System Components

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │─────▶│  Web/REST    │─────▶│   Kafka     │
│  (UI/API)   │      │  Controller  │      │  Producer   │
└─────────────┘      └──────────────┘      └─────────────┘
                                                   │
                                                   ▼
                                            ┌─────────────┐
                                            │ tp8-input   │
                                            │   Topic     │
                                            └─────────────┘
                                                   │
                                                   ▼
                                            ┌─────────────┐
                                            │    Main     │
                                            │  Consumer   │
                                            └─────────────┘
                                                   │
                                    ┌──────────────┴──────────────┐
                                    │                             │
                                    ▼                             ▼
                            ┌──────────────┐            ┌──────────────┐
                            │    Valid     │            │   tp8-dlq    │
                            │  Processing  │            │    Topic     │
                            └──────────────┘            └──────────────┘
                                    │                             │
                                    ▼                             ▼
                            ┌──────────────┐            ┌──────────────┐
                            │    Valid     │            │     DLQ      │
                            │    Store     │            │   Consumer   │
                            └──────────────┘            └──────────────┘
                                    │                             │
                                    └──────────────┬──────────────┘
                                                   ▼
                                            ┌─────────────┐
                                            │     Web     │
                                            │  Dashboard  │
                                            └─────────────┘
```

### Package Structure

```
com.example.tpdlq/
├── config/
│   ├── KafkaConfig.java           # Producer/Consumer bean configuration
│   └── KafkaTopicConfig.java      # Topic creation
├── consumer/
│   ├── MainConsumer.java          # Input topic consumer with validation
│   └── DlqConsumer.java           # DLQ topic consumer
├── controller/
│   ├── WebController.java         # Web dashboard endpoints
│   └── MessageController.java     # REST API endpoints
├── model/
│   ├── Order.java                 # Order entity with extra fields support
│   ├── DlqMessage.java            # DLQ entry with metadata
│   ├── ValidMessage.java          # Valid message record
│   └── ErrorCategory.java         # Error classification enum
└── service/
    ├── MessageProducerService.java  # Kafka producer service
    ├── OrderValidator.java          # Order validation logic
    ├── ValidMessageStore.java       # In-memory valid message store
    └── FileProducerService.java     # JSONL file processing
```

---

## Features

### 1. Message Validation
- **Required Fields**: `orderId`, `userId`, `amount`
- **Field Constraints**:
  - `orderId`: non-empty string
  - `userId`: non-empty string
  - `amount`: positive number (> 0)
- **Strict Mode**: Rejects messages with extra/unexpected fields

### 2. Error Classification
Three error categories:
- **ValidationError**: Missing/invalid required fields
- **MalformedError**: Invalid JSON syntax or unexpected fields
- **UnknownError**: Parsing failures or unclassified errors

### 3. Dead Letter Queue
- Invalid messages routed to `tp8-dlq` topic
- Structured DLQ payload:
  ```json
  {
    "reason": "Missing required field: userId",
    "originalMessage": "{\"orderId\":\"o1\",\"amount\":100}",
    "category": "VALIDATION_ERROR"
  }
  ```
- UUID assigned to each DLQ entry for tracking

### 4. Web Dashboard
- **Metrics Cards**: Processed/Valid/Invalid/Malformed counts
- **DLQ Table**: Timestamp, category badge, reason, original message
- **Valid Messages Table**: Recent successful orders
- **Actions**:
  - Edit & Fix: Modal to correct invalid JSON
  - Reprocess: Resend single message to input topic
  - Reprocess All: Bulk resend all DLQ messages
  - Clear: Remove DLQ or valid message lists

### 5. Metrics & Monitoring
- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`, `/actuator/info`
- **Micrometer Counters**:
  - `tpdlq_messages_processed_total`
  - `tpdlq_messages_valid_total`
  - `tpdlq_messages_invalid_total`
  - `tpdlq_messages_malformed_total`
  - `tpdlq_dlq_total`
  - `tpdlq_dlq_category_total{category=...}`
- **Gauge**: `tpdlq_dlq_backlog` (current DLQ size)

---

## Setup & Installation

### Prerequisites
- **Docker & Docker Compose** (recommended) OR
- **Java 17+** and **Maven 3.8+** for local development
- **Kafka** at `localhost:9092` (if running locally)

### Quick Start with Docker

```bash
cd /home/khalil/tp_dlq
docker-compose up --build
```

Services:
- **Zookeeper**: `localhost:2181`
- **Kafka**: `localhost:9092`
- **Application**: `http://localhost:8080`

### Local Development Setup

1. **Start Kafka**:
   ```bash
   docker run -d --name zookeeper -p 2181:2181 \
     -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper:latest
   
   docker run -d --name kafka -p 9092:9092 \
     -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
     -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
     -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
     confluentinc/cp-kafka:latest
   ```

2. **Build & Run**:
   ```bash
   export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   mvn -DskipTests package
   java -jar target/tp-dlq-1.0.0.jar
   ```

---

## Configuration

### application.properties

```properties
# Server
server.port=8080

# Kafka Bootstrap (Docker: kafka:29092, Local: localhost:9092)
spring.kafka.bootstrap-servers=kafka:29092
spring.kafka.consumer.bootstrap-servers=kafka:29092

# Consumer Group
spring.kafka.consumer.group-id=tp8-consumer-group

# Topics
kafka.topic.input=tp8-input
kafka.topic.dlq=tp8-dlq

# Serialization
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

### Environment Variables

Override defaults:
```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SERVER_PORT=8080
```

---

## Validation Rules

### Order Schema

```json
{
  "orderId": "string (required, non-empty)",
  "userId": "string (required, non-empty)",
  "amount": "number (required, > 0)"
}
```

### Validation Logic

1. **Check Required Fields**:
   - Missing `orderId` → `VALIDATION_ERROR`
   - Missing `userId` → `VALIDATION_ERROR`
   - Missing `amount` → `VALIDATION_ERROR`

2. **Validate Constraints**:
   - `amount <= 0` → `VALIDATION_ERROR`

3. **Check Extra Fields**:
   - Any field beyond `orderId`, `userId`, `amount` → `MALFORMED_ERROR`

4. **JSON Parsing**:
   - Invalid JSON syntax → `MALFORMED_ERROR`

### Examples

| Input | Result | Category |
|-------|--------|----------|
| `{"orderId":"o1","userId":"u1","amount":100}` | ✅ Valid | — |
| `{"orderId":"o1","amount":100}` | ❌ DLQ | ValidationError |
| `{"orderId":"o1","userId":"u1","amount":-5}` | ❌ DLQ | ValidationError |
| `{"orderId":"o1","userId":"u1","amount":100,"extra":"field"}` | ❌ DLQ | MalformedError |
| `{"orderId":"o1","userId":"u1","amount":10` | ❌ DLQ | MalformedError |

---

## Error Handling & DLQ

### DLQ Message Structure

```java
public class DlqMessage {
    private String id;                    // UUID
    private String reason;                // Error description
    private String originalMessage;       // Original JSON
    private LocalDateTime timestamp;      // When error occurred
    private ErrorCategory category;       // Error classification
}
```

### Error Categories

```java
public enum ErrorCategory {
    VALIDATION_ERROR("ValidationError"),   // Business rule violations
    MALFORMED_ERROR("MalformedError"),     // Syntax/schema errors
    UNKNOWN_ERROR("UnknownError");         // Unclassified errors
}
```

### DLQ Processing Flow

1. **Message Consumption**: MainConsumer receives message from `tp8-input`
2. **Validation**: OrderValidator checks required fields, constraints, extra fields
3. **Error Detection**: If validation fails, categorize error
4. **DLQ Production**: Send structured message to `tp8-dlq`
5. **DLQ Consumption**: DlqConsumer logs and stores for dashboard
6. **Manual Intervention**: User can edit & reprocess via web UI

---

## Web Dashboard

### URL
`http://localhost:8080/`

### Features

#### 1. Metrics Dashboard
Display cards showing real-time counters:
- **Processed**: Total messages consumed
- **Valid**: Successfully validated orders
- **Invalid**: Validation errors
- **Malformed**: JSON syntax/schema errors

#### 2. File Upload
- Upload `.jsonl` (JSON Lines) files
- Each line sent to `tp8-input` topic
- Returns: total lines, successful, skipped

#### 3. DLQ Table
Columns:
- **Timestamp**: When error occurred
- **Category**: Color-coded badge (ValidationError, MalformedError, UnknownError)
- **Reason**: Error description
- **Original Message**: JSON that failed validation
- **Actions**:
  - **Edit & Fix**: Opens modal to correct JSON
  - **Reprocess**: Resend original to input topic

#### 4. Valid Messages Table
Recent successfully processed orders:
- Timestamp
- Order ID
- User ID
- Amount
- Original Message

#### 5. Bulk Actions
- **Clear DLQ**: Remove all DLQ entries
- **Clear Valid**: Remove all valid entries
- **Reprocess All**: Resend all DLQ messages

---

## REST API

### Base URL
`http://localhost:8080/api/messages`

### Endpoints

#### 1. Health Check
```http
GET /api/messages/health
```
**Response**: `200 OK`
```json
"DLQ Service is running"
```

#### 2. Send Message
```http
POST /api/messages/send
Content-Type: application/json

{"orderId":"o1","userId":"u1","amount":100}
```
**Response**: `200 OK`
```json
"Message sent to input topic: {...}"
```

#### 3. Process File
```http
POST /api/messages/process-file
Content-Type: text/plain

/absolute/path/to/orders.jsonl
```
**Response**: `200 OK`
```json
"File processing started for: /absolute/path/to/orders.jsonl"
```

### Web Endpoints

#### Dashboard
```http
GET /
```
Renders main dashboard with metrics, DLQ, and valid messages.

#### Reprocess DLQ Message
```http
POST /dlq/reprocess/{id}
```
Resends original message to input topic.

#### Edit & Reprocess
```http
POST /dlq/reprocess-edited/{id}
?editedMessage={"orderId":"o1","userId":"u1","amount":100}
```
Sends corrected JSON to input topic.

#### Bulk Reprocess
```http
POST /dlq/reprocess-all
```
Resends all DLQ messages.

#### Clear DLQ
```http
POST /dlq/clear
```
Removes all DLQ entries from store.

#### Clear Valid
```http
POST /valid/clear
```
Removes all valid entries from store.

---

## Metrics & Monitoring

### Actuator Endpoints

#### Health
```http
GET /actuator/health
```
Returns application health status and Kafka connectivity.

#### Metrics
```http
GET /actuator/metrics
```
Lists all available metrics.

#### Specific Metric
```http
GET /actuator/metrics/tpdlq_messages_processed_total
```
Returns detailed metric information.

### Available Metrics

| Metric Name | Type | Description |
|-------------|------|-------------|
| `tpdlq_messages_processed_total` | Counter | Total messages consumed |
| `tpdlq_messages_valid_total` | Counter | Successfully validated |
| `tpdlq_messages_invalid_total` | Counter | Validation failures |
| `tpdlq_messages_malformed_total` | Counter | JSON syntax errors |
| `tpdlq_dlq_total` | Counter | Total DLQ messages |
| `tpdlq_dlq_category_total` | Counter | DLQ by category (tag: category) |
| `tpdlq_dlq_backlog` | Gauge | Current DLQ size |

### Prometheus Integration

The application exposes metrics in Prometheus format via the `/actuator/prometheus` endpoint.

**Configuration** (`application.properties`):
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

**Prometheus Endpoint**:
```http
GET /actuator/prometheus
```

**Prometheus Configuration** (`prometheus.yml`):
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'tp-dlq-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['tp-dlq:8080']
        labels:
          application: 'tp-dlq'
          environment: 'dev'
```

**Access Prometheus UI**: http://localhost:9090

**Useful PromQL Queries**:
```promql
# Total messages processed
tpdlq_messages_processed_total

# Message processing rate (per second)
rate(tpdlq_messages_processed_total[1m])

# Current DLQ backlog
tpdlq_dlq_backlog

# Error rate (5-minute average)
rate(tpdlq_messages_invalid_total[5m])

# DLQ messages by category
tpdlq_dlq_category_total{category="ValidationError"}
```

### Grafana Integration

Grafana provides rich visualization for Prometheus metrics.

**Access**: http://localhost:3000 (admin/admin)

**Setup Steps**:
1. **Add Data Source**:
   - Configuration → Data Sources → Add data source
   - Select "Prometheus"
   - URL: `http://prometheus:9090`
   - Click "Save & Test"

2. **Create Dashboard**:
   - Click "+" → Dashboard → Add new panel
   - Query examples:
     - `tpdlq_messages_processed_total` (Total processed)
     - `rate(tpdlq_messages_valid_total[5m])` (Valid messages/sec)
     - `tpdlq_dlq_backlog` (DLQ size)
   - Select visualization type (Graph, Gauge, Stat, Table)
   - Configure panel options and save

3. **Dashboard Ideas**:
   - **Overview Panel**: Display total processed, valid, invalid counters
   - **Rate Panel**: Show message processing rate over time
   - **DLQ Panel**: Current backlog with trend
   - **Error Distribution**: Pie chart of error categories
   - **Health Check**: Application uptime and status

---

## Development Guide

### Building

```bash
# Clean and build
mvn clean package

# Skip tests
mvn -DskipTests package

# Run tests
mvn test
```

### Running Locally

```bash
# With Maven plugin
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvn spring-boot:run

# With JAR
java -jar target/tp-dlq-1.0.0.jar
```

### Docker Build

```bash
# Build image
docker build -t tp-dlq:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  tp-dlq:latest
```

### Code Structure

#### Adding New Validation Rules

1. Update `OrderValidator.validateOrder()`:
   ```java
   if (order.getCustomField() == null) {
       return "Missing required field: customField";
   }
   ```

2. Update `Order.java` model:
   ```java
   private String customField;
   // Add getters/setters
   ```

#### Adding New Error Categories

1. Add to `ErrorCategory` enum:
   ```java
   NEW_ERROR_TYPE("NewErrorType")
   ```

2. Update `MainConsumer` to route:
   ```java
   if (validationError.contains("new error")) {
       category = ErrorCategory.NEW_ERROR_TYPE;
   }
   ```

3. Update UI badge colors in `style.css`:
   ```css
   .badge-new-error {
       background: #color;
   }
   ```

#### Adding New Metrics

```java
public class YourService {
    private final Counter customCounter;
    
    public YourService(MeterRegistry registry) {
        this.customCounter = registry.counter("tpdlq_custom_total");
    }
    
    public void doSomething() {
        customCounter.increment();
    }
}
```

### Testing

#### Sample JSONL File

Create `test_orders.jsonl`:
```json
{"orderId":"o1","userId":"u1","amount":100}
{"orderId":"o2","amount":50}
{"orderId":"o3","userId":"u3","amount":-10}
{"orderId":"o4","userId":"u4","amount":75,"extra":"field"}
```

#### Upload via UI
1. Navigate to `http://localhost:8080/`
2. Click "Choose File"
3. Select `test_orders.jsonl`
4. Click "Upload and Process"

#### Upload via cURL
```bash
curl -X POST http://localhost:8080/api/messages/process-file \
  -H "Content-Type: text/plain" \
  -d "$(pwd)/test_orders.jsonl"
```

---

## Troubleshooting

### Issue: KafkaTemplate Bean Not Found
**Solution**: Ensure `KafkaConfig.java` exists with `@Bean` definitions.

### Issue: Kafka Connection Refused
**Cause**: Kafka not running or wrong bootstrap server.
**Solution**: 
- Docker: Use `kafka:29092`
- Local: Use `localhost:9092` and set `SPRING_KAFKA_BOOTSTRAP_SERVERS`

### Issue: Edit & Fix Button Not Working
**Solution**: Check browser console for JavaScript errors. Ensure `data-id`, `data-message`, `data-reason` attributes exist on buttons.

### Issue: Metrics Not Visible
**Solution**: Verify `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics
```

### Issue: DLQ Messages Show UnknownError
**Cause**: Category parsing mismatch.
**Solution**: Ensure producer sends `category` as enum name (e.g., `VALIDATION_ERROR`).

### Issue: Prometheus Shows "Empty Query Result"
**Cause**: Prometheus cannot scrape metrics or no data generated yet.
**Solution**:
1. Check Prometheus targets: http://localhost:9090/targets
2. Verify target is "UP"
3. Process some messages to generate metrics
4. Wait 15 seconds for next scrape interval

### Issue: Prometheus Target Shows "DOWN"
**Cause**: Network connectivity or endpoint not accessible.
**Solution**:
1. Verify services are on same Docker network: `docker network inspect tp_dlq_tp-dlq-network`
2. Test connectivity: `docker exec prometheus wget -qO- http://tp-dlq:8080/actuator/prometheus`
3. Restart stack: `docker-compose down && docker-compose up --build`

### Issue: Grafana Cannot Connect to Prometheus
**Cause**: Wrong data source URL.
**Solution**: Use `http://prometheus:9090` (not `localhost`) for Docker network communication.

### Issue: Grafana Shows "No Data"
**Cause**: No metrics collected or wrong query.
**Solution**:
1. Verify Prometheus is scraping: Check targets page
2. Test query in Prometheus UI first
3. Check time range in Grafana panel
4. Ensure data exists for selected time period

---

## License

Educational use only.

---

## Contributing

For questions or improvements, contact the development team.
