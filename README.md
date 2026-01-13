# tp-dlq — Spring Boot + Kafka DLQ

A pragmatic Dead Letter Queue (DLQ) demo built with Spring Boot and Apache Kafka. It validates incoming orders, routes invalid or malformed messages to a DLQ, and provides a simple web dashboard to visualize DLQ and valid messages — plus basic metrics via Actuator/Micrometer.

## Overview
- Web UI for uploading `.jsonl` files, viewing DLQ/valid messages, and reprocessing DLQ entries.
- REST endpoints for direct sends and file processing.
- Validation pipeline: parse JSON → validate required fields → route to DLQ with category/reason if invalid.
- Metrics: counters for processed/valid/invalid/malformed messages, DLQ backlog gauge.
- **Prometheus & Grafana**: Built-in monitoring stack for metrics visualization and alerting.

## Key Features
- **Kafka integration**: input topic `tp8-input`, DLQ topic `tp8-dlq` ([application.properties](src/main/resources/application.properties)).
- **Validation**: checks `orderId`, `userId`, `amount>0` ([OrderValidator](src/main/java/com/example/tpdlq/service/OrderValidator.java)).
- **DLQ handling**: structured DLQ payload with `reason`, `category`, and quoted `originalMessage` ([MessageProducerService](src/main/java/com/example/tpdlq/service/MessageProducerService.java)).
- **Consumers**: main consumer validates; DLQ consumer logs/stores errors ([MainConsumer](src/main/java/com/example/tpdlq/consumer/MainConsumer.java), [DlqConsumer](src/main/java/com/example/tpdlq/consumer/DlqConsumer.java)).
- **Web dashboard**: list DLQ messages with badges, reprocess by ID, and visualize recent valid messages ([index.html](src/main/resources/templates/index.html), [WebController](src/main/java/com/example/tpdlq/controller/WebController.java)).
- **Metrics & health**: Actuator endpoints (`/actuator/metrics`, `/actuator/health`, `/actuator/info`, `/actuator/prometheus`).
- **Prometheus monitoring**: Automatic metrics collection and scraping from Spring Boot Actuator.
- **Grafana dashboards**: Pre-configured visualization for real-time monitoring.

## Architecture
- Producers: [MessageProducerService](src/main/java/com/example/tpdlq/service/MessageProducerService.java), [FileProducerService](src/main/java/com/example/tpdlq/service/FileProducerService.java)
- Consumers: [MainConsumer](src/main/java/com/example/tpdlq/consumer/MainConsumer.java), [DlqConsumer](src/main/java/com/example/tpdlq/consumer/DlqConsumer.java)
- Web/UI: [WebController](src/main/java/com/example/tpdlq/controller/WebController.java), [static/css/style.css](src/main/resources/static/css/style.css), [templates/index.html](src/main/resources/templates/index.html)
- Validation: [OrderValidator](src/main/java/com/example/tpdlq/service/OrderValidator.java)
- Storage (in-memory): [ValidMessageStore](src/main/java/com/example/tpdlq/service/ValidMessageStore.java) for recent valid messages
- Config: [KafkaConfig](src/main/java/com/example/tpdlq/config/KafkaConfig.java), [KafkaTopicConfig](src/main/java/com/example/tpdlq/config/KafkaTopicConfig.java)

## Prerequisites
- **Docker & Docker Compose** (for Option 1) OR
- **Java 17** + **Maven 3.8+** (for local development)
- Kafka at `localhost:9092` (if running locally without Compose)

### Quick Kafka (Docker)
If you only want to run Kafka (without using Compose for the whole stack):
```bash
# Zookeeper
docker run -d --name zookeeper -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper:latest
# Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```
Then run the app locally with Option 2 above.

## Configuration
Edit [src/main/resources/application.properties](src/main/resources/application.properties):
- `spring.kafka.bootstrap-servers=kafka:29092` (default for Docker; override `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092` for local dev)
- `spring.kafka.consumer.group-id=tp8-consumer-group`
- `kafka.topic.input=tp8-input`
- `kafka.topic.dlq=tp8-dlq`
- Actuator exposure: `management.endpoints.web.exposure.include=health,info,metrics,prometheus`

### Local Development (without Docker)
If running locally without Docker Compose, override the Kafka bootstrap server:
```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvn -DskipTests spring-boot:run
```
## Build & Run

