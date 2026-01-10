# Dead Letter Queue (DLQ) Pattern - Spring Boot & Kafka

This project demonstrates the implementation of the Dead Letter Queue (DLQ) pattern using Java Spring Boot and Apache Kafka, with support for JSON Lines (.jsonl) file processing.

## Features

1. **Kafka Topics Configuration**:
   - Primary topic: `tp8-input` for processing messages
   - Dead Letter Queue topic: `tp8-dlq` for handling error messages

2. **Message Producers**:
   - Service for sending messages to Kafka topics (`tp8-input` and `tp8-dlq`)
   - File producer service for processing `.jsonl` files

3. **Consumers**:
   - Main consumer for the `tp8-input` topic with JSON validation logic
   - DLQ consumer for monitoring error messages from the `tp8-dlq` topic with reason tracking

4. **JSON Validation Logic**:
   - Validates JSON orders with required fields: `orderId`, `userId`, `amount`
   - Ensures `amount` is greater than 0
   - Sends invalid messages to DLQ with detailed error reasons
   - Handles malformed JSON gracefully

5. **JSONL File Processing**:
   - Reads `.jsonl` (JSON Lines) files line by line
   - Validates each JSON line before sending to Kafka
   - Skips malformed lines with error logging
   - Provides processing statistics

6. **Retry Strategy**:
   - Scheduled retry mechanism (runs every 5 minutes)
   - Placeholder for processing messages from the DLQ topic

7. **REST APIs**:
   - Endpoints to send individual messages for testing
   - Endpoint to trigger file-based production from `.jsonl` files

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Apache Kafka (running on localhost:9092)
- Docker (optional, for running Kafka)

## Running Kafka with Docker

If you don't have Kafka installed, you can run it using Docker:

```bash
# Create a docker-compose.yml file or use the following commands:
docker run -d --name zookeeper -p 2181:2181 -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper:latest

docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  --link zookeeper \
  confluentinc/cp-kafka:latest
```

Or use docker-compose:

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Run with: `docker-compose up -d`

## Building the Application

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080.

## Testing with curl

### Check service health
```bash
curl http://localhost:8080/api/messages/health
```

### Send a valid JSON order
```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"orderId":"o1","userId":"u1","amount":120.5}'
```

### Send an invalid JSON order (missing userId)
```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"orderId":"o2","amount":15.0}'
```

### Send an invalid JSON order (negative amount)
```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"orderId":"o3","userId":"u3","amount":-5}'
```

### Process a JSONL file
```bash
curl -X POST http://localhost:8080/api/messages/process-file \
  -H "Content-Type: text/plain" \
  -d "/absolute/path/to/orders_in.jsonl"
```

For testing with the provided sample file:
```bash
curl -X POST http://localhost:8080/api/messages/process-file \
  -H "Content-Type: text/plain" \
  -d "$(pwd)/orders_in.jsonl"
```

## Expected Behavior

1. **Valid JSON orders** (all required fields present, amount > 0):
   - Processed successfully
   - Logged with INFO level
   - Remain in the main processing flow

2. **Invalid JSON orders** (missing fields or amount <= 0):
   - Redirected to the DLQ topic with reason
   - Logged with WARN level
   - Consumed by DLQ consumer
   - Logged with ERROR level in DLQ consumer including the reason

3. **Malformed JSON**:
   - Skipped during file processing or sent to DLQ if sent via consumer
   - Logged with ERROR level
   - Error reason provided in DLQ message

4. **Retry mechanism**:
   - Runs every 5 minutes
   - Placeholder for reprocessing DLQ messages

## Sample JSONL File

The repository includes a sample `orders_in.jsonl` file with test data:
- Line 1: Valid order
- Line 2: Valid order
- Line 3: Malformed JSON (missing closing brace)
- Line 4: Invalid order (missing userId)
- Line 5: Invalid order (negative amount)

## Project Structure

```
tp-dlq/
├── orders_in.jsonl                                # Sample JSONL file for testing
├── src/
│   ├── main/
│   │   ├── java/com/example/tpdlq/
│   │   │   ├── TpDlqApplication.java              # Main application class
│   │   │   ├── config/
│   │   │   │   └── KafkaTopicConfig.java          # Kafka topics configuration
│   │   │   ├── consumer/
│   │   │   │   ├── MainConsumer.java              # Main topic consumer with JSON validation
│   │   │   │   └── DlqConsumer.java               # DLQ topic consumer with reason logging
│   │   │   ├── controller/
│   │   │   │   └── MessageController.java         # REST API endpoints
│   │   │   ├── model/
│   │   │   │   └── Order.java                     # Order model for JSON validation
│   │   │   └── service/
│   │   │       ├── MessageProducerService.java    # Message producer service
│   │   │       ├── FileProducerService.java       # JSONL file processor
│   │   │       └── RetryService.java              # Scheduled retry service
│   │   └── resources/
│   │       └── application.properties             # Application configuration
│   └── test/
├── pom.xml
└── README.md
```
│   │   │       └── RetryService.java          # Scheduled retry service
│   │   └── resources/
│   │       └── application.properties         # Application configuration
│   └── test/
├── pom.xml
└── README.md
```

## Configuration

Key configuration properties in `application.properties`:

- `kafka.topic.input=tp8-input` - Main input topic
- `kafka.topic.dlq=tp8-dlq` - Dead Letter Queue topic
- `spring.kafka.bootstrap-servers=localhost:9092` - Kafka server address

## Monitoring Logs

Watch the application logs to see messages being processed:

```bash
# In the terminal where the application is running, you'll see:
# - INFO logs for valid messages being processed
# - WARN logs for invalid messages being sent to DLQ
# - ERROR logs for messages received in DLQ consumer
```

## Dependencies

- Spring Boot 3.2.1
- Spring Kafka
- Java 17

## License

This project is for educational purposes.