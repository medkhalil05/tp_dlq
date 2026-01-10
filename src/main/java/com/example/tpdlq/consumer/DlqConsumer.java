package com.example.tpdlq.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DlqConsumer.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.dlq}", groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeFromDlq(String message) {
        try {
            // Try to parse the DLQ message to extract reason and original message
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("reason") && jsonNode.has("originalMessage")) {
                String reason = jsonNode.get("reason").asText();
                String originalMessage = jsonNode.get("originalMessage").toString();
                logger.error("DLQ Consumer - Reason: {} | Original Message: {}", reason, originalMessage);
            } else {
                // Old format or plain message
                logger.error("DLQ Consumer - Received error message: {}", message);
            }
        } catch (Exception e) {
            // If parsing fails, log as-is
            logger.error("DLQ Consumer - Received error message: {}", message);
        }
        // Monitor and handle error messages from DLQ
        // This could involve alerting, manual review, or custom error handling
    }
}