### Option 1: Docker Compose (Recommended)
The easiest way — includes Kafka, Zookeeper, application, Prometheus, and Grafana in one command:
```bash
cd /home/khalil/tp_dlq
docker-compose up --build
```
Then:
- **Application Dashboard**: http://localhost:8080/
- **Prometheus UI**: http://localhost:9090/
- **Grafana Dashboard**: http://localhost:3000/ (admin/admin)
- **Kafka**: localhost:9092 (accessible from host)
- **Metrics Endpoint**: http://localhost:8080/actuator/metrics
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus

To stop:
```bash
docker-compose down
```

### Option 2: Local Development (without Docker Compose)
Requires Kafka running on `localhost:9092`.

**Build:**
```bash
mvn -DskipTests package
```

**Run via Spring Boot:**
```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvn -DskipTests spring-boot:run
```

**Or run the JAR:**
```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
java -jar target/tp-dlq-1.0.0.jar
```

Web UI: http://localhost:8080/

## REST Endpoints
Base: `/api/messages`
- `POST /send` — send raw JSON string to input topic
- `POST /process-file` — body contains absolute path to `.jsonl` file
- `GET /health` — service health

## Web Dashboard Actions
- Upload `.jsonl` and process lines to input topic.
- View DLQ table with timestamp, category, reason, original message.
- Reprocess DLQ entry: `POST /dlq/reprocess/{id}`.
- Clear lists: `POST /dlq/clear`, `POST /valid/clear`.
- View valid messages table (recent).

## Monitoring Stack

### Prometheus
Prometheus automatically scrapes metrics from the Spring Boot application every 15 seconds.

**Access Prometheus UI**: http://localhost:9090

**Useful Queries**:
- All metrics: `{job="tp-dlq-app"}`
- Message rate: `rate(tpdlq_messages_processed_total[1m])`
- DLQ backlog: `tpdlq_dlq_backlog`
- Error rate: `rate(tpdlq_messages_invalid_total[5m])`

**Check Targets**: Navigate to Status → Targets to verify scraping status.

### Grafana
Grafana provides rich visualization and dashboards for metrics.

**Access Grafana**: http://localhost:3000 (default credentials: admin/admin)

**Setup Data Source**:
1. Go to Configuration → Data Sources
2. Add Prometheus data source
3. URL: `http://prometheus:9090`
4. Click "Save & Test"

**Create Dashboard**:
1. Click "+" → Dashboard → Add new panel
2. Use PromQL queries like `tpdlq_messages_processed_total`
3. Configure visualization (graph, gauge, table)
4. Save dashboard

## Metrics
- Actuator: `/actuator/metrics`
- Prometheus endpoint: `/actuator/prometheus`
- Counters: `tpdlq_messages_processed_total`, `tpdlq_messages_valid_total`, `tpdlq_messages_invalid_total`, `tpdlq_messages_malformed_total`
- DLQ: `tpdlq_dlq_total`, `tpdlq_dlq_category_total{category=...}`
- Gauge: `tpdlq_dlq_backlog`

## Sample Data
Use the bundled [orders_in.jsonl](orders_in.jsonl). Upload via the UI or:
```bash
curl -X POST http://localhost:8080/api/messages/process-file \
  -H "Content-Type: text/plain" \
  -d "$(pwd)/orders_in.jsonl"
```

## Troubleshooting
- **KafkaTemplate bean not found**: ensured by [KafkaConfig](src/main/java/com/example/tpdlq/config/KafkaConfig.java).
- **Malformed DLQ JSON**: DLQ payload constructed via `ObjectMapper`; `originalMessage` is quoted.
- **No metrics visible**: check actuator exposure in [application.properties](src/main/resources/application.properties).
- **Docker Compose connection issues**: ensure `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092` (set in compose) or override for local dev.
- **Kafka not found**: make sure Kafka is running (Compose or manual) before starting the app.

## Files & Configuration
- [Dockerfile](Dockerfile) — multi-stage build (Maven → Java 17 Alpine)
- [docker-compose.yml](docker-compose.yml) — Zookeeper, Kafka, tp-dlq, Prometheus, Grafana services
- [prometheus.yml](prometheus.yml) — Prometheus scrape configuration
- [.dockerignore](.dockerignore) — excludes build artifacts from Docker context
- [application.properties](src/main/resources/application.properties) — Kafka/server config and Prometheus endpoint exposure

## License
Educational use only.
